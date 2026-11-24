package com.school.textbook.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * 静态资源与前端单页应用路由回退配置（支持将前端 dist 产物内置于 static 目录单包运行）
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    if (resourcePath.startsWith("api/") || resourcePath.equals("api")) {
                        return null;
                    }
                    Resource requestedResource = location.createRelative(resourcePath);
                    if (requestedResource.exists() && requestedResource.isReadable()) {
                        return requestedResource;
                    }
                    ClassPathResource indexResource = new ClassPathResource("/static/index.html");
                    return indexResource.exists() ? indexResource : null;
                }
            });
    }
}
