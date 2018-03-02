package com.gx.morgan.downloadlib.callback;


import com.gx.morgan.downloadlib.entity.FileInfo;

/**
 * description：文件下载回调接口
 * <br>author：caowugao
 * <br>time： 2017/03/16 14:39
 */

public interface FileDownloadCallback {
    void onDownloadPreExecute(FileInfo info);

    void onDownloadSuccess(FileInfo info, int fileTotalLength);

    void onDownloadFailure(FileInfo info, int errorNo, String msg);

    void onDownloading(FileInfo info, int current, int count);
}
