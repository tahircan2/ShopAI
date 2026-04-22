package com.shopai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ThreadPoolTaskExecutor taskExecutor;

    public WebMvcConfig(ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // SSE ve DeferredResult gibi asenkron MVC işlemleri için pool tanımla
        configurer.setTaskExecutor(taskExecutor);
        configurer.setDefaultTimeout(60000); // 60 saniye
    }
}
