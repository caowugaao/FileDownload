package com.gx.morgan.downloadlib.task;

import android.util.Log;

import com.gx.morgan.downloadlib.config.FileDownloadConfig;
import com.gx.morgan.downloadlib.entity.ThreadInfo;
import com.gx.morgan.downloadlib.https.SSLSocketFactoryHelper;
import com.gx.morgan.downloadlib.record.RecordManager;
import com.gx.morgan.downloadlib.util.FileUtil;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;


/**
 * description：线程下载任务
 * <br>author：caowugao
 * <br>time： 2017/07/10 13:01
 */

public class ThreadDownloadTask implements Runnable {

    private static final String TAG = ThreadDownloadTask.class.getSimpleName();
    private static final boolean DEBUG = true;
    private ThreadInfo threadInfo;
    private IThreadCallback callback;
    private boolean isCancel = false;
    private boolean isRealCancel = false;
    private FileDownloadConfig fileDownloadConfig;
    private RecordManager manager;
    private boolean isCompleted = false;

    public interface IThreadCallback {
        void onThreadDownloadSuccess(ThreadInfo threadInfo);

        void onThreadDownloadFail(ThreadInfo threadInfo, int code, String msg);

        void onThreadDownloading(ThreadInfo threadInfo, int downloadLength, int totalLength);
    }

    public ThreadDownloadTask(ThreadInfo threadInfo, FileDownloadConfig config, IThreadCallback
            callback) {
        this.threadInfo = threadInfo;
        this.callback = callback;
        this.fileDownloadConfig = config;
        manager = RecordManager.getInstance();
    }

    @Override
    public void run() {
        if (isCancel) {
            callback.onThreadDownloadFail(threadInfo, -1, "cancel");
            return;
        }
        download();
    }

    public boolean isCancel() {
        return isRealCancel;
    }

