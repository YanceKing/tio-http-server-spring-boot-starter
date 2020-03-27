package com.yance.configuration;


import com.yance.properties.TioHttpServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.intf.GroupListener;
import org.tio.core.stat.IpStatListener;
import org.tio.http.common.HttpConfig;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.server.HttpServerStarter;
import org.tio.server.ServerTioConfig;
import org.tio.server.intf.ServerAioListener;

import java.io.IOException;

/**
 * @author yance
 */
public final class TioHttpServerBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(TioHttpServerBootstrap.class);


    private TioHttpServerProperties serverProperties;
    private ServerTioConfig serverTioConfig;
    private IpStatListener ipStatListener;
    private GroupListener groupListener;
    private ServerAioListener serverAioListener;
    private HttpServerStarter httpServerStarter;
    private static volatile TioHttpServerBootstrap tioServerBootstrap;
    /**
     * 标记是否初始化，防止重复初始化造成端口冲突问题
     */
    private boolean initialized = false;

    /**
     * 初始化
     * @param serverProperties 配置项
     * @param ipStatListener ip监听器
     * @param groupListener group监听器
     * @param serverAioListener serverAio监听器
     * @return 返回TioHttpServerBootstrap单例对象
     */
    public static TioHttpServerBootstrap getInstance(
            TioHttpServerProperties serverProperties,
            IpStatListener ipStatListener,
            GroupListener groupListener,
            ServerAioListener serverAioListener) {
        if (tioServerBootstrap == null) {
            synchronized (TioHttpServerBootstrap.class) {
                if (tioServerBootstrap == null) {
                    tioServerBootstrap = new TioHttpServerBootstrap(
                            serverProperties,
                            ipStatListener,
                            groupListener,
                            serverAioListener);
                }
            }
        }
        return tioServerBootstrap;

    }

    /**
     * 构造器
     * @param serverProperties 配置项
     * @param ipStatListener ip监听器
     * @param groupListener group监听器
     * @param serverAioListener serverAio监听器
     */
    private TioHttpServerBootstrap(
            TioHttpServerProperties serverProperties,
            IpStatListener ipStatListener,
            GroupListener groupListener,
            ServerAioListener serverAioListener) {

        this.serverProperties = serverProperties;
        this.ipStatListener = ipStatListener;
        this.groupListener = groupListener;
        this.serverAioListener = serverAioListener;
        afterSetProperties();
    }

    private void afterSetProperties() {
        if (this.ipStatListener == null) {
            logger.warn("No instance of IpStatListener found");
        }
        if (this.groupListener == null) {
            logger.warn("No instance of GroupListener found");
        }
        if (this.serverAioListener == null) {
            logger.warn("No instance of serverAioListener found");
        }
    }


    public ServerTioConfig getServerTioConfig() {
        return serverTioConfig;
    }

    public void contextInitialized(HttpConfig httpConfig, HttpRequestHandler requestHandler) {
        if (initialized) {
            logger.info("Tio server has already been initialized.");
            return;
        }
        logger.info("Try to initializing tio server...");
        try {
            initHttpServerStarter(httpConfig, requestHandler);
            initServerTioConfig();
            start();
            initialized = true;
        } catch (Throwable e) {
            logger.error("Cannot bootstrap tio server :", e);
            throw new RuntimeException("Cannot bootstrap tio server :", e);
        }
    }


    private void initHttpServerStarter(HttpConfig httpConfig, HttpRequestHandler requestHandler) {
        this.httpServerStarter = new HttpServerStarter(httpConfig, requestHandler);
    }


    private void initServerTioConfig() {
        serverTioConfig = httpServerStarter.getServerTioConfig();
        if (ipStatListener != null) {
            serverTioConfig.setIpStatListener(ipStatListener);
            serverTioConfig.ipStats.addDurations(serverProperties.getIpStatDurations());
        }
        if (serverAioListener != null) {
            serverTioConfig.setServerAioListener(serverAioListener);
        }
        if (groupListener != null) {
            serverTioConfig.setGroupListener(groupListener);
        }
        if (serverProperties.getHeartbeatTimeout() > 0) {
            serverTioConfig.setHeartbeatTimeout(serverProperties.getHeartbeatTimeout());
        }

        //ssl config
        if (serverProperties.isSslEnabled()) {
            try {
                serverTioConfig.useSsl(serverProperties.getSslKeyStore(), serverProperties.getSslTrustStore(), serverProperties.getSslPassword());
            } catch (Exception e) {
                //catch and log
                logger.error("init ssl config error", e);
            }
        }
    }

    private void start() throws IOException {
        //启动http服务器
        this.httpServerStarter.start();
    }


    /**
     * 关闭 tio 服务，解决 springboot devtools 模式下热启动时无法关闭服务的 Bug
     * support.DefaultLifecycleProcessor : Failed to shut down 1 bean with phase value 0 within timeout of 30000
     */
    public void stop() {
        if (serverProperties.isUseSpringBootDevtools()) {
            if (!initialized) {
                logger.info("Tio Http Server is not yet been initialized.");
                return;
            }
            logger.info("Try to stop Tio Http Server...");
            try {
                httpServerStarter.stop();
            } catch (IOException e) {
                e.printStackTrace();
                logger.info("The Tio Http Server stopped.", e);
            }
            logger.info("The Tio Http Server has been stopped.");
        }
    }
}
