package com.bssm.meal.config; // 패키지 경로 확인

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String projectPath = System.getProperty("user.dir");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + projectPath + File.separator + "uploads" + File.separator);
    }
}