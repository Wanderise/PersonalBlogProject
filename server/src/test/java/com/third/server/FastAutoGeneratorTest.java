package com.third.server;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.HashMap;
import java.util.Map;

public class FastAutoGeneratorTest {
    public static void main(String[] args) {
        String projectPath = System.getProperty("user.dir");

        // 1. 定义物理路径（注意：pathInfo 会直接定位到该目录，不自动拼接包名）
        // 对应 com.third.pojo 包
        String pojoModulePath = projectPath + "/pojo/src/main/java/com/third/pojo/entity";
        // 对应 com.third.server 包
        String serverModulePath = projectPath + "/server/src/main/java";
        String mapperXmlPath = projectPath + "/server/src/main/resources/mapper";

        String databaseUrl = System.getenv().getOrDefault(
                "DB_URL", "jdbc:mysql://localhost:3306/blog?serverTimezone=GMT%2B8");
        String databaseUser = System.getenv().getOrDefault("DB_USERNAME", "root");
        String databasePassword = System.getenv().getOrDefault("DB_PASSWORD", "");

        FastAutoGenerator.create(databaseUrl, databaseUser, databasePassword)
                .globalConfig(builder -> {
                    builder.author("bsgm")
                            .enableSwagger()
                            .disableOpenDir()
                            .outputDir(serverModulePath); // 默认输出到 server
                })
                .packageConfig(builder -> {
                    builder.parent("com.third")      // 统一父包名为 com.third
                            .entity("pojo")           // Entity 依然保持在 com.third.pojo
                            .controller("controller") // 生成到 com.third.controller
                            .mapper("mapper")         // 生成到 com.third.mapper
                            .service("service")       // 生成到 com.third.service
                            .serviceImpl("service.impl") // 生成到 com.third.service.impl
                            .pathInfo(new HashMap<OutputFile, String>() {{
                                // Entity 依然重定向到 pojo 模块
                                put(OutputFile.entity, pojoModulePath);
                                // XML 依然重定向到 server 模块的 resources 下
                                put(OutputFile.xml, mapperXmlPath);
                            }});
                })
                .strategyConfig(builder -> {
                    builder.addInclude("article", "article_tag", "tag")
                            .addTablePrefix("t_", "c_")
                            // --- 策略配置：默认不覆盖已存在的文件 ---
                            .entityBuilder()
                            .enableLombok()
                            .enableChainModel()

                            .controllerBuilder()
                            .enableRestStyle()

                            .serviceBuilder()
                            .formatServiceFileName("%sService")

                            .mapperBuilder()
                            .enableBaseResultMap()
                            .enableBaseColumnList()
                            .mapperAnnotation(org.apache.ibatis.annotations.Mapper.class);
                })
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }
}
