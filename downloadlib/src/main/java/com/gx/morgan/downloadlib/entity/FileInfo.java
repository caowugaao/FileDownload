package com.gx.morgan.downloadlib.entity;

/**
 * description：
 * <br>author：caowugao
 * <br>time： 2017/07/08 12:33
 */
public class FileInfo {
    public int id;
    public String url;
    public String filePath;
    public int downloadLength;
    public int totalLength;
    public long startDownloadTime;
    public long completeDownloadTime;
    public int status = Status.INITIALIZE;

//    public FileInfo(int id, String url, String filePath, int downloadLength, int totalLength, long
//            startDownloadTime, long completeDownloadTime, int status) {
//        this.id = id;
//        this.url = url;
//        this.filePath = filePath;
//        this.downloadLength = downloadLength;
//        this.totalLength = totalLength;
//        this.startDownloadTime = startDownloadTime;
//        this.completeDownloadTime = completeDownloadTime;
//        this.status = status;
//    }

    public static class Status {
        public static final int INITIALIZE = 0;
        public static final int NOT_COMPLETED = 1;
        public static final int COMPLETED = 2;
    }
}
