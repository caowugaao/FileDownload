package com.gx.morgan.downloadlib.entity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * description：线程下载的信息
 * <br>author：caowugao
 * <br>time： 2017/07/08 13:02
 */

public class ThreadInfo {
    public int id = ILLEGAL_ID;
    public int startPos;
    public int downloadLength;
    public int endPos;
    public String filePath;
    public String url;
    public int fileId;
    public int status;
    /**
     * 顺序，第几个线程
     */
    public int order;
    public static final int ILLEGAL_ID = -1;

    public static class Status {
        //        public static final int INITIALIZE = 0;
//        public static final int COMPLETED = 1;
        public static final int DOWNLOADING = 2;
        public static final int ERROR = 3;
    }

    @Override
    public String toString() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", id);
            jsonObject.put("startPos", startPos);
            jsonObject.put("downloadLength", downloadLength);
            jsonObject.put("endPos", endPos);
            jsonObject.put("filePath", filePath);
            jsonObject.put("url", url);
            jsonObject.put("fileId", fileId);
            jsonObject.put("status", status);
            return jsonObject.toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return super.toString();

    }
}
