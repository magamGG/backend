package com.kh.magamGG.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get(uploadDir).toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
        
        // SPA 라우팅: API가 아닌 경로만 처리
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        log.info("🌐 WebMvc 리소스 요청: {}", resourcePath);
                        
                        // API 요청은 제외
                        if (resourcePath.startsWith("api/")) {
                            log.info("🚫 API 요청이므로 WebMvc에서 제외: {}", resourcePath);
                            return null;
                        }
                        
                        Resource requestedResource = location.createRelative(resourcePath);
                        boolean exists = requestedResource.exists() && requestedResource.isReadable();
                        
                        if (exists) {
                            log.info("✅ 정적 파일 발견: {}", resourcePath);
                            return requestedResource;
                        } else {
                            log.info("🔄 SPA 라우팅 - index.html 반환: {}", resourcePath);
                            return new ClassPathResource("/static/index.html");
                        }
                    }
                });
    }
}

