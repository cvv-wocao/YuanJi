package com.example.item.demo35.config;

import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatJarScannerConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addContextCustomizers(context -> {
                StandardJarScanFilter jarScanFilter = new StandardJarScanFilter();
                jarScanFilter.setTldSkip("*.jar");
                context.getJarScanner().setJarScanFilter(jarScanFilter);
            });
        };
    }
}