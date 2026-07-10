package com.ween.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.stream()
                .filter(c -> c instanceof MappingJackson2HttpMessageConverter)
                .forEach(c -> {
                    MappingJackson2HttpMessageConverter converter = (MappingJackson2HttpMessageConverter) c;
                    List<MediaType> supportedMediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
                    if (!supportedMediaTypes.contains(MediaType.APPLICATION_OCTET_STREAM)) {
                        supportedMediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
                        converter.setSupportedMediaTypes(supportedMediaTypes);
                    }
                });
    }
}
