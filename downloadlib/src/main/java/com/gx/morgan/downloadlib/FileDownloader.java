package com.gx.morgan.downloadlib;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.gx.morgan.downloadlib.callback.FileDownloadCallback;
import com.gx.morgan.downloadlib.config.FileDownloadConfig;
import com.gx.morgan.downloadlib.constant.MemoryConstants;
import com.gx.morgan.downloadlib.entity.FileInfo;
import com.gx.morgan.downloadlib.entity.ThreadInfo;
import com.gx.morgan.downloadlib.https.SSLSocketFactoryHelper;
import com.gx.morgan.downloadlib.record.RecordManager;
import com.gx.morgan.downloadlib.task.ThreadDownloadTask;
import com.gx.morgan.downloadlib.util.FileUtil;
import com.gx.morgan.downloadlib.util.Md5Util;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;


/**
 * description：文件下载器 <br>
 * author：caowugao <br>
 * time： 2017/07/10 10:37
 */
public class FileDownloader extends Thread implements ThreadDownloadTask.IThreadCallback {

    private FileDownloadConfig fileDownloadConfig;

    private FileInfo fileInfo;

    private RecordManager manager;

    public FileDownloadCallback callback;

    private static final int CODE_PRE = 50;

    private static final int CODE_DOWNLOADING = 100;

    private static final int CODE_SUCCESS = 200;

    private static final int CODE_ERROR = 502;

    private MainHandler mainHandler;

    // private static final int MAX_THREAD_NUM = (int) (Runtime.getRuntime().availableProcessors() * 1.5);
    private ExecutorService executorService;

    /**
     * 要求可控8个线程以内
     */
    private static final int MAX_REQUEST_THREAD_NUM = 8;

    /**
     * 用于记录下载的task，threadId=task
     */
    private Map<Integer, ThreadDownloadTask> taskMap;

    private int threadDownloadSuccessSize;

    private int totalThreadTaskSize;

    private int threadDownloadFailSize;

    private static final String TAG = FileDownloader.class.getSimpleName();

    private int toalThreadDownloadedLength = 0;

    private boolean isFileInfoError = false;

    private final Semaphore semaphoreLoading;

    private final Semaphore semaphoreSuccess;

    private final Semaphore semaphoreFail;

    private static class MainHandler extends Handler {
        private WeakReference<FileDownloader> fileDownloaderWeakReference;

        public MainHandler(FileDownloader fileDownloader) {
            super(Looper.getMainLooper());
            this.fileDownloaderWeakReference = new WeakReference<FileDownloader>(fileDownloader);
        }

        @Override
        public void handleMessage(Message msg) {
            FileDownloader fileDownloader = fileDownloaderWeakReference.get();
            if (null == fileDownloader) {
                return;
            }
            FileDownloadCallback callback = fileDownloader.callback;
            if (null == callback) {
                return;
            }

            switch (msg.what) {
            case CODE_PRE:
                callback.onDownloadPreExecute(fileDownloader.fileInfo);
                break;
            case CODE_DOWNLOADING:
                // mainHandler.obtainMessage(CODE_DOWNLOADING, downloadLength, totalLength).sendToTarget();
                int downloadLength = msg.arg1;
                int totalLength = msg.arg2;
                callback.onDownloading(fileDownloader.fileInfo, downloadLength, totalLength);
                break;
            case CODE_SUCCESS:
                FileInfo successFileInfo = (FileInfo) msg.obj;
                callback.onDownloadSuccess(successFileInfo, successFileInfo.totalLength);
                break;
            case CODE_ERROR:
                int errorCode = msg.arg1;
                String error = (String) msg.obj;
                callback.onDownloadFailure(fileDownloader.fileInfo, errorCode, error);
                break;
            }
        }
    }

