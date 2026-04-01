package org.search.search.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.search.search.entity.*;
import org.search.search.index.ParagraphIndex;
import org.search.search.mapper.*;
import org.search.search.repository.ParagraphIndexRepository;
import org.search.search.service.ParagraphSyncService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/*
 * @description: 用于删除旧索引,新建索引,同步数据
 * @param null
 * @return: 
 * @author: GX
 * @time: 2025/12/3 8:58
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParagraphSyncServiceImpl implements ParagraphSyncService {

    private final ParagraphMapper paragraphMapper;
    private final ChapterMapper chapterMapper;            
    private final TbBookMapper tbBookMapper;              
    private final TbEventAssociationAiMapper tbEventAssociationAiMapper;
    private final TbEventAiMapper tbEventAiMapper;
    private final ParagraphIndexRepository paragraphIndexRepository;
    private final org.search.search.component.SyncTaskMonitor monitor;

    private static final int PAGE_SIZE = 1000;

    private final ExecutorService executorService =
            new ThreadPoolExecutor(
                    4, 8, 60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(20),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

    public void syncAllParagraphsToEs() {
        monitor.addLog("开始分批同步 Paragraph 数据到 ES ...");

        long total = paragraphMapper.selectCount(null);
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        monitor.addLog(String.format("总记录数：%d，每页 %d 条，共 %d 页", total, PAGE_SIZE, totalPages));

        List<Future<?>> futures = new ArrayList<>();
        // 使用原子计数器追踪完成的页数
        java.util.concurrent.atomic.AtomicInteger completedPages = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int page = 1; page <= totalPages; page++) {
            final int currentPage = page;

            Future<?> future = executorService.submit(() -> {
                try {
                    syncPageToEs(currentPage);
                    int current = completedPages.incrementAndGet();
                    int progress = (int) ((current * 100.0) / totalPages);
                    monitor.updateProgress(progress, String.format("已完成第 %d/%d 页", current, totalPages));
                } catch (Exception e) {
                    log.error("第 {} 页同步失败：{}", currentPage, e.getMessage(), e);
                    monitor.addLog(String.format("第 %d 页同步失败: %s", currentPage, e.getMessage()));
                }
            });
            futures.add(future);
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                log.error("同步线程执行异常", e);
            }
        }

        monitor.addLog("全部分页同步完成");
    }

    private void syncPageToEs(int pageNum) {
        Page<Paragraph> page = new Page<>(pageNum, PAGE_SIZE);
        List<Paragraph> records = paragraphMapper.selectPage(page, null).getRecords();
        if (records.isEmpty()) return;

        List<ParagraphIndex> indexList = new ArrayList<>(records.size());

        for (Paragraph p : records) {

            // 1. 查 chapter（获取 bookId）
            Chapter chapter = chapterMapper.selectById(p.getChapterId());

            Integer bookId = null;
            String author = null;
            String title = null;
            String chapterContent = null;
            if (chapter != null && chapter.getBookId() != null) {
                // 2. 查 book（获取 title/author）
                chapterContent = chapter.getChapterContent();
                TbBooks book = tbBookMapper.selectById(chapter.getBookId());
                if (book != null) {
                    bookId = book.getBookId();
                    author = book.getAuthor();
                    title = book.getTitle();
                }
            }

            // 3. 查 AI 事件 eventTitle
            String eventTitle = null;
            Integer assocAiId = p.getEventAssociationAiId();
            if (assocAiId != null) {
                TbEventAssociationAi mid = tbEventAssociationAiMapper.selectById(assocAiId);
                if (mid != null && mid.getEventId() != null) {
                    TbEventAi ev = tbEventAiMapper.selectById(mid.getEventId());
                    if (ev != null) {
                        eventTitle = ev.getEventTitle();
                    }
                }
            }
            log.info("准备索引 Paragraph ID = {}，chapterContent = {}, bookId = {}, author = {}, title = {}, paragraphContent = {}, paragraphOrder = {}, chapterId = {}, eventTitle = {}",
                    p.getId(),
                    chapterContent,
                    bookId,
                    author,
                    title,
                    p.getParagraphContent(),
                    p.getParagraphOrder(),
                    p.getChapterId(),
                    eventTitle
            );

            // 4. 组装 ES 文档（只保留新索引需要的字段）
            ParagraphIndex idx = ParagraphIndex.builder()
                    .id(p.getId())
                    .paragraphContent(p.getParagraphContent())
                    .paragraphOrder(p.getParagraphOrder())
                    .chapterId(p.getChapterId())
                    .chapterContent(chapterContent)
                    .bookId(bookId)
                    .author(author)
                    .title(title)
                    .eventTitle(eventTitle)
                    .build();

            indexList.add(idx);
        }

        try {
            paragraphIndexRepository.saveAll(indexList);
            log.info("第 {} 页同步完成，共 {} 条记录", pageNum, indexList.size());
        } catch (IOException e) {
            log.error("第 {} 页同步到 ES 失败：{}", pageNum, e.getMessage(), e);
        }
    }
}
