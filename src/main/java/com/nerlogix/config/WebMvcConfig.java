package com.nerlogix.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/dashboard").setViewName("forward:/index.html");
        registry.addViewController("/map").setViewName("forward:/index.html");
        registry.addViewController("/roads").setViewName("forward:/index.html");
        registry.addViewController("/incidents").setViewName("forward:/index.html");
        registry.addViewController("/shipments").setViewName("forward:/index.html");
        registry.addViewController("/vehicles").setViewName("forward:/index.html");
        registry.addViewController("/alerts").setViewName("forward:/index.html");
        registry.addViewController("/weather").setViewName("forward:/index.html");
        registry.addViewController("/planner").setViewName("forward:/index.html");
        registry.addViewController("/field-report").setViewName("forward:/index.html");
        registry.addViewController("/pending-reports").setViewName("forward:/index.html");
        registry.addViewController("/settings").setViewName("forward:/index.html");
        registry.addViewController("/admin/**").setViewName("forward:/index.html");
    }
}