    public static FileDownloader start(FileDownloadConfig config, String url, String filePath, Context context, FileDownloadCallback callback) {

        if (isDownloading(url, filePath, context)) { // 这里url可能是个重定向链接，filePath可能为空,所以这里有可能无法判断是否正在下载
            Log.e(TAG, TAG + " start: 正在下载中....url=" + url);
            return null;
        }

        FileDownloader downloader = new FileDownloader(config, url, filePath, context, callback);
        downloader.start();
        return downloader;
    }

    public static FileDownloader start(String url, String filePath, Context context, FileDownloadCallback callback) {
        if (isDownloading(url, filePath, context)) { // 这里url可能是个重定向链接，filePath可能为空,所以这里有可能无法判断是否正在下载
            Log.e(TAG, TAG + " start: 正在下载中....url=" + url);
            return null;
        }

        FileDownloader downloader = new FileDownloader(url, filePath, context, callback);
        downloader.start();
        return downloader;
    }

    public static FileDownloader start(String url, Context context, FileDownloadCallback callback) {

        if (isDownloading(url, null, context)) { // 这里url可能是个重定向链接，filePath可能为空,所以这里有可能无法判断是否正在下载
            Log.e(TAG, TAG + " start: 正在下载中....url=" + url);
            return null;
        }

        FileDownloader downloader = new FileDownloader(url, context, callback);
        downloader.start();
        return downloader;
    }

    private boolean isDownloading(String url, String path) {
        return isDownloadingByUrl(url) || isDownloadingByPath(path);
    }

    private boolean isDownloadingByUrl(String url) {
        List<ThreadInfo> threadInfos = manager.queryDownloadingThreadInfosByUrl(url);
        return null == threadInfos || threadInfos.isEmpty() ? false : true;
    }

