package com.yance.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.tio.http.common.HttpResponse;
import org.tio.http.common.handler.HttpRequestHandler;
import org.tio.http.common.session.id.ISessionIdGenerator;
import org.tio.http.common.session.limiter.SessionRateLimiter;
import org.tio.http.common.view.freemarker.FreemarkerConfig;
import org.tio.utils.cache.ICache;
import org.tio.utils.time.Time;

/**
 * @author yance
 */
@ConfigurationProperties(prefix = "tio.http-server")
public class TioHttpServerProperties {

    private Integer port = 8888;

    private String serverInfo = "t-io";

    private boolean useSession = true;

    private String jsonpParamName = "tio_http_jsonp";

    private int maxLengthOfPostBody = 2097152;

    private boolean isProxied = false;

    private String pageRoot = null;

    private boolean pageInClasspath = false;

    private String page404 = "/404.html";

    private String page500 = "/500.html";

    private String name = null;

    public int maxForwardCount = 10;

    public boolean checkHost = true;

    private String charset = "utf-8";

    private String bindIp = null;

    private boolean appendRequestHeaderString = false;

    private String[] allowDomains = null;

    public boolean compatible1_0 = true;

    private String welcomeFile = null;

    private int maxLiveTimeOfStaticRes = 600;

    private String sessionCacheName = "tio-h-s";

    /**
     * 是否使用了 spring-boot-devtools 热加载模式
     */
    private boolean useSpringBootDevtools = false;

    public boolean monitorFileChange = false;

    private int maxLengthOfMultiBody = 2097152;

    /**
     * 心跳超时时间，超时会自动关闭连接
     */
    private int heartbeatTimeout = 5000;

    /**
     * GroupContext name
     */
    private String groupContextName;

    /**
     * 添加监控时段，不要添加过多的时间段，因为每个时间段都要消耗一份内存，一般加一个时间段就可以了
     */
    private Long[] ipStatDurations = {Time.MINUTE_1};

    private boolean sslEnabled = false;

    private String sslKeyStore;

    private String sslTrustStore;

    private String sslPassword;

    /**
     * 扫描路径，最好是启动类根路径，全盘扫描
     */
    private Class[] ComponentScan;

    private SessionRateLimiter sessionRateLimiter;

    private ISessionIdGenerator sessionIdGenerator;

    private HttpRequestHandler httpRequestHandler;

    private HttpResponse respForBlackIp = null;

    private FreemarkerConfig freemarkerConfig = null;

    private ICache sessionStore = null;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(String serverInfo) {
        this.serverInfo = serverInfo;
    }

    public boolean isUseSession() {
        return useSession;
    }

    public void setUseSession(boolean useSession) {
        this.useSession = useSession;
    }

    public String getJsonpParamName() {
        return jsonpParamName;
    }

    public void setJsonpParamName(String jsonpParamName) {
        this.jsonpParamName = jsonpParamName;
    }

    public int getMaxLengthOfPostBody() {
        return maxLengthOfPostBody;
    }

    public void setMaxLengthOfPostBody(int maxLengthOfPostBody) {
        this.maxLengthOfPostBody = maxLengthOfPostBody;
    }

    public boolean isProxied() {
        return isProxied;
    }

    public void setProxied(boolean proxied) {
        isProxied = proxied;
    }

    public String getPageRoot() {
        return pageRoot;
    }

    public void setPageRoot(String pageRoot) {
        this.pageRoot = pageRoot;
    }

    public boolean isPageInClasspath() {
        return pageInClasspath;
    }

    public void setPageInClasspath(boolean pageInClasspath) {
        this.pageInClasspath = pageInClasspath;
    }

    public String getPage404() {
        return page404;
    }

    public void setPage404(String page404) {
        this.page404 = page404;
    }

    public String getPage500() {
        return page500;
    }

