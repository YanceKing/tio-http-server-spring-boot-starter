package com.yance.annotation;

import java.lang.annotation.*;

/**
 * @author yance
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TioAutowired {
}
