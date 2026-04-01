package org.search.search.component;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class IpResolver {

    private Searcher searcher;
    private byte[] cBuff;

    @PostConstruct
    public void init() {
        try {
            // 1. 从资源文件读取 ip2region.xdb 到内存
            ClassPathResource resource = new ClassPathResource("ip2region.xdb");
            if (!resource.exists()) {
                log.warn("ip2region.xdb not found in resources. IP resolution will default to 'Unknown'.");
                return;
            }
            
            InputStream is = resource.getInputStream();
            cBuff = StreamUtils.copyToByteArray(is);
            
            // 2. 创建 searcher 对象
            searcher = Searcher.newWithBuffer(cBuff);
            log.info("Ip2region loaded successfully.");
            
        } catch (Exception e) {
            log.error("Failed to load ip2region.xdb", e);
        }
    }

    /**
     * 解析 IP 返回省份
     * @param ip 如 1.2.3.4
     * @return 省份名称 (如 北京市)，如果解析失败返回 null
     */
    public String resolveProvince(String ip) {
        if (searcher == null || ip == null || ip.isEmpty()) return null;

        try {
            // ip2region 原始格式通常是: 国家|区域|省份|城市|ISP (5段)
            // 但测试发现当前环境返回的是: 国家|省份|城市|ISP (4段)
            String region = searcher.search(ip);
            if (region == null) return null;

            String[] parts = region.split("\\|");
            String province = null;

            if (parts.length == 4) {
                // 4段格式: 中国|安徽省|黄山市|联通
                province = parts[1];
            } else if (parts.length >= 5) {
                // 5段格式: 中国|0|安徽省|黄山市|联通
                province = parts[2];
            } else if (parts.length == 3) {
                // 3段格式兜底: 中国|安徽省|黄山市
                province = parts[1];
            }

            if (province != null && !"0".equals(province)) {
                return cleanProvinceName(province);
            }
        } catch (Exception e) {
            log.warn("Error resolving IP {}: {}", ip, e.getMessage());
        }
        return null;
    }

    private String cleanProvinceName(String name) {
        if (name == null) return null;

        // 1. 处理自治区、特别行政区和直辖市 (适配 ECharts 地图匹配)
        if (name.contains("内蒙古")) return "内蒙古";
        if (name.contains("广西")) return "广西";
        if (name.contains("西藏")) return "西藏";
        if (name.contains("宁夏")) return "宁夏";
        if (name.contains("新疆")) return "新疆";
        if (name.contains("香港")) return "香港";
        if (name.contains("澳门")) return "澳门";

        // 2. 去掉 "省" 后缀
        if (name.endsWith("省")) {
            return name.substring(0, name.length() - 1);
        }

        // 3. 去掉 "市" 后缀 (如 "北京市" -> "北京", "上海市" -> "上海")
        if (name.endsWith("市")) {
            return name.substring(0, name.length() - 1);
        }

        return name;
    }
}
