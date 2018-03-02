package com.gx.morgan.downloadlib.https;

import javax.net.ssl.SSLSocketFactory;

/**
 * description：
 * <br>author：caowugao
 * <br>time： 2017/11/02 19:30
 */

public class SSLSocketFactoryHelper {
    private volatile static SSLSocketFactoryHelper instance;
    private SSLSocketFactory sslSocketFactory;
    private SSLSocketFactory defaultSSlSocketFactory;

    private SSLSocketFactoryHelper() {
    }

    public static SSLSocketFactoryHelper getInstance() {
        if (null == instance) {
            synchronized (SSLSocketFactoryHelper.class) {
                if (null == instance) {
                    instance = new SSLSocketFactoryHelper();
                }
            }
        }
        return instance;
    }

    public SSLSocketFactory getDefaultSSLSocketFactory() {
        if (null == defaultSSlSocketFactory) {
            defaultSSlSocketFactory = HttpsSSL.getSslSocketFactory(null, null, null).sSLSocketFactory;
        }
        return defaultSSlSocketFactory;
    }


}
