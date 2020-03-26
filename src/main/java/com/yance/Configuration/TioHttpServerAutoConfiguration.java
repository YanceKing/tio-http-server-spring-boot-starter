package com.yance.Configuration;

import com.yance.properties.TioHttpServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.tio.core.intf.GroupListener;
import org.tio.core.stat.IpStatListener;
import org.tio.http.common.HttpConfig;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.server.handler.DefaultHttpRequestHandler;
import org.tio.server.ServerTioConfig;
import org.tio.server.intf.ServerAioListener;

import java.io.IOException;

/**
 * @author yance
 */
@Configuration
@Import(TioHttpServerInitializerConfiguration.class)
@EnableConfigurationProperties(TioHttpServerProperties.class)
@ConditionalOnBean(TioHttpServerMarkerConfiguration.Marker.class)
public class TioHttpServerAutoConfiguration {

    @Autowired(required = false)
    private ServerAioListener serverAioListener;

    @Autowired(required = false)
    private IpStatListener ipStatListener;

    @Autowired
    private TioHttpServerProperties tioHttpServerProperties;

    @Autowired(required = false)
    private GroupListener groupListener;


    /**
     * HttpConfig
     * @param properties 配置项
     * @return HttpConfig
     * @throws IOException 异常
     */
    @Bean
    @ConditionalOnMissingBean
    public HttpConfig httpConfig(TioHttpServerProperties properties) throws IOException {
        HttpConfig httpConfig = new HttpConfig(properties.getPort(), properties.isUseSession());
        httpConfig.setSessionCacheName(properties.getSessionCacheName());
        httpConfig.setWelcomeFile(properties.getWelcomeFile());
        httpConfig.setServerInfo(properties.getServerInfo());
        httpConfig.setUseSession(properties.isUseSession());
        httpConfig.setProxied(properties.isProxied());
        httpConfig.setPageRoot(properties.getPageRoot());
        httpConfig.setJsonpParamName(properties.getJsonpParamName());
        httpConfig.setMaxLiveTimeOfStaticRes(properties.getMaxLiveTimeOfStaticRes());
        httpConfig.setMaxLengthOfPostBody(properties.getMaxLengthOfPostBody());
        httpConfig.setMaxForwardCount(httpConfig.getMaxForwardCount());
        httpConfig.setMonitorFileChange(httpConfig.isMonitorFileChange());
        httpConfig.setName(properties.getName());
        httpConfig.setPage500(properties.getPage500());
        httpConfig.setPage404(properties.getPage404());
        httpConfig.setPageInClasspath(properties.isPageInClasspath());
        httpConfig.setCheckHost(properties.isCheckHost());
        httpConfig.setBindIp(properties.getBindIp());
        httpConfig.setCompatible1_0(properties.isCompatible1_0());
        httpConfig.setCharset(properties.getCharset());
        httpConfig.setAppendRequestHeaderString(properties.isAppendRequestHeaderString());
        httpConfig.setAllowDomains(properties.getAllowDomains());
        httpConfig.setMaxLengthOfMultiBody(properties.getMaxLengthOfMultiBody());
        return httpConfig;
    }

    /**
     * 启动项
     * @return TioHttpServerBootstrap
     */
    @Bean
    public TioHttpServerBootstrap tioHttpServerBootstrap() {
        return TioHttpServerBootstrap.getInstance(
                tioHttpServerProperties,
                ipStatListener,
                groupListener,
                serverAioListener);
    }

    /**
     * ServerTioConfig对象
     * @param bootstrap TioHttpServerBootstrap
     * @return ServerTioConfig对象
     */
    @Bean
    public ServerTioConfig serverGroupContext(TioHttpServerBootstrap bootstrap) {
        return bootstrap.getServerTioConfig();
    }

    /**
     * http请求处理器以及路由处理
     * @param httpConfig 系统配置项
     * @param properties 自定义配置项
     * @return Http处理器
     * @throws Exception 异常
     */
    @Bean
    @ConditionalOnMissingBean
    public HttpRequestHandler httpRequestHandler(HttpConfig httpConfig, TioHttpServerProperties properties) throws Exception {
        DefaultHttpRequestHandler defaultHttpRequestHandler = new DefaultHttpRequestHandler(httpConfig, properties.getComponentScan());
        return defaultHttpRequestHandler;
    }
}