    public void setPage500(String page500) {
        this.page500 = page500;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxForwardCount() {
        return maxForwardCount;
    }

    public void setMaxForwardCount(int maxForwardCount) {
        this.maxForwardCount = maxForwardCount;
    }

    public boolean isCheckHost() {
        return checkHost;
    }

    public void setCheckHost(boolean checkHost) {
        this.checkHost = checkHost;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getBindIp() {
        return bindIp;
    }

    public void setBindIp(String bindIp) {
        this.bindIp = bindIp;
    }

    public boolean isAppendRequestHeaderString() {
        return appendRequestHeaderString;
    }

    public void setAppendRequestHeaderString(boolean appendRequestHeaderString) {
        this.appendRequestHeaderString = appendRequestHeaderString;
    }

    public String[] getAllowDomains() {
        return allowDomains;
    }

    public void setAllowDomains(String[] allowDomains) {
        this.allowDomains = allowDomains;
    }

    public boolean isCompatible1_0() {
        return compatible1_0;
    }

    public void setCompatible1_0(boolean compatible1_0) {
        this.compatible1_0 = compatible1_0;
    }

    public String getWelcomeFile() {
        return welcomeFile;
    }

    public void setWelcomeFile(String welcomeFile) {
        this.welcomeFile = welcomeFile;
    }

    public int getMaxLiveTimeOfStaticRes() {
        return maxLiveTimeOfStaticRes;
    }

    public void setMaxLiveTimeOfStaticRes(int maxLiveTimeOfStaticRes) {
        this.maxLiveTimeOfStaticRes = maxLiveTimeOfStaticRes;
    }

    public String getSessionCacheName() {
        return sessionCacheName;
    }

    public void setSessionCacheName(String sessionCacheName) {
        this.sessionCacheName = sessionCacheName;
    }

    public boolean isUseSpringBootDevtools() {
        return useSpringBootDevtools;
    }

    public void setUseSpringBootDevtools(boolean useSpringBootDevtools) {
        this.useSpringBootDevtools = useSpringBootDevtools;
    }

    public boolean isMonitorFileChange() {
        return monitorFileChange;
    }

    public void setMonitorFileChange(boolean monitorFileChange) {
        this.monitorFileChange = monitorFileChange;
    }

    public int getMaxLengthOfMultiBody() {
        return maxLengthOfMultiBody;
    }

    public void setMaxLengthOfMultiBody(int maxLengthOfMultiBody) {
        this.maxLengthOfMultiBody = maxLengthOfMultiBody;
    }

    public int getHeartbeatTimeout() {
        return heartbeatTimeout;
    }

    public void setHeartbeatTimeout(int heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    public String getGroupContextName() {
        return groupContextName;
    }

    public void setGroupContextName(String groupContextName) {
        this.groupContextName = groupContextName;
    }

    public Long[] getIpStatDurations() {
        return ipStatDurations;
    }

    public void setIpStatDurations(Long[] ipStatDurations) {
        this.ipStatDurations = ipStatDurations;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public String getSslKeyStore() {
        return sslKeyStore;
    }

    public void setSslKeyStore(String sslKeyStore) {
        this.sslKeyStore = sslKeyStore;
    }

    public String getSslTrustStore() {
        return sslTrustStore;
    }

    public void setSslTrustStore(String sslTrustStore) {
        this.sslTrustStore = sslTrustStore;
    }

    public String getSslPassword() {
        return sslPassword;
    }

    public void setSslPassword(String sslPassword) {
        this.sslPassword = sslPassword;
    }

    public Class[] getComponentScan() {
        return ComponentScan;
    }

    public void setComponentScan(Class[] componentScan) {
        ComponentScan = componentScan;
    }

    public SessionRateLimiter getSessionRateLimiter() {
        return sessionRateLimiter;
    }

    public void setSessionRateLimiter(SessionRateLimiter sessionRateLimiter) {
        this.sessionRateLimiter = sessionRateLimiter;
    }

    public ISessionIdGenerator getSessionIdGenerator() {
        return sessionIdGenerator;
    }

    public void setSessionIdGenerator(ISessionIdGenerator sessionIdGenerator) {
        this.sessionIdGenerator = sessionIdGenerator;
    }

    public HttpRequestHandler getHttpRequestHandler() {
        return httpRequestHandler;
    }

    public void setHttpRequestHandler(HttpRequestHandler httpRequestHandler) {
        this.httpRequestHandler = httpRequestHandler;
    }

    public HttpResponse getRespForBlackIp() {
        return respForBlackIp;
    }

    public void setRespForBlackIp(HttpResponse respForBlackIp) {
        this.respForBlackIp = respForBlackIp;
    }

    public FreemarkerConfig getFreemarkerConfig() {
        return freemarkerConfig;
    }

    public void setFreemarkerConfig(FreemarkerConfig freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
    }

    public ICache getSessionStore() {
        return sessionStore;
    }

    public void setSessionStore(ICache sessionStore) {
        this.sessionStore = sessionStore;
    }
}
