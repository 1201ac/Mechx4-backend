package com.nerlogix.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class SpaWebMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("api")
                                || resourcePath.startsWith("auth/") || resourcePath.startsWith("auth")
                                || resourcePath.startsWith("realtime/") || resourcePath.startsWith("realtime")
                                || resourcePath.startsWith("roads/") || resourcePath.startsWith("roads")
                                || resourcePath.startsWith("vehicles/") || resourcePath.startsWith("vehicles")
                                || resourcePath.startsWith("shipments/") || resourcePath.startsWith("shipments")
                                || resourcePath.startsWith("incidents/") || resourcePath.startsWith("incidents")
                                || resourcePath.startsWith("alerts/") || resourcePath.startsWith("alerts")
                                || resourcePath.startsWith("health")) {
                            return null;
                        }
                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
