package com.yance.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.tio.http.common.HttpConfig;
import org.tio.http.common.handler.HttpRequestHandler;

/**
 * @author yance
 */
@Configuration
public class TioHttpServerInitializerConfiguration
        implements SmartLifecycle, Ordered {

    private int order = 1;
    private boolean running = false;

    @Autowired
    private TioHttpServerBootstrap tioHttpServerBootstrap;

    @Autowired
    private HttpConfig httpConfig;

    @Autowired
    private HttpRequestHandler httpRequestHandler;


    @Override
    public void start() {
        new Thread(() -> {
            tioHttpServerBootstrap.contextInitialized(httpConfig, httpRequestHandler);
            running = true;
        }).start();
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        tioHttpServerBootstrap.stop();
        // 如果你让 isRunning() 返回true，需要执行 stop 这个方法，那么就不要忘记调用 callback.run()。
        // 否则在你程序退出时，Spring 的 DefaultLifecycleProcessor 会认为你这个 TestSmartLifecycle 没有 stop 完成，
        // 程序会一直卡着结束不了，等待一定时间（默认超时时间30秒）后才会自动结束。
        callback.run();
        running = false;
    }

}
