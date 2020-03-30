package com.yance.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 获取spring信息的工具类
 *
 * @author yance
 */
public class TioSpringApplication implements ApplicationContextAware {

    private static Logger log = LoggerFactory.getLogger(TioSpringApplication.class);

    private static ApplicationContext applicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        if (applicationContext == null) {
            applicationContext = context;
            log.info("\n[----------------------------------------------------------]\n\t" +
                    "ApplicationContext配置成功:\t在普通类可以通过调用TioSpring.getAppContext()获取applicationContext对象" +
                    "\n[----------------------------------------------------------");
        }
    }

    private static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * @param name 注入名称
     * @return spring容器注入对象
     * @deprecated 建议直接使用TioAutowired注入
     */
    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    /**
     * @param clazz class对象
     * @param <T> spring容器注入对象
     * @return spring容器注入对象
     * @deprecated 建议直接使用TioAutowired注入
     */
    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    /**
     * @param clazz class对象
     * @param <T> spring容器注入对象
     * @return spring容器注入对象
     */
    public <T> T getTioBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }
}