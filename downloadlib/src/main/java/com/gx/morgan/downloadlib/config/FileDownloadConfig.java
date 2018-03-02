package com.gx.morgan.downloadlib.config;


import com.gx.morgan.downloadlib.constant.HttpContentType;

/**
 * description：文件下载配置
 * <br>author：caowugao
 * <br>time： 2017/03/15 20:58
 */

public class FileDownloadConfig extends RequestConfig {

    /**
     * 支持的图片文件
     */
    private static final String IMAGE_FILE = HttpContentType.IMAGE_GIF + "," + HttpContentType.IMAGE_JPG + "," +
            HttpContentType.IMAGE_PNG;

    /**
     * 支持的微软文件
     */
    private static final String MICROSOFT_FILE = HttpContentType.XLS + "," + HttpContentType.DOC + "," +
            HttpContentType.PPT;

    /**
     * 支持的视频文件
     */
    private static final String VIDEO_FILE = HttpContentType.VIDEO_MP4 + "," + HttpContentType.VIDEO_RMVB;

    /**
     * 支持的音频文件
     */
    private static final String AUDIO_FILE = HttpContentType.AUDIO_MP4 + "," + HttpContentType.AUDIO_WAV;

    /**
     * 默认请求的文件类型
     */
    public static final String ACCEPT_FILE_TYPE_DEFAULT = IMAGE_FILE + "," + HttpContentType.APK + "," +
            MICROSOFT_FILE + "," + HttpContentType.XML
            + "," + HttpContentType.JSON + "," + VIDEO_FILE + "," + AUDIO_FILE + ",*/*";

    /**
     * 默认请求得语言
     */
    public static final String ACCEPT_LANGUAGE_DEFAULT = "zh-CN";

    /**
     * 默认请求连接的类型
     */
    public static final String CONN_TYPE_DEFAULT = "Keep-Alive";

    /**
     * 文件下载默认连接时间
     */
    public static final int TIME_OUT_FILE_DOWNLOAD_CONN_DEFAULT = 30000;

    /**
     * 文件下载默认读取时间
     */
    public static final int TIME_OUT_FILE_DOWNLOAD_READ_DEFAULT = 30000;

    private String acceptFileType;//请求的文件类型

    private String acceptLanguage;//请求的语言

    private String connType;//请求连接的类型

    public FileDownloadConfig() {
    }

    /**
     * 功    能：默认配置
     *
     * @return RequestConfig
     */
    public static FileDownloadConfig getDefaultConfig() {
        FileDownloadConfig config = new FileDownloadConfig();
        config.connectTimeOut = TIME_OUT_FILE_DOWNLOAD_CONN_DEFAULT;
        config.readTimeout = TIME_OUT_FILE_DOWNLOAD_READ_DEFAULT;
        config.charSet = CHAR_SET_DEFAULT;
        config.isUseCache = USE_CACHE_DEFAULT;
        config.acceptFileType = ACCEPT_FILE_TYPE_DEFAULT;
        config.acceptLanguage = ACCEPT_LANGUAGE_DEFAULT;
        config.connType = CONN_TYPE_DEFAULT;
        return config;
    }

    /**
     * @return Returns the acceptFileType.
     */
    public String getAcceptFileType() {
        return acceptFileType;
    }

    /**
     * @param acceptFileType The acceptFileType to set.
     */
    public void setAcceptFileType(String acceptFileType) {
        this.acceptFileType = acceptFileType;
    }

    /**
     * @return Returns the acceptLanguage.
     */
    public String getAcceptLanguage() {
        return acceptLanguage;
    }

    /**
     * @param acceptLanguage The acceptLanguage to set.
     */
    public void setAcceptLanguage(String acceptLanguage) {
        this.acceptLanguage = acceptLanguage;
    }

    /**
     * @return Returns the connType.
     */
    public String getConnType() {
        return connType;
    }

    /**
     * @param connType The connType to set.
     */
    public void setConnType(String connType) {
        this.connType = connType;
    }

}
