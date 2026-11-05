package com.ait.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;

/**
 * Developer utility: generates entity/mapper/service/controller skeletons from
 * an existing PostgreSQL schema.
 *
 * <p>Run it from the project root so the relative output directory resolves:
 *
 * <pre>
 * mvn -pl ecom-generator exec:java \
 *   -Dexec.mainClass=com.ait.generator.CodeGenerator \
 *   -Dexec.args="shop_xxx"
 * </pre>
 *
 * Generated files land under {@code ecom-generator/target/generated} so nothing
 * ever overwrites hand-written code by accident; copy what you need into
 * {@code ecom-mall-core}.
 */
public final class CodeGenerator {

    private static final String URL = System.getenv().getOrDefault(
            "GEN_DB_URL", "jdbc:postgresql://localhost:5432/ecom");
    private static final String USER = System.getenv().getOrDefault("GEN_DB_USER", "postgres");
    private static final String PASSWORD = System.getenv().getOrDefault("GEN_DB_PASSWORD", "postgres");
    private static final String OUT = "target/generated";

    private CodeGenerator() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: CodeGenerator <table_name>[,<table_name>...]");
            System.err.println("       tables are matched as prefixes, e.g. shop_");
            System.exit(1);
        }

        FastAutoGenerator.create(URL, USER, PASSWORD)
                .globalConfig(builder -> builder
                        .author("ecom-generator")
                        .disableOpenDir()
                        .dateType(DateType.TIME_PACK)
                        .commentDate("yyyy-MM-dd")
                        .outputDir(OUT))
                .packageConfig(builder -> builder
                        .parent("com.ait.mall.core")
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .controller("rest")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, OUT + "/mapper")))
                .strategyConfig(builder -> builder
                        .addInclude(args)
                        .addTablePrefix("shop_", "sys_")
                        .entityBuilder()
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .logicDeleteColumnName("deleted")
                            .formatFileName("%s")
                            .build()
                        .mapperBuilder()
                            .enableBaseResultMap()
                            .superClass("com.ait.common.mapper.CoreMapper")
                            .build()
                        .serviceBuilder()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl")
                            .superServiceClass("com.ait.common.service.BaseService")
                            .superServiceImplClass("com.ait.common.service.impl.BaseServiceImpl")
                            .build()
                        .controllerBuilder()
                            .enableRestStyle()
                            .build())
                .templateEngine(new VelocityTemplateEngine())
                .execute();

        System.out.println("generated into " + OUT + " — review before copying into ecom-mall-core");
    }
}
