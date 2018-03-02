package com.gx.morgan.downloadlib.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * description： Md5辅助类
 * <br>author：caowugao
 * <br>time： 2017/07/17 17:26
 */

public class Md5Util {
    private Md5Util() {
    }

    /**
     * md5字符串进行加密
     *
     * @param strings
     * @return
     */
    public static String hashKeyForDisk(String strings) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(strings.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(strings.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
