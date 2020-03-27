package com.yance.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 负责添加一个标记，表示 Tio Http Server 已经启用，防止重复启用
 *
 * @author yance
 */
@Configuration
public class TioHttpServerMarkerConfiguration {

    @Bean
    public Marker tioWebSocketServerMarkBean() {
        return new Marker();
    }

    class Marker {
    }
}