package com.iphayao.linebot;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;

@Configuration
@PropertySource("classpath:ouybot.properties")
@Getter
@Setter
@ToString
public class WebConfigure {

    //@Value("${http.restTemplate.readTimeout}")
    private int readTimeout = 60000;
    //@Value("${http.restTemplate.connectTimeout}")
    private int connectTimeout = 60000;

    @Bean
    public RestTemplate myRestTemplate() {
        RestTemplateBuilder builder =  new RestTemplateBuilder().setConnectTimeout(readTimeout)
                .setReadTimeout(connectTimeout);

        return builder.build();
    }
}