    private boolean isDownloadingByPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        List<ThreadInfo> threadInfos = manager.queryDownloadingThreadInfosByPath(path);
        return null == threadInfos || threadInfos.isEmpty() ? false : true;
    }

    private static boolean isDownloading(String url, String path, Context context) {
        return isDownloadingByUrl(url, context) || isDownloadingByPath(path, context);
    }

    private static boolean isDownloadingByUrl(String url, Context context) {
        RecordManager.getInstance().init(context);
        List<ThreadInfo> threadInfos = RecordManager.getInstance().queryDownloadingThreadInfosByUrl(url);
        return null == threadInfos || threadInfos.isEmpty() ? false : true;
    }

    private static boolean isDownloadingByPath(String path, Context context) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        RecordManager.getInstance().init(context);
        List<ThreadInfo> threadInfos = RecordManager.getInstance().queryDownloadingThreadInfosByPath(path);
        return null == threadInfos || threadInfos.isEmpty() ? false : true;
    }

    /**
     * 取消下载
     * 
     * @param downloader
     */
    public static void cancel(FileDownloader downloader) {
        downloader.cancel();
    }

    // public FileDownloader(FileDownloadConfig config, FileInfo fileInfo, Context context, FileDownloadCallback
    // callback) {
    // this.fileDownloadConfig = config;
    // this.fileInfo = fileInfo;
    // manager = RecordManager.getInstance();
    // manager.init(context);
    // this.callback = callback;
    // mainHandler = new MainHandler(this);
    //
    // checkFileInfo(fileInfo, context, callback);
    // }

    public FileDownloader(FileDownloadConfig config, String url, String filePath, Context context, FileDownloadCallback callback) {
        this.fileDownloadConfig = config;
        this.fileInfo = new FileInfo();
        fileInfo.url = url;
        fileInfo.filePath = filePath;

        manager = RecordManager.getInstance();
        manager.init(context);
        this.callback = callback;
        mainHandler = new MainHandler(this);
        semaphoreLoading = new Semaphore(1);
        semaphoreSuccess = new Semaphore(1);
        semaphoreFail = new Semaphore(1);

        checkFileInfo(fileInfo, context, callback);
    }

    public FileDownloader(String url, String filePath, Context context, FileDownloadCallback callback) {
        // this.fileDownloadConfig = FileDownloadConfig.getDefaultConfig();
        // this.fileInfo = new FileInfo();
        // fileInfo.url = url;
        // fileInfo.filePath = filePath;
        // manager = RecordManager.getInstance();
        // manager.init(context);
        // this.callback = callback;
        // mainHandler = new MainHandler(this);
        //
        // checkFileInfo(fileInfo, context, callback);
        this(null, url, filePath, context, callback);
    }

    public FileDownloader(String url, Context context, FileDownloadCallback callback) {
        // this.fileDownloadConfig = FileDownloadConfig.getDefaultConfig();
        // this.fileInfo = new FileInfo();
        // fileInfo.url = url;
        // manager = RecordManager.getInstance();
        // manager.init(context);
        // this.callback = callback;
        // mainHandler = new MainHandler(this);
        //
        // checkFileInfo(fileInfo, context, callback);
        this(null, url, null, context, callback);
    }

    private void checkFileInfo(FileInfo fileInfo, Context context, FileDownloadCallback callback) {
        if (null == fileInfo) {
            isFileInfoError = true;
            if (null != callback) {
                callback.onDownloadFailure(fileInfo, -1, "fileInfo cannot be NULL");
            }
            return;
        }

        if (TextUtils.isEmpty(fileInfo.url)) {
            isFileInfoError = true;
            if (null != callback) {
                callback.onDownloadFailure(fileInfo, -1, "file url cannot be empty");
            }
            return;
        }

        if (TextUtils.isEmpty(fileInfo.filePath)) {
            String[] splits = fileInfo.url.split("/");
            String apkName = splits[splits.length - 1];
            apkName = Md5Util.hashKeyForDisk(apkName);
            apkName = apkName.replaceAll("%3f", "").replaceAll("[?]", "").replaceAll("\\.", "");
            apkName += ".apk";
            String baseFileDirString = context.getFilesDir().getAbsolutePath();
            fileInfo.filePath = baseFileDirString + File.separator + apkName;
        }

        File filePath = FileUtil.getFile(fileInfo.filePath);
        if (null == filePath) {
            if (null != callback) {
                isFileInfoError = true;
                callback.onDownloadFailure(fileInfo, -1, "filePath illegal!");
            }
        }

    }

    @Override
    public synchronized void start() {
        if (isFileInfoError) {// FileInfo错误直接返回
            return;
        }
        super.start();
    }

    @Override
    public void run() {

        if (isDownloading(fileInfo.url, fileInfo.filePath)) {// 解决正在下载过程中 再次触发同一个下载链接而引起安装错误的问题，不可省略。到这里url可能是个重定向链接，filePath一定非空
            Log.e(TAG, TAG + " run: 正在下载中....url=" + fileInfo.url);
            return;
        }

        // FileInfo recordFileInfo = manager.queryFileInfoByUrl(this.fileInfo.url);
        FileInfo recordFileInfo = manager.queryFileInfoByFilePath(this.fileInfo.filePath);
        File recordPath = null;
        boolean isExist = false;
        if (null != recordFileInfo) {
            recordPath = new File(recordFileInfo.filePath);
            isExist = recordPath.exists();
        }

        if (null != recordFileInfo && recordFileInfo.status == FileInfo.Status.COMPLETED && isExist && null != recordPath && 0 != recordPath.length()) {// 已经下载完成，就直接回调
            // mainHandler.obtainMessage(CODE_SUCCESS, recordFileInfo).sendToTarget();
            Log.d(TAG, "run: 下载完成直接返回");
            sendSuccessMsg(recordFileInfo);
            return;
        }

        if (null != recordPath && isExist && 0 == recordPath.length()) {// 存在，空文件
            manager.deleteFileInfo(recordFileInfo.id);
            Log.e(TAG, "run: 存在，空文件,删除file表记录");
            recordFileInfo = null;// chongzhi
        }

        Log.d(TAG, "run: 下载前回调");
        mainHandler.obtainMessage(CODE_PRE).sendToTarget();// 下载前回调

        if (null != recordFileInfo && recordFileInfo.status == FileInfo.Status.NOT_COMPLETED) {// 没有下载完成，就接着以前下载的线程记录
            Log.d(TAG, "run: 存在未下载完成的记录");
            fileInfo = recordFileInfo;
            continueLocalRecord(recordFileInfo);
            return;
        }

        // 首次下载
        int fileLength = getFileLength(fileInfo.url);
        if (-1 == fileLength || 0 == fileLength) {
            sendErrorMsg(-1, "fileLength ==" + fileLength);
            return;
        }
        fileInfo.totalLength = fileLength;
        fileInfo.startDownloadTime = System.currentTimeMillis();
        manager.insertFileInfo(fileInfo);// 首次先插入数据库

        int Mb2 = 2 * MemoryConstants.MB;
        int Mb3 = 3 * MemoryConstants.MB;
        int Mb5 = 5 * MemoryConstants.MB;
        int Mb6 = 6 * MemoryConstants.MB;
        int Mb10 = 10 * MemoryConstants.MB;
        int Mb20 = 20 * MemoryConstants.MB;
        int Mb30 = 30 * MemoryConstants.MB;
        int Mb50 = 50 * MemoryConstants.MB;
        int Mb100 = 100 * MemoryConstants.MB;
        int Mb200 = 200 * MemoryConstants.MB;

        // if (fileLength <= Mb10) {//10M以内，单线程下载
        // Log.d(TAG, "run: 10M以内，单线程下载");
        // singleThreadDownload(fileLength);
        // } else if (fileLength > Mb10 && fileLength < Mb100) {//10M到100M之间，以10M为块
        // Log.d(TAG, "run: 10M到100M之间，以10M为块");
        // separateDownload(fileLength, Mb10);
        // } else if (fileLength > Mb100 && fileLength < Mb200) {//100M到200M之间，以20M为块
        // Log.d(TAG, "run: 100M到200M之间，以20M为块");
        // separateDownload(fileLength, Mb20);
        // } else {
        // Log.d(TAG, "run: 200M以上，以50M为块");
        // separateDownload(fileLength, Mb50);
        // int blockSize = fileLength / MAX_REQUEST_THREAD_NUM;
        // }
        if (fileLength < Mb2) {// 2M以内，单线程下载
            Log.d(TAG, "run: 2M以内，单线程下载");
            singleThreadDownload(fileLength);
        }
        else if(fileLength>=Mb2&&fileLength<Mb10){//2M到10M之间，以2M为块
            Log.d(TAG, "run: 2M到10M之间，以2M为块");
            separateDownload(fileLength, Mb2);
        }
        else if(fileLength >=Mb10&&fileLength<Mb50){//10M到50M之间，以5M为块
            Log.d(TAG, "run: 10M到50M之间，以5M为块");
            separateDownload(fileLength, Mb5);
        }
        else if (fileLength >=Mb50 && fileLength < Mb100) {// 50M到100M之间，以10M为块
            Log.d(TAG, "run: 50M到100M之间，以10M为块");
            separateDownload(fileLength, Mb10);
        } else if (fileLength >= Mb100 && fileLength < Mb200) {// 100M到200M之间，以20M为块
            Log.d(TAG, "run: 100M到200M之间，以20M为块");
            separateDownload(fileLength, Mb20);
        } else {
            Log.d(TAG, "run: 200M以上，以最大" + MAX_REQUEST_THREAD_NUM + "个线程");
            int blockSize = fileLength / MAX_REQUEST_THREAD_NUM;
            separateDownload(fileLength, blockSize);
        }

    }

    /**
     * 单线程下载
     * 
     * @param fileLength
     */
    private void singleThreadDownload(int fileLength) {
        ThreadInfo threadInfo = new ThreadInfo();
        threadInfo.startPos = 0;
        threadInfo.downloadLength = 0;
        threadInfo.endPos = fileLength;
        threadInfo.filePath = fileInfo.filePath;
        threadInfo.url = fileInfo.url;
        threadInfo.fileId = fileInfo.id;
        threadInfo.order = 0;
        threadInfo.status = 0;
        manager.insertThreadInfo(threadInfo);

        taskMap = new ConcurrentHashMap<>(1);
        // threadDownloadSuccessSize = 1;
        totalThreadTaskSize = 1;
        // threadDownloadFailSize = 1;
        ThreadDownloadTask singleTask = new ThreadDownloadTask(threadInfo, fileDownloadConfig, this);
        taskMap.put(threadInfo.id, singleTask);
        singleTask.run();
    }

    /**
     * 分段下载
     * 
     * @param fileLength
     *            文件大小
     * @param blockSize
     *            分段的块大小
     */
    private void separateDownload(int fileLength, int blockSize) {
        int requestThreadNum = fileLength / blockSize;
        int remainder = fileLength - requestThreadNum * blockSize;
        requestThreadNum = 0 == remainder ? requestThreadNum : requestThreadNum + 1;
        Log.d(TAG, "separateDownload: 分" + requestThreadNum + "个线程下载");
        initExecutors(requestThreadNum);
        taskMap = new ConcurrentHashMap<>(requestThreadNum);
        // threadDownloadSuccessSize = requestThreadNum;
        totalThreadTaskSize = requestThreadNum;
        // threadDownloadFailSize = requestThreadNum;
        int lastEndPos = 0;
        for (int i = 0; i < requestThreadNum; i++) {
            int startPos = blockSize * i;
            int endPos = blockSize * (i + 1) - 1;
            Log.e(TAG, "separateDownload: startPos=" + startPos + ",endPos=" + endPos);
            if (i == requestThreadNum - 1) {// 最后一个
                if (0 != remainder) {// 有余数
                    startPos = lastEndPos;
                    endPos = fileLength - 1;
                    Log.e(TAG, "separateDownload: 有余数 startPos=" + startPos + ",endPos=" + endPos);
                }
            }
            lastEndPos = endPos;

            ThreadInfo threadInfo = new ThreadInfo();
            threadInfo.startPos = startPos;
            threadInfo.downloadLength = 0;
            threadInfo.endPos = endPos;
            threadInfo.filePath = fileInfo.filePath;
            threadInfo.url = fileInfo.url;
            threadInfo.fileId = fileInfo.id;
            threadInfo.order = i;
            threadInfo.status = 0;
            manager.insertThreadInfo(threadInfo);

            ThreadDownloadTask task = new ThreadDownloadTask(threadInfo, fileDownloadConfig, this);
            taskMap.put(threadInfo.id, task);
            executeTask(task);
        }
    }

    private void sendErrorMsg(int code, String error) {
        mainHandler.obtainMessage(CODE_ERROR, code, -1, error).sendToTarget();
    }

    /**
     * 继续下载本地记录录
     * 
     * @param fileInfo
     */
    private void continueLocalRecord(FileInfo fileInfo) {
        List<ThreadInfo> threadInfos = manager.queryThreadInfoByFileId(fileInfo.id);
        print(threadInfos);
        if (null != threadInfos && !threadInfos.isEmpty()) {
            int size = threadInfos.size();
            initExecutors(size);
            taskMap = new ConcurrentHashMap<>(size);
            // threadDownloadSuccessSize = size;
            totalThreadTaskSize = size;
            // threadDownloadFailSize = size;
            Log.e(TAG, "continueLocalRecord: 继续下载...totalThreadTaskSize=" + totalThreadTaskSize);
            for (ThreadInfo info : threadInfos) {
                if (info.status == ThreadInfo.Status.ERROR) {
                    ThreadDownloadTask task = new ThreadDownloadTask(info, fileDownloadConfig, this);
                    taskMap.put(info.id, task);

                    toalThreadDownloadedLength += info.downloadLength;
                    executeTask(task);
                } else if (info.status == ThreadInfo.Status.DOWNLOADING) {// 正在下载中就重新下载
                    Log.e(TAG, "continueLocalRecord: 正在下载中就重新下载");
                    ThreadDownloadTask task = new ThreadDownloadTask(info, fileDownloadConfig, this);
                    taskMap.put(info.id, task);

                    toalThreadDownloadedLength += info.downloadLength;
                    executeTask(task);
                }
            }
            sendThreadDownloadingMsg(toalThreadDownloadedLength, fileInfo.totalLength);
        }
    }

    private void print(List<ThreadInfo> threadInfos) {
        if (null == threadInfos || threadInfos.isEmpty()) {
            Log.e("kk", "print: threadInfos为null");
            return;
        }
        Log.e("kk", "print: start======================");
        for (ThreadInfo info : threadInfos) {
            Log.e("kk", info.toString());
        }
        Log.e("kk", "print: end======================");
    }

    private void sendThreadDownloadingMsg(int downloadLength, int totalLength) {
        mainHandler.obtainMessage(CODE_DOWNLOADING, downloadLength, totalLength).sendToTarget();
    }

    private void initExecutors(int maxThreadNum) {
        if (null == executorService) {
            // int realThreadNum = maxThreadNum > MAX_THREAD_NUM ? MAX_THREAD_NUM : maxThreadNum;
            // Log.e(TAG, "initExecutors: 实际上分" + realThreadNum + "个线程下载" + ",MAX_THREAD_NUM=" + MAX_THREAD_NUM + "," +
            // "maxThreadNum=" + maxThreadNum);
            int realThreadNum = maxThreadNum;
            executorService = Executors.newFixedThreadPool(realThreadNum);
        }
    }

    private void executeTask(Runnable runnable) {
        executorService.execute(runnable);
    }

    private void sendSuccessMsg(FileInfo fileInfo) {
        mainHandler.obtainMessage(CODE_SUCCESS, fileInfo).sendToTarget();
    }

    public boolean isCancelAll() {
        if (null == taskMap || taskMap.isEmpty()) {
            return true;
        }
        Collection<ThreadDownloadTask> tasks = taskMap.values();
        for (ThreadDownloadTask task : tasks) {
            if (!task.isCancel()) {
                return false;
            }
        }
        return true;
    }

    public void release() {
        if (null != taskMap) {
            Collection<ThreadDownloadTask> tasks = taskMap.values();
            for (ThreadDownloadTask task : tasks) {
                if (!task.isCancel()) {
                    task.cancel();
                }
            }
            taskMap.clear();
            taskMap = null;
        }

        try {
            if (null != executorService && !executorService.isShutdown()) {
                executorService.shutdown();
                executorService = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        // TODO: 2017/7/10 0010 停止
        if (null != taskMap) {
            Collection<ThreadDownloadTask> tasks = taskMap.values();
            for (ThreadDownloadTask task : tasks) {
                task.cancel();
            }
            // taskMap.clear();
        }

        // try {
        // if (null != executorService) {
        // executorService.shutdown();
        // }
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

    }

    /**
     * @return int
     * @features 功    能：获取文件大小
     */
    private int getFileLength(String url) {
        HttpURLConnection conn = null;
        try {
            int fileSize = 0;
            URL urlEntity = new URL(fileInfo.url);
            if (null == fileDownloadConfig) {
                fileDownloadConfig = FileDownloadConfig.getDefaultConfig();
            }
            if (url.startsWith("https")) {
                conn = (HttpsURLConnection) urlEntity.openConnection();
                SSLSocketFactory sslSocketFactory = SSLSocketFactoryHelper.getInstance().getDefaultSSLSocketFactory();
                ((HttpsURLConnection) conn).setSSLSocketFactory(sslSocketFactory);
            } else {
                conn = (HttpURLConnection) urlEntity.openConnection();
            }
            conn.setConnectTimeout(30 * 1000);
            conn.setRequestMethod("GET");
            // conn.setRequestProperty(
            // "Accept",
            // "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash,
            // application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap,
            // application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint,
            // application/msword, */*");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("Accept", fileDownloadConfig.getAcceptFileType());
            conn.setRequestProperty("Accept-Language", fileDownloadConfig.getAcceptLanguage());
            conn.setRequestProperty("Referer", urlEntity.toString());
            conn.setRequestProperty("Charset", fileDownloadConfig.getCharSet());
            conn.setRequestProperty("Connection", fileDownloadConfig.getConnType());
            conn.connect();
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                // 根据响应获取文件大小
                fileSize = conn.getContentLength();
            } else if (responseCode >= 300 && responseCode < 400) {// 重定向
                String location = conn.getHeaderField("Location");
                Log.e(TAG, "getFileLength: location=" + location);
                if (TextUtils.isEmpty(location)) {
                    return -1;
                }
                fileInfo.url = location;
                conn.disconnect();
                return getFileLength(location);

            }
            Log.e(TAG, "getFileLength: fileSize=" + fileSize);
            return fileSize;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return 0;
    }

    @Override
    public void onThreadDownloadSuccess(ThreadInfo threadInfo) {
        try {
            semaphoreSuccess.acquire();
            Log.d(TAG, "onThreadDownloadSuccess:threadInfo.id=" + threadInfo.id);
            taskMap.remove(threadInfo.id);
            threadDownloadSuccessSize++;
            if (totalThreadTaskSize == threadDownloadSuccessSize) {// 全部线程下载完成才回调成功
                Log.e(TAG, "onThreadDownloadSuccess: 下载完成");
                fileInfo.status = FileInfo.Status.COMPLETED;
                fileInfo.downloadLength = fileInfo.totalLength;
                fileInfo.completeDownloadTime = System.currentTimeMillis();
                manager.updateFileInfo(fileInfo);
                sendSuccessMsg(fileInfo);
                release();
            }
            semaphoreSuccess.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
            release();
        }

    }

    @Override
    public void onThreadDownloadFail(ThreadInfo threadInfo, int code, String msg) {

        try {
            semaphoreFail.acquire();

            Log.e(TAG, "onThreadDownloadFail: threadInfo.id=" + threadInfo.id + ",code=" + code + ",msg=" + msg);

            taskMap.remove(threadInfo.id);
            // threadDownloadSuccessSize--;
            threadDownloadFailSize++;
            if (totalThreadTaskSize == threadDownloadFailSize || totalThreadTaskSize == threadDownloadSuccessSize + threadDownloadFailSize) {// 有一个线程下载失败就回调失败
                Log.e(TAG, "onThreadDownloadFail: 下载失败");
                fileInfo.status = FileInfo.Status.NOT_COMPLETED;
                fileInfo.downloadLength = toalThreadDownloadedLength;
                manager.updateFileInfo(fileInfo);
                sendErrorMsg(code, msg);
                release();
            }
            semaphoreFail.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
            release();
        }

    }

    @Override
    public void onThreadDownloading(ThreadInfo threadInfo, int downloadLength, int totalLength) {

        try {
            semaphoreLoading.acquire();

            // Log.d(TAG, "onThreadDownloading: threadInfo.id=" + threadInfo.id + ",downloadLength=" + downloadLength + "," +
            // "totalLength=" + totalLength);
            // Log.d("kk", "onThreadDownloading: threadInfo.id=" + threadInfo.id + ",downloadLength=" + downloadLength +
            // "," +
            // "totalLength=" + totalLength);

            toalThreadDownloadedLength += downloadLength;
            // Log.e("kk", "onThreadDownloading: threadInfo.id=" + threadInfo.id + "toalThreadDownloadedLength=" +
            // toalThreadDownloadedLength + ",fileInfo.totalLength=" + fileInfo.totalLength);
            sendThreadDownloadingMsg(toalThreadDownloadedLength, fileInfo.totalLength);
            semaphoreLoading.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
            release();
        }

    }

}
