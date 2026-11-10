package com.grodziec.filmoteka.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Style CSS z classpath
        registry.addResourceHandler("/style/**")
                .addResourceLocations("classpath:/static/style/");
        // Plakaty zapisywane dynamicznie w runtime
        registry.addResourceHandler("/posters/**")
                .addResourceLocations("file:data/posters/");
    }
}
