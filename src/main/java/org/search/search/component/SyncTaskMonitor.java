package org.search.search.component;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SyncTaskMonitor {
    // 0: 空闲, 1: 进行中, 2: 完成, 3: 失败
    private volatile int status = 0;
    private volatile int progress = 0;
    private final List<String> logs = new CopyOnWriteArrayList<>();
    private volatile String currentStep = "";

    public void startTask() {
        this.status = 1;
        this.progress = 0;
        this.logs.clear();
        addLog("任务开始...");
    }

    public void addLog(String message) {
        // 保留最近 50 条日志
        if (logs.size() > 50) {
            logs.remove(0);
        }
        logs.add(String.format("[%tT] %s", System.currentTimeMillis(), message));
    }

    public void updateProgress(int progress, String step) {
        this.progress = progress;
        this.currentStep = step;
        if (step != null) {
            addLog(step);
        }
    }

    public void finishTask() {
        this.status = 2;
        this.progress = 100;
        this.currentStep = "完成";
        addLog("任务执行成功");
    }

    public void failTask(String error) {
        this.status = 3;
        addLog("任务失败: " + error);
    }

    // Getters
    public int getStatus() { return status; }
    public int getProgress() { return progress; }
    public List<String> getLogs() { return new ArrayList<>(logs); }
    public String getCurrentStep() { return currentStep; }
}
