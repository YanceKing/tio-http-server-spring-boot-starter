package com.yance.annotation;

import com.yance.configuration.TioHttpServerMarkerConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 此注解用于启用 Tio Http Server 服务
 *
 * @author yance
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TioHttpServerMarkerConfiguration.class)
public @interface EnableTioHttpServer {
}
