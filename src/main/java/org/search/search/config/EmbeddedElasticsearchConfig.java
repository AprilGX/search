package org.search.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 嵌入式Elasticsearch配置
 */
@Configuration
public class EmbeddedElasticsearchConfig {

    @Bean(destroyMethod = "stop")
    public EmbeddedElastic embeddedElastic() throws IOException, InterruptedException {

        String userDir = System.getProperty("user.dir");
        String dataDir = userDir + File.separator + "data" + File.separator + "es-data";
        String logsDir = userDir + File.separator + "data" + File.separator + "es-logs";
        System.out.println("Elasticsearch 数据目录: " + dataDir);
        System.out.println("Elasticsearch 日志目录: " + logsDir);
        
        // 创建必要的目录
        new File(dataDir).mkdirs();
        new File(logsDir).mkdirs();
        
        // 构建安装目录和插件文件
        File installationDir = new File(userDir + File.separator + "elasticsearch-install");
        File pluginFile = new File(userDir + File.separator + "data" + File.separator + "es-plugins" + File.separator + "elasticsearch-analysis-ik-6.8.23.zip");
        String pluginUrl = pluginFile.toURI().toString();
        
        System.out.println("Elasticsearch 安装目录: " + installationDir.getAbsolutePath());
        System.out.println("IK插件路径: " + pluginFile.getAbsolutePath());
        System.out.println("IK插件URL: " + pluginUrl);

        // 增加超时时间到5分钟
        EmbeddedElastic embeddedElastic = EmbeddedElastic.builder()
                .withElasticVersion("6.8.23")
                .withSetting("cluster.name", "spring-embedded-es")
                .withCleanInstallationDirectoryOnStop(false)
                .withSetting("http.port", 9200)
                .withSetting("transport.port", 9300)
                .withInstallationDirectory(installationDir)
                .withPlugin(pluginUrl)

                // 禁用磁盘阈值检查，防止索引被强制只读
                .withSetting("cluster.routing.allocation.disk.threshold_enabled", false)

                .withSetting("path.data", dataDir)
                .withSetting("path.logs", logsDir)
                .withSetting("node.name", "node-1")
                .withSetting("network.host", "127.0.0.1")
                .withSetting("discovery.type", "single-node")
                .withSetting("bootstrap.memory_lock", false)
                .withSetting("action.auto_create_index", true)
                .withSetting("index.store.type", "simplefs")
                .withSetting("indices.fielddata.cache.size", "25%")
                .withSetting("indices.memory.index_buffer_size", "10%")
                //等待时间
                .withStartTimeout(5, TimeUnit.MINUTES)
                .build();

        System.out.println("开始启动 Embedded Elasticsearch 6.8.23...");
        embeddedElastic.start(); // start() 会阻塞直到 ES 启动完成

        System.out.println("Embedded Elasticsearch 已启动，HTTP 端口 9200");
        return embeddedElastic;
    }

    @Bean(destroyMethod = "close")
    public RestHighLevelClient restHighLevelClient() {
        // 直接创建客户端即可
        return new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http"))
        );
    }
}