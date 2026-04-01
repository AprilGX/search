package org.search.search;

import org.lionsoul.ip2region.xdb.Searcher;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class IpTest {
    public static void main(String[] args) throws Exception {
        String ip = "116.149.0.127";
        System.out.println("Testing IP: " + ip);

        // 1. 尝试从 src/main/resources 读取 xdb
        File xdbFile = new File("Search/src/main/resources/ip2region.xdb");
        if (!xdbFile.exists()) {
             // 备用路径
             xdbFile = new File("src/main/resources/ip2region.xdb");
        }
        
        if (!xdbFile.exists()) {
            System.err.println("Error: ip2region.xdb not found at " + xdbFile.getAbsolutePath());
            return;
        }

        byte[] cBuff = Files.readAllBytes(xdbFile.toPath());
        Searcher searcher = Searcher.newWithBuffer(cBuff);

        String region = searcher.search(ip);
        System.out.println("Original Region String: " + region);

        String[] parts = region.split("\\|");
        if (parts.length >= 3) {
            String province = parts[2];
            String city = parts[3];
            System.out.println("Parsed Province: " + province);
            System.out.println("Parsed City: " + city);
            
            System.out.println("Cleaned Province: " + cleanProvinceName(province));
        } else {
            System.out.println("Failed to parse parts.");
        }
    }

    private static String cleanProvinceName(String name) {
        if (name == null) return null;
        if (name.startsWith("内蒙古")) return "内蒙古";
        if (name.startsWith("广西")) return "广西";
        if (name.startsWith("西藏")) return "西藏";
        if (name.startsWith("宁夏")) return "宁夏";
        if (name.startsWith("新疆")) return "新疆";
        if (name.startsWith("香港")) return "香港";
        if (name.startsWith("澳门")) return "澳门";
        
        if (name.endsWith("省")) {
            return name.substring(0, name.length() - 1);
        }
        if (name.endsWith("市")) {
            // 这里就是我们刚才讨论的潜在 BUG 点
            // if (name.length() > 2) return name; 
            return name.substring(0, name.length() - 1);
        }
        return name;
    }
}