    private void download() {
        int downloadLength = threadInfo.downloadLength;
        RandomAccessFile threadfile = null;
        InputStream inStream = null;
        HttpURLConnection conn = null;
        try {

            URL url = new URL(threadInfo.url);

            if(threadInfo.url.startsWith("https")){
                conn = (HttpsURLConnection) url.openConnection();
                SSLSocketFactory sslSocketFactory = SSLSocketFactoryHelper.getInstance()
                        .getDefaultSSLSocketFactory();
                ((HttpsURLConnection)conn).setSSLSocketFactory(sslSocketFactory);

            }else {
                // 使用Get方式下载
                conn = (HttpURLConnection) url.openConnection();

            }
            if (null == fileDownloadConfig) {
                fileDownloadConfig = FileDownloadConfig.getDefaultConfig();
            }
            conn.setConnectTimeout(fileDownloadConfig.getConnectTimeOut());
            conn.setReadTimeout(fileDownloadConfig.getReadTimeout());
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", fileDownloadConfig.getAcceptFileType());
            conn.setRequestProperty("Accept-Language", fileDownloadConfig.getAcceptLanguage());
            conn.setRequestProperty("Referer", url.toString());
            conn.setRequestProperty("Charset", fileDownloadConfig.getCharSet());

            int startPos = threadInfo.startPos + threadInfo.downloadLength;// 开始位置
//            int endPos = totalLength - 1;// 结束位置
//            int endPos = threadInfo.endPos - 1;// 结束位置
            int endPos = threadInfo.endPos;// 结束位置

//            Log.e("kk", "download: startPos=" + startPos + ",endPos=" + endPos + ",threadInfo.startPos=" + threadInfo
//                    .startPos + ",threadInfo.downloadLength=" + threadInfo.downloadLength);

            conn.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);// 设置获取实体数据的范围
            conn.setRequestProperty("Connection", fileDownloadConfig.getConnType());

            conn.connect();

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                saveLoadedInfo();
                String errorMsg = "response error code=" + code;
                callback.onThreadDownloadFail(threadInfo, code, errorMsg);
                return;
            }
            inStream = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int offset = 0;
            threadfile = new RandomAccessFile(threadInfo.filePath, "rwd");
            threadfile.seek(startPos);
            int totalLength = threadInfo.endPos - threadInfo.startPos;
//            setThreadInfoDownloading(FileThreadInfo.DownloadStatus.STATUS_DOWNLOADING);
            setThreadInfoDownloading();
            Log.e(TAG, "ThreadDownloadTask download: 状态置为下载状态");
//            while (!isCancel && ((offset = inStream.read(buffer, 0, 1024)) != -1)) {
            while (!isCancel && ((offset = inStream.read(buffer, 0, 1024)) >0)) {
                threadfile.write(buffer, 0, offset);
                downloadLength += offset;
                threadInfo.downloadLength = downloadLength;
//                handler.obtainMessage(CALLBACK_LOADDING, threadInfo.totalLength, downloadLength, threadInfo)
//                        .sendToTarget();
//                callback.onThreadDownloading(threadInfo, threadInfo.downloadLength, totalLength);
//                Log.e("kk", "download: offset="+offset );
                callback.onThreadDownloading(threadInfo, offset, totalLength);

            }
            if (isCancel) {//取消下载
                Log.e(TAG, threadInfo.id + "取消下载。。。");
                saveLoadedInfo();
                callback.onThreadDownloadFail(threadInfo, -1, "cancel");
                isRealCancel = true;
//                    deleteNotCompleteRecord();
                return;
            }
            //判断是否已经下载完毕
//            if (downloadLength == totalLength) {//下载完毕
            if (threadInfo.id != ThreadInfo.ILLEGAL_ID) {
//                        manager.delete(threadInfo.id);//删除数据库中记录
//                        manager.updateDownloadLength(threadInfo.id, threadInfo.savePath, threadInfo.totalLength);

//                    manager.setDownloadComplete(threadInfo.id, threadInfo.totalLength);
                manager.deleteThreadInfo(threadInfo.id);
                Log.e(TAG, "download: 下载完毕，删除之前保存的线程id=" + threadInfo.id);
                callback.onThreadDownloadSuccess(threadInfo);
            } else {
                callback.onThreadDownloadSuccess(threadInfo);
                Log.e(TAG, "download: 下载完毕");
            }
            isCompleted = true;
//            }
        } catch (Exception e) {
            Log.e(TAG, "ThreadDownloadTask download: 异常=" + e.getMessage());
            saveLoadedInfo();
            callback.onThreadDownloadFail(threadInfo, -1, e.getMessage());
            e.printStackTrace();
        }finally {
            FileUtil.closeIO(inStream,threadfile);
            if(null!=conn){
                conn.disconnect();
            }
        }
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    private void setThreadInfoDownloading() {
        saveThreadInfoStatus(ThreadInfo.Status.DOWNLOADING);
    }

    private void saveThreadInfoStatus(int status) {
        logError("saveLoadedInfo() downloadLength=" + threadInfo.downloadLength);
        if (threadInfo.id == -1) {//数据库中没有存放
//            manager.insert(threadInfo);
            threadInfo.status = status;
            manager.insertThreadInfo(threadInfo);
        } else {
//            manager.updateDownloadLength(threadInfo.id, threadInfo
//                    .downloadLength);
//            setThreadInfoDownloading(FileThreadInfo.DownloadStatus.STATUS_ERROR);
            threadInfo.status = status;
            manager.updateThreadInfo(threadInfo);
        }
    }

    private void logError(String msg) {
        if (DEBUG) {
            Log.e(TAG, msg);
        }
    }

    /**
     * 保存已下载的线程信息，并且移除
     */
    private void saveLoadedInfo() {
//        logError("saveLoadedInfo() downloadLength=" + threadInfo.downloadLength);
//        if (threadInfo.id == -1) {//数据库中没有存放
////            manager.insert(threadInfo);
//            manager.insertThreadInfo(threadInfo);
//        } else {
////            manager.updateDownloadLength(threadInfo.id, threadInfo
////                    .downloadLength);
////            setThreadInfoDownloading(FileThreadInfo.DownloadStatus.STATUS_ERROR);
//            threadInfo.status = ThreadInfo.Status.ERROR;
//            manager.updateThreadInfo(threadInfo);
//        }
        saveThreadInfoStatus(ThreadInfo.Status.ERROR);
    }

    public void cancel() {
        isCancel = true;
    }
}
