package com.follow_coin.follow_coin_compute.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ConfBean {

    @Bean
    public WebClient.Builder lbWebClient() {
        return WebClient.builder();
    }


}
