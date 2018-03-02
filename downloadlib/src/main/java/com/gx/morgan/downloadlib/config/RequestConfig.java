package com.gx.morgan.downloadlib.config;



import java.util.Map;

/**
 * description：http请求配置
 * <br>author：caowugao
 * <br>time： 2017/05/03 11:14
 */

public class RequestConfig {

//    public static final int TIME_OUT_CONN_DEFAULT = 20 * 1000; // 默认连接时间20秒
    public static final int TIME_OUT_CONN_DEFAULT = 3 * 1000; // 默认连接时间3秒

    public static final int TIME_OUT_READ_DEFAULT = 20 * 1000; // 默认读取时间20秒

    public static final String CHAR_SET_DEFAULT = "UTF-8";//默认字符集utf-8

    public static final String CONTENT_TYPE_DEFAULT = "application/x-www-form-urlencoded";//默认contentType

    public static final boolean USE_CACHE_DEFAULT = false;//默认不开启缓存

    protected boolean isUseCache;

//    protected IHttpCache cache;

    protected int connectTimeOut; // 连接主机超时时间

    protected int readTimeout; // 从主机读取数据超时时间

    protected String charSet; // 字符编码格式

    protected String cookie;

    protected String contentType;

    protected Map<String, String> requestHeader;//http请求头

    protected volatile static RequestConfig defaultConfig;

    public RequestConfig() {
    }

    /**
     * @features 功    能：默认配置
     * @method 方法名：getDefaultConfig
     * @return RequestConfig
     * @modify 修改者: caowg
     */
    public static RequestConfig getDefaultConfig() {

        if (defaultConfig == null) {
            synchronized (RequestConfig.class) {
                if (defaultConfig == null) {
                    defaultConfig = new RequestConfig();
                    defaultConfig.connectTimeOut = TIME_OUT_CONN_DEFAULT;
                    defaultConfig.readTimeout = TIME_OUT_READ_DEFAULT;
                    defaultConfig.charSet = CHAR_SET_DEFAULT;
                    defaultConfig.isUseCache = USE_CACHE_DEFAULT;
                    defaultConfig.contentType = CONTENT_TYPE_DEFAULT;
                }
            }
        }
        return defaultConfig;
    }

    /**
     * @return Returns the isUseCache.
     */
    public boolean isUseCache() {
        return isUseCache;
    }

    /**
     * @param isUseCache The isUseCache to set.
     */
    public void setUseCache(boolean isUseCache) {
        this.isUseCache = isUseCache;
    }

    /**
     * @return Returns the contentType.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @param contentType The contentType to set.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return Returns the connectTimeOut.
     */
    public int getConnectTimeOut() {
        return connectTimeOut;
    }

    /**
     * @param connectTimeOut The connectTimeOut to set.
     */
    public void setConnectTimeOut(int connectTimeOut) {
        this.connectTimeOut = connectTimeOut;
    }

    /**
     * @return Returns the readTimeout.
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * @param readTimeout The readTimeout to set.
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * @return Returns the charSet.
     */
    public String getCharSet() {
        return charSet;
    }

    /**
     * @param charSet The charSet to set.
     */
    public void setCharSet(String charSet) {
        this.charSet = charSet;
    }

    /**
     * @return Returns the cookie.
     */
    public String getCookie() {
        return cookie;
    }

    /**
     * @param cookie The cookie to set.
     */
    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    /**
     * @return Returns the requestHeader.
     */
    public Map<String, String> getRequestHeader() {
        return requestHeader;
    }

    /**
     * @param requestHeader The requestHeader to set.
     */
    public void setRequestHeader(Map<String, String> requestHeader) {
        this.requestHeader = requestHeader;
    }

//    /**
//     * @return Returns the cache.
//     */
//    public IHttpCache getCache() {
//        return cache;
//    }
//
//    /**
//     * @param cache The cache to set.
//     */
//    public void setCache(IHttpCache cache) {
//        this.cache = cache;
//    }

}

