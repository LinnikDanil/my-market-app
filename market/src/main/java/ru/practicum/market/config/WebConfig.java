package ru.practicum.market.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    @Value("${image.path}")
    private String imagePath;
    @Value("${image.resource-handler-pattern}")
    private String imageResourceHandlerPattern;

    /**
     * Регистрирует обработчик статических изображений, загруженных администратором.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(imageResourceHandlerPattern)
                .addResourceLocations("file:" + imagePath + "/");
    }

}
