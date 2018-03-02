package com.gx.morgan.downloadlib.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * description：文件操作辅助类
 * <br>author：caowugao
 * <br>time： 2017/06/20 17:50
 */

public class FileUtil {
    private FileUtil() {
    }

    public static void closeIO(Closeable... closeables) {
        if (null == closeables) {
            return;
        }
        try {
            for (Closeable closeable : closeables) {
                if (null != closeable) {
                    closeable.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static File getFile(String path) {
        if (null == path || "".equals(path.trim())) {
            return null;
        }

        File file = new File(path);
        if (file.isDirectory()) {
            return null;
        }

        if (!file.exists()) {
            File parentFile = file.getParentFile();
            if (null != parentFile && !parentFile.exists()) {
                parentFile.mkdirs();
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return file;
    }

}
