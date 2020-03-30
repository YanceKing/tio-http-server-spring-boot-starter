package com.yance.handler;

import com.alibaba.fastjson.JSON;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.yance.utils.StringTioUtil;
import com.yance.utils.TioConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.Tio;
import org.tio.http.common.*;
import org.tio.http.common.session.HttpSession;
import org.tio.http.common.session.limiter.SessionRateLimiter;
import org.tio.http.common.session.limiter.SessionRateVo;
import org.tio.http.server.handler.DefaultHttpRequestHandler;
import org.tio.http.server.intf.CurrUseridGetter;
import org.tio.http.server.intf.HttpServerInterceptor;
import org.tio.http.server.mvc.Routes;
import org.tio.http.server.session.SessionCookieDecorator;
import org.tio.http.server.stat.StatPathFilter;
import org.tio.http.server.stat.ip.path.IpAccessStat;
import org.tio.http.server.stat.ip.path.IpPathAccessStat;
import org.tio.http.server.stat.ip.path.IpPathAccessStatListener;
import org.tio.http.server.stat.ip.path.IpPathAccessStats;
import org.tio.http.server.stat.token.TokenAccessStat;
import org.tio.http.server.stat.token.TokenPathAccessStat;
import org.tio.http.server.stat.token.TokenPathAccessStatListener;
import org.tio.http.server.stat.token.TokenPathAccessStats;
import org.tio.http.server.util.ClassUtils;
import org.tio.http.server.util.Resps;
import org.tio.server.ServerChannelContext;
import org.tio.utils.SysConst;
import org.tio.utils.SystemTimer;
import org.tio.utils.cache.caffeine.CaffeineCache;
import org.tio.utils.hutool.ArrayUtil;
import org.tio.utils.hutool.StrUtil;
import org.tio.utils.lock.LockUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 * @author yance
 */
public class CustomDefaultHttpRequestHandler extends DefaultHttpRequestHandler {

    private static Logger log = LoggerFactory.getLogger(CustomDefaultHttpRequestHandler.class);

    private static final Map<Class<?>, MethodAccess> CLASS_METHODACCESS_MAP = new HashMap<>();

    private SessionCookieDecorator sessionCookieDecorator;

    private TokenPathAccessStats tokenPathAccessStats;

    private IpPathAccessStats ipPathAccessStats;

    private HttpServerInterceptor httpServerInterceptor;

    private String contextPath;

    private String suffix;

    private int contextPathLength = 0;

    private int suffixLength = 0;

    private static final String SESSIONRATELIMITER_KEY_SPLIT = "?";

    /**
     * 限流缓存
     */
    private CaffeineCache sessionRateLimiterCache;

    private static final String SESSIONRATELIMITER_CACHENAME = "TIO_HTTP_SESSIONRATELIMITER_CACHENAME";

    /**
     * 把cookie对象存到ChannelContext对象中
     * request.channelContext.setAttribute(SESSION_COOKIE_KEY, sessionCookie);
     */
    private static final String SESSION_COOKIE_KEY = "TIO_HTTP_SESSION_COOKIE";

    public CustomDefaultHttpRequestHandler(HttpConfig httpConfig, Routes routes) throws Exception {
        super(httpConfig, routes);
        this.contextPath = httpConfig.getContextPath();
        this.suffix = httpConfig.getSuffix();
        if (StrUtil.isNotBlank(contextPath)) {
            contextPathLength = contextPath.length();
        }
        if (StrUtil.isNotBlank(suffix)) {
            suffixLength = suffix.length();
        }
        sessionRateLimiterCache = CaffeineCache.register(SESSIONRATELIMITER_CACHENAME, 60 * 1L, null);
    }

    @Override
    public HttpResponse handler(HttpRequest request) throws Exception {
        //自定义处理JSON字符串赋值
        String bodyString = request.getBodyString();
        //只处理JSON字符串并且请求方法为POST的方法，否则调用父类方法处理
        if (StrUtil.isNotBlank(bodyString) && StringTioUtil.isJsonString(bodyString) && TioConstants.STRING_METHOD_POST.equals(request.getRequestLine().method.name())) {
            HttpResponse response = null;
            RequestLine requestLine = request.getRequestLine();
            request.setNeedForward(false);
            if (!checkDomain(request)) {
                Tio.remove(request.channelContext, "过来的域名[" + request.getDomain() + "]不对");
                return null;
            }
            long start = SystemTimer.currTime;
            String path = requestLine.path;
            if (StrUtil.isNotBlank(contextPath)) {
                if (StrUtil.startWith(path, contextPath)) {
                    path = StrUtil.subSuf(path, contextPathLength);
                } else {

                }
            }

            if (StrUtil.isNotBlank(suffix)) {
                if (StrUtil.endWith(path, suffix)) {
                    path = StrUtil.sub(path, 0, path.length() - suffixLength);
                } else {

                }
            }
            requestLine.setPath(path);
            try {
                processCookieBeforeHandler(request, requestLine);
                requestLine = request.getRequestLine();

                Method method = getMethod(request, requestLine);
                path = requestLine.path;
                if (httpServerInterceptor != null) {
                    response = httpServerInterceptor.doBeforeHandler(request, requestLine, response);
                    if (response != null) {
                        return response;
                    }
                }
                if (method == null) {
                    method = getMethod(request, requestLine);
                    path = requestLine.path;
                }
                //流控
                if (httpConfig.isUseSession()) {
                    SessionRateLimiter sessionRateLimiter = httpConfig.sessionRateLimiter;
                    if (sessionRateLimiter != null) {
                        boolean pass = false;

                        HttpSession httpSession = request.getHttpSession();
                        String key = path + SESSIONRATELIMITER_KEY_SPLIT + httpSession.getId();
                        SessionRateVo sessionRateVo = sessionRateLimiterCache.get(key, SessionRateVo.class);
                        if (sessionRateVo == null) {
                            synchronized (httpSession) {
                                sessionRateVo = sessionRateLimiterCache.get(key, SessionRateVo.class);
                                if (sessionRateVo == null) {
                                    sessionRateVo = SessionRateVo.create(path);
                                    sessionRateLimiterCache.put(key, sessionRateVo);
                                    pass = true;
                                }
                            }
                        }

                        if (!pass) {
                            if (sessionRateLimiter.allow(request, sessionRateVo)) {
                                pass = true;
                            }
                        }

                        if (!pass) {
                            response = sessionRateLimiter.response(request, sessionRateVo);
                            return response;
                        }

                        //更新上次访问时间（放在这个位置：被拒绝访问的就不更新lastAccessTime）
                        sessionRateVo.setLastAccessTime(SystemTimer.currTime);
                        sessionRateVo.getAccessCount().incrementAndGet();
                    }
                }
                if (method != null) {
                    String[] paramnames = routes.METHOD_PARAMNAME_MAP.get(method);
                    Class<?>[] parameterTypes = routes.METHOD_PARAMTYPE_MAP.get(method);
                    Object bean = routes.METHOD_BEAN_MAP.get(method);
                    Object obj = null;
                    if (parameterTypes == null || parameterTypes.length == 0) {
                        obj = Routes.BEAN_METHODACCESS_MAP.get(bean).invoke(bean, method.getName(), parameterTypes, (Object) null);
                    } else {
                        //赋值这段代码待重构，先用上
                        Object[] paramValues = new Object[parameterTypes.length];
                        int i = 0;
                        label_3:
                        for (Class<?> paramType : parameterTypes) {
                            try {
                                if (paramType == HttpRequest.class) {
                                    paramValues[i] = request;
                                    continue label_3;
                                } else {
                                    if (paramType == HttpSession.class) {
                                        paramValues[i] = request.getHttpSession();
                                        continue label_3;
                                    } else if (paramType == HttpConfig.class) {
                                        paramValues[i] = httpConfig;
                                        continue label_3;
                                    } else if (paramType == ServerChannelContext.class) {
                                        paramValues[i] = request.channelContext;
                                        continue label_3;
                                    }
                                    Map<String, Object[]> params = request.getParams();
                                    if (params != null) {
                                        if (ClassUtils.isSimpleTypeOrArray(paramType)) {
                                            Object[] value = params.get(paramnames[i]);
                                            if (value != null && value.length > 0) {
                                                if (paramType.isArray()) {
                                                    if (value.getClass() == String[].class) {
                                                        paramValues[i] = StrUtil.convert(paramType, (String[]) value);
                                                    } else {
                                                        paramValues[i] = value;
                                                    }
                                                } else {
                                                    if (value[0] == null) {
                                                        paramValues[i] = null;
                                                    } else {
                                                        if (value[0].getClass() == String.class) {
                                                            paramValues[i] = StrUtil.convert(paramType, (String) value[0]);
                                                        } else {
                                                            paramValues[i] = value[0];
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            paramValues[i] = paramType.newInstance();
                                            paramValues[i] = JSON.parseObject(request.getBodyString().trim(), paramValues[i].getClass());
                                        }
                                    }
                                }
                            } catch (Throwable e) {
                                log.error(request.toString(), e);
                            } finally {
                                i++;
                            }
                        }
                        MethodAccess methodAccess = Routes.BEAN_METHODACCESS_MAP.get(bean);
                        obj = methodAccess.invoke(bean, method.getName(), parameterTypes, paramValues);
                    }

                    if (obj instanceof HttpResponse) {
                        response = (HttpResponse) obj;
                    } else {
                        if (obj == null) {
                            if (method.getReturnType() == HttpResponse.class) {
                                return null;
                            } else {
                                response = Resps.json(request, obj);
                            }
                        } else {
                            response = Resps.json(request, obj);
                        }
                    }
                    return response;
                }
            } catch (Exception e) {
                logError(request, requestLine, e);
                //Resps.html(request, "500--服务器出了点故障", httpConfig.getCharset());
                response = resp500(request, requestLine, e);
                return response;
            } finally {
                try {
                    long time = SystemTimer.currTime;
                    long iv = time - start; //本次请求消耗的时间，单位：毫秒
                    try {
                        processCookieAfterHandler(request, requestLine, response);
                    } catch (Throwable e) {
                        logError(request, requestLine, e);
                    } finally {
                        if (httpServerInterceptor != null) {
                            try {
                                httpServerInterceptor.doAfterHandler(request, requestLine, response, iv);
                            } catch (Exception e) {
                                log.error(requestLine.toString(), e);
                            }
                        }

                        boolean f = statIpPath(request, response, path, iv);
                        if (!f) {
                            return null;
                        }

                        f = statTokenPath(request, response, path, iv);
                        if (!f) {
                            return null;
                        }
                    }
                } catch (Exception e) {
                    log.error(request.requestLine.toString(), e);
                } finally {
                    if (request.isNeedForward()) {
                        request.setForward(true);
                        return handler(request);
                    }
                }
            }
        }
        return super.handler(request);
    }

    private Method getMethod(HttpRequest request, RequestLine requestLine) {
        Method method = null;
        String path = requestLine.path;
        if (routes != null) {
            method = routes.getMethodByPath(path, request);
            path = requestLine.path;
        }
        if (method == null) {
            if (StrUtil.isNotBlank(httpConfig.getWelcomeFile())) {
                if (StrUtil.endWith(path, "/")) {
                    path = path + httpConfig.getWelcomeFile();
                    requestLine.setPath(path);

                    if (routes != null) {
                        method = routes.getMethodByPath(path, request);
                        path = requestLine.path;
                    }
                }
            }
        }
        return method;
    }

    private static MethodAccess getMethodAccess(Class<?> clazz) throws Exception {
        MethodAccess ret = CLASS_METHODACCESS_MAP.get(clazz);
        if (ret == null) {
            LockUtils.runWriteOrWaitRead("_tio_http_h_ma_" + clazz.getName(), clazz, () -> {
                if (CLASS_METHODACCESS_MAP.get(clazz) == null) {
                    CLASS_METHODACCESS_MAP.put(clazz, MethodAccess.get(clazz));
                }
            });
            ret = CLASS_METHODACCESS_MAP.get(clazz);
        }
        return ret;
    }

    private void logError(HttpRequest request, RequestLine requestLine, Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append(SysConst.CRLF).append("remote  :").append(request.getClientIp());
        sb.append(SysConst.CRLF).append("request :").append(requestLine.toString());
        log.error(sb.toString(), e);

    }

    /**
     * 检查域名是否可以访问本站
     *
     * @param request
     * @return
     * @author tanyaowu
     */
    private boolean checkDomain(HttpRequest request) {
        String[] allowDomains = httpConfig.getAllowDomains();
        if (allowDomains == null || allowDomains.length == 0) {
            return true;
        }
        String host = request.getHost();
        if (ArrayUtil.contains(allowDomains, host)) {
            return true;
        }
        return false;
    }

    private void processCookieAfterHandler(HttpRequest request, RequestLine requestLine, HttpResponse httpResponse) throws ExecutionException {
        if (httpResponse == null) {
            return;
        }

        if (!httpConfig.isUseSession()) {
            return;
        }

        HttpSession httpSession = request.getHttpSession();//(HttpSession) channelContext.get();//.getHttpSession();//not null
        //		Cookie cookie = getSessionCookie(request, httpConfig);
        String sessionId = getSessionId(request);

        if (StrUtil.isBlank(sessionId)) {
            createSessionCookie(request, httpSession, httpResponse, false);
            //			log.info("{} 创建会话Cookie, {}", request.getChannelContext(), cookie);
        } else {
            HttpSession httpSession1 = (HttpSession) httpConfig.getSessionStore().get(sessionId);

            if (httpSession1 == null) {//有cookie但是超时了
                createSessionCookie(request, httpSession, httpResponse, false);
            }
        }
    }

    /**
     * 根据session创建session对应的cookie
     * 注意：先有session，后有session对应的cookie
     *
     * @param request
     * @param httpSession
     * @param httpResponse
     * @param forceCreate
     * @return
     * @author tanyaowu
     */
    private void createSessionCookie(HttpRequest request, HttpSession httpSession, HttpResponse httpResponse, boolean forceCreate) {
        if (httpResponse == null) {
            return;
        }

        if (!forceCreate) {
            Object test = request.channelContext.getAttribute(SESSION_COOKIE_KEY);
            if (test != null) {
                return;
            }
        }

        String sessionId = httpSession.getId();
        String domain = getDomain(request);
        String name = httpConfig.getSessionCookieName();
        //Math.max(httpConfig.getSessionTimeout() * 30, 3600 * 24 * 365 * 10);
        long maxAge = 3600 * 24 * 365 * 10;
        Cookie sessionCookie = new Cookie(domain, name, sessionId, maxAge);
        if (sessionCookieDecorator != null) {
            sessionCookieDecorator.decorate(sessionCookie, request, request.getDomain());
        }
        httpResponse.addCookie(sessionCookie);

        httpConfig.getSessionStore().put(sessionId, httpSession);
        request.channelContext.setAttribute(SESSION_COOKIE_KEY, sessionCookie);
        return;
    }

    @Override
    public SessionCookieDecorator getSessionCookieDecorator() {
        return sessionCookieDecorator;
    }

    @Override
    public void setSessionCookieDecorator(SessionCookieDecorator sessionCookieDecorator) {
        this.sessionCookieDecorator = sessionCookieDecorator;
    }

    /**
     * tokenPathAccessStat
     *
     * @param request
     * @param response
     * @param path
     * @param iv
     * @return
     */
    private boolean statTokenPath(HttpRequest request, HttpResponse response, String path, long iv) {
        if (tokenPathAccessStats == null) {
            return true;
        }

        if (response == null) {
            return false;
        }

        if (response.isSkipTokenStat() || request.isClosed()) {
            return true;
        }

        //统计一下Token访问数据
        String token = tokenPathAccessStats.getTokenGetter().getToken(request);
        if (StrUtil.isNotBlank(token)) {
            List<Long> list = tokenPathAccessStats.durationList;

            CurrUseridGetter currUseridGetter = tokenPathAccessStats.getCurrUseridGetter();
            String uid = null;
            if (currUseridGetter != null) {
                uid = currUseridGetter.getUserid(request);
            }

            StatPathFilter statPathFilter = tokenPathAccessStats.getStatPathFilter();

            //添加统计
            for (Long duration : list) {
                TokenAccessStat tokenAccessStat = tokenPathAccessStats.get(duration, token, request.getClientIp(), uid);//.get(duration, ip, path);//.get(v, channelContext.getClientNode().getIp());

                tokenAccessStat.count.incrementAndGet();
                tokenAccessStat.timeCost.addAndGet(iv);
                tokenAccessStat.setLastAccessTime(SystemTimer.currTime);

                if (statPathFilter.filter(path, request, response)) {
                    TokenPathAccessStat tokenPathAccessStat = tokenAccessStat.get(path);
                    tokenPathAccessStat.count.incrementAndGet();
                    tokenPathAccessStat.timeCost.addAndGet(iv);
                    tokenPathAccessStat.setLastAccessTime(SystemTimer.currTime);

                    TokenPathAccessStatListener tokenPathAccessStatListener = tokenPathAccessStats.getListener(duration);
                    if (tokenPathAccessStatListener != null) {
                        boolean isContinue = tokenPathAccessStatListener.onChanged(request, token, path, tokenAccessStat, tokenPathAccessStat);
                        if (!isContinue) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    @Override
    public TokenPathAccessStats getTokenPathAccessStats() {
        return tokenPathAccessStats;
    }

    @Override
    public void setTokenPathAccessStats(TokenPathAccessStats tokenPathAccessStats) {
        this.tokenPathAccessStats = tokenPathAccessStats;
    }

    /**
     * ipPathAccessStat and ipAccessStat
     *
     * @param request
     * @param response
     * @param path
     * @param iv
     * @return
     */
    private boolean statIpPath(HttpRequest request, HttpResponse response, String path, long iv) {
        if (ipPathAccessStats == null) {
            return true;
        }

        if (response == null) {
            return false;
        }

        if (response.isSkipIpStat() || request.isClosed()) {
            return true;
        }

        //统计一下IP访问数据
        String ip = request.getClientIp();//IpUtils.getRealIp(request);

        //		Cookie cookie = getSessionCookie(request, httpConfig);
        String sessionId = getSessionId(request);

        StatPathFilter statPathFilter = ipPathAccessStats.getStatPathFilter();

        //添加统计
        for (Long duration : ipPathAccessStats.durationList) {
            IpAccessStat ipAccessStat = ipPathAccessStats.get(duration, ip);//.get(duration, ip, path);//.get(v, channelContext.getClientNode().getIp());

            ipAccessStat.count.incrementAndGet();
            ipAccessStat.timeCost.addAndGet(iv);
            ipAccessStat.setLastAccessTime(SystemTimer.currTime);
            if (StrUtil.isBlank(sessionId)) {
                ipAccessStat.noSessionCount.incrementAndGet();
            } else {
                ipAccessStat.sessionIds.add(sessionId);
            }

            if (statPathFilter.filter(path, request, response)) {
                IpPathAccessStat ipPathAccessStat = ipAccessStat.get(path);
                ipPathAccessStat.count.incrementAndGet();
                ipPathAccessStat.timeCost.addAndGet(iv);
                ipPathAccessStat.setLastAccessTime(SystemTimer.currTime);

                if (StrUtil.isBlank(sessionId)) {
                    ipPathAccessStat.noSessionCount.incrementAndGet();
                }
                //				else {
                //					ipAccessStat.sessionIds.add(cookie.getValue());
                //				}

                IpPathAccessStatListener ipPathAccessStatListener = ipPathAccessStats.getListener(duration);
                if (ipPathAccessStatListener != null) {
                    boolean isContinue = ipPathAccessStatListener.onChanged(request, ip, path, ipAccessStat, ipPathAccessStat);
                    if (!isContinue) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public IpPathAccessStats getIpPathAccessStats() {
        return ipPathAccessStats;
    }

    @Override
    public void setIpPathAccessStats(IpPathAccessStats ipPathAccessStats) {
        this.ipPathAccessStats = ipPathAccessStats;
    }

    @Override
    public HttpServerInterceptor getHttpServerInterceptor() {
        return httpServerInterceptor;
    }

    @Override
    public void setHttpServerInterceptor(HttpServerInterceptor httpServerInterceptor) {
        this.httpServerInterceptor = httpServerInterceptor;
    }

    private void processCookieBeforeHandler(HttpRequest request, RequestLine requestLine) throws ExecutionException {
        if (!httpConfig.isUseSession()) {
            return;
        }

        String sessionId = getSessionId(request);
        //		Cookie cookie = getSessionCookie(request, httpConfig);
        HttpSession httpSession = null;
        if (StrUtil.isBlank(sessionId)) {
            httpSession = createSession(request);
        } else {
            //			if (StrUtil.isBlank(sessionId)) {
            //				sessionId = cookie.getValue();
            //			}

            httpSession = (HttpSession) httpConfig.getSessionStore().get(sessionId);
            if (httpSession == null) {
                if (log.isInfoEnabled()) {
                    log.info("{} session【{}】超时", request.channelContext, sessionId);
                }

                httpSession = createSession(request);
            }
        }
        request.setHttpSession(httpSession);
    }
}
