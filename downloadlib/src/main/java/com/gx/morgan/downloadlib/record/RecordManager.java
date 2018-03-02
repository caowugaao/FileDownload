package com.gx.morgan.downloadlib.record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.gx.morgan.downloadlib.entity.FileInfo;
import com.gx.morgan.downloadlib.entity.ThreadInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * description：记录管理器
 * <br>author：caowugao
 * <br>time： 2017/07/08 18:59
 */

public class RecordManager {
    private static final String TAG = RecordManager.class.getSimpleName();
    private  volatile static RecordManager instance;
    private SQLiteDatabase db;
    private static final Object LOCK_FILE = new Object();
    private static final Object LOCK_THREAD = new Object();

    private RecordManager() {
    }

    public static RecordManager getInstance() {
        if (null == instance) {
            synchronized (RecordManager.class) {
                if (null == instance) {
                    instance = new RecordManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        if (null == db) {
            DBOpenHelper helper = new DBOpenHelper(context);
            db = helper.getWritableDatabase();
        }
    }

    private List<FileInfo> queryFileInfosByStatus(int status) {
        String sql = "select * from " + FileTable.TABLE + " where " + FileTable.COL_STATUS + "=?";
        String[] selectionArgs={String.valueOf(status)};
        return queryFileInfos(sql,selectionArgs);
    }


    public List<FileInfo> queryNotCompeletedFileInfo() {
        return queryFileInfosByStatus(FileInfo.Status.NOT_COMPLETED);
    }

    public List<FileInfo> queryCompeletedFileInfos() {
        return queryFileInfosByStatus(FileInfo.Status.COMPLETED);
    }

    public List<ThreadInfo> queryNotCompeletedThreadInfos() {
        return queryThreadInfosByStatus(ThreadInfo.Status.ERROR);
    }

    public List<ThreadInfo> queryDownloadingThreadInfos() {
        return queryThreadInfosByStatus(ThreadInfo.Status.DOWNLOADING);
    }

    private List<ThreadInfo> queryThreadInfosByStatus(int status) {
        String sql = "select * from " + ThreadTable.TABLE + " where " + ThreadTable.COL_STATUS + "=?";
        String[] selectionArgs = {String.valueOf(status)};
        return queryThreadInfos(sql, selectionArgs);
    }

    private List<ThreadInfo> queryThreadInfos(String sql, String[] selectionArgs) {
        if (null == db) {
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, selectionArgs);
            List<ThreadInfo> infos = null;
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst) {//有数据
                int rows = cursor.getCount();
                infos = new ArrayList<>(rows);
                for (int row = 0; row < rows; row++) {
                    ThreadInfo info = getThreadInfoFromCursor(cursor);
                    infos.add(info);
                    cursor.moveToNext();
                }
            }
            return infos;
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public List<ThreadInfo> queryNotCompeletedThreadInfosByUrl(String url) {
        return queryThreadInfosByStatusUrl(url, ThreadInfo.Status.ERROR);
    }

    public List<ThreadInfo> queryDownloadingThreadInfosByUrl(String url) {
        return queryThreadInfosByStatusUrl(url, ThreadInfo.Status.DOWNLOADING);
    }

    private List<ThreadInfo> queryThreadInfosByStatusUrl(String url, int status) {

        String sql = "select * from " + ThreadTable.TABLE + " where " + ThreadTable.COL_STATUS + " =? and " +
                ThreadTable.COL_URL + " =?";
        String[] selectionArgs = {String.valueOf(status), url};
        return queryThreadInfos(sql, selectionArgs);

    }

    public List<ThreadInfo> queryDownloadingThreadInfosByPath(String path) {
        return queryThreadInfosByStatusPath(path, ThreadInfo.Status.DOWNLOADING);
    }

    private List<ThreadInfo> queryThreadInfosByStatusPath(String path, int status) {

        String sql = "select * from " + ThreadTable.TABLE + " where " + ThreadTable.COL_STATUS + " =? and " +
                ThreadTable.COL_FILE_PATH + " =?";
        String[] selectionArgs = {String.valueOf(status), path};
        return queryThreadInfos(sql, selectionArgs);
    }

    public void updateFileInfo(FileInfo info) {
        synchronized (LOCK_FILE) {
            int id = info.id;
            String url = info.url;
            String filePath = info.filePath;
            int downloadLength = info.downloadLength;
            int totalLength = info.totalLength;
            long startDownloadTime = info.startDownloadTime;
            long completeDownloadTime = info.completeDownloadTime;
            int status = info.status;

            if (null != db) {
                try {
                    ContentValues values = new ContentValues(7);
                    values.put(FileTable.COL_URL, url);
                    values.put(FileTable.COL_FILE_PATH, filePath);
                    values.put(FileTable.COL_DOWNLOAD_LENGTH, downloadLength);
                    values.put(FileTable.COL_TOTAL_LENGTH, totalLength);
                    values.put(FileTable.COL_START_DOWNLOAD_TIME, startDownloadTime);
                    values.put(FileTable.COL_COMPLETE_DOWNLOAD_TIME, completeDownloadTime);
                    values.put(FileTable.COL_STATUS, status);

                    int updateRows = db.update(FileTable.TABLE, values, FileTable.COL_ID + "=?", new
                            String[]{String
                            .valueOf(id)});
                    if (0 == updateRows) {
                        Log.e(TAG, "updateFileInfo: 更改失败!!!");
                    } else {
                        Log.d(TAG, "updateFileInfo:更改成功");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void deleteThreadInfo(int id) {
        synchronized (LOCK_THREAD) {
            int delete = delete(ThreadTable.TABLE, ThreadTable.COL_ID, id);
            if (0 == delete) {
                Log.e(TAG, "deleteThreadInfo: 删除id为" + id + "的数据失败!!!");
            } else {
                Log.d(TAG, "deleteThreadInfo:  删除id为" + id + "的数据成功");
            }
        }
    }

    private int delete(String tableName, String colIdName, int id) {
        if (null != db) {
            int delete = db.delete(tableName, colIdName + "=?", new String[]{String.valueOf(id)});
            return delete;
        }
        return 0;
    }

    public void deleteFileInfo(int id) {

        synchronized (LOCK_FILE) {
            int delete = delete(FileTable.TABLE, FileTable.COL_ID, id);
            if (0 == delete) {
                Log.e(TAG, "deleteFileInfo: 删除id为" + id + "的数据失败!!!");
            } else {
                Log.d(TAG, "deleteFileInfo:  删除id为" + id + "的数据成功");
            }
        }
    }

    public void updateThreadInfo(ThreadInfo info) {
        synchronized (LOCK_THREAD) {
            int id = info.id;
            int startPos = info.startPos;
            int downloadLength = info.downloadLength;
            int endPos = info.endPos;
            String filePath = info.filePath;
            String url = info.url;
            int status = info.status;

            if (null != db) {
                try {
                    ContentValues values = new ContentValues(6);
                    values.put(ThreadTable.COL_START_POS, startPos);
                    values.put(ThreadTable.COL_DOWNLOAD_LENGTH, downloadLength);
                    values.put(ThreadTable.COL_END_POS, endPos);
                    values.put(ThreadTable.COL_FILE_PATH, filePath);
                    values.put(ThreadTable.COL_URL, url);
                    values.put(ThreadTable.COL_STATUS, status);

                    int updateRows = db.update(ThreadTable.TABLE, values, ThreadTable.COL_ID + "=?", new
                            String[]{String
                            .valueOf(id)});
                    if (0 == updateRows) {
                        Log.e(TAG, "updateThreadInfo: 更改失败!!!");
                    } else {
                        Log.d(TAG, "updateThreadInfo:更改成功");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public List<ThreadInfo> queryAllThredInfo() {
        String sql = "select * from " + ThreadTable.TABLE;
        return queryThreadInfos(sql, null);
    }

    public List<FileInfo> queryAllFileInfo() {
        String sql = "select * from " + FileTable.TABLE;
        return queryFileInfos(sql, null);
    }

    private List<FileInfo> queryFileInfos(String sql, String[] selectionArgs) {
        if (null == db) {
            return null;
        }

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, selectionArgs);
            List<FileInfo> infos = null;
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst) {//有数据
                int rows = cursor.getCount();
                infos = new ArrayList<>(rows);
                for (int row = 0; row < rows; row++) {
                    FileInfo info = getFileInfoFromCursor(cursor);
                    infos.add(info);
                    cursor.moveToNext();
                }
            }
            return infos;
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private FileInfo getFileInfoFromCursor(Cursor cursor) throws Exception {
        int indexId = cursor.getColumnIndex(FileTable.COL_ID);
        int indexUrl = cursor.getColumnIndex(FileTable.COL_URL);
        int indexFilePath = cursor.getColumnIndex(FileTable.COL_FILE_PATH);
        int indexDownloadLength = cursor.getColumnIndex(FileTable.COL_DOWNLOAD_LENGTH);
        int indexTotalLength = cursor.getColumnIndex(FileTable.COL_TOTAL_LENGTH);
        int indexStartDownloadTime = cursor.getColumnIndex(FileTable.COL_START_DOWNLOAD_TIME);
        int indexCompleteDownloadTime = cursor.getColumnIndex(FileTable.COL_COMPLETE_DOWNLOAD_TIME);
        int indexStatus = cursor.getColumnIndex(FileTable.COL_STATUS);


        int id = cursor.getInt(indexId);
        String url = cursor.getString(indexUrl);
        String filePath = cursor.getString(indexFilePath);
        int downloadLength = cursor.getInt(indexDownloadLength);
        int totalLength = cursor.getInt(indexTotalLength);
        long startDownloadTime = cursor.getLong(indexStartDownloadTime);
        long completeDownloadTime = cursor.getLong(indexCompleteDownloadTime);
        int status = cursor.getInt(indexStatus);

        FileInfo info = new FileInfo();
        info.id = id;
        info.url = url;
        info.filePath = filePath;
        info.downloadLength = downloadLength;
        info.totalLength = totalLength;
        info.startDownloadTime = startDownloadTime;
        info.completeDownloadTime = completeDownloadTime;
        info.status = status;
        return info;
    }

    public void insertFileInfo(FileInfo info) {
        synchronized (LOCK_FILE) {
            String url = info.url;
            String filePath = info.filePath;
            int downloadLength = info.downloadLength;
            int totalLength = info.totalLength;
            long startDownloadTime = info.startDownloadTime;
            long completeDownloadTime = info.completeDownloadTime;
            int status = info.status;

            if (null != db) {
                try {
                    ContentValues values = new ContentValues(7);
                    values.put(FileTable.COL_URL, url);
                    values.put(FileTable.COL_FILE_PATH, filePath);
                    values.put(FileTable.COL_DOWNLOAD_LENGTH, downloadLength);
                    values.put(FileTable.COL_TOTAL_LENGTH, totalLength);
                    values.put(FileTable.COL_START_DOWNLOAD_TIME, startDownloadTime);
                    values.put(FileTable.COL_COMPLETE_DOWNLOAD_TIME, completeDownloadTime);
                    values.put(FileTable.COL_STATUS, status);

                    db.insert(FileTable.TABLE, null, values);

                    FileInfo infoInner = queryFileInfoByUrl(url);
                    info.id = infoInner.id;
                } catch (Exception e) {
                    Log.e(TAG, "insertFileInfo() error===>" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public ThreadInfo insertThreadInfo(ThreadInfo info) {
        synchronized (LOCK_THREAD) {
            int startPos = info.startPos;
            int downloadLength = info.downloadLength;
            int endPos = info.endPos;
            String filePath = info.filePath;
            String url = info.url;
            int fileId = info.fileId;
            int status = info.status;
            int order = info.order;

            if (null != db) {
                try {
                    ContentValues values = new ContentValues(7);
                    values.put(ThreadTable.COL_START_POS, startPos);
                    values.put(ThreadTable.COL_DOWNLOAD_LENGTH, downloadLength);
                    values.put(ThreadTable.COL_END_POS, endPos);
                    values.put(ThreadTable.COL_FILE_PATH, filePath);
                    values.put(ThreadTable.COL_URL, url);
                    values.put(ThreadTable.COL_FILE_ID, fileId);
                    values.put(ThreadTable.COL_STATUS, status);
                    values.put(ThreadTable.COL_ORDER, order);

                    db.insert(ThreadTable.TABLE, null, values);

                    ThreadInfo infoInner = querySingleThreadInfo(fileId, order);
                    info.id = infoInner.id;

                } catch (Exception e) {
                    Log.e(TAG, "insertThreadInfo() error===>" + e.getMessage());
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private ThreadInfo querySingleThreadInfo(int fileId, int order) {

        String sql = "select * from " + ThreadTable.TABLE + " where " + ThreadTable.COL_FILE_ID + "=? and " +
                ThreadTable.COL_ORDER + "=?";
        String[] selectionArgs = {String.valueOf(fileId), String.valueOf(order)};
        return querySingleThreadInfo(sql, selectionArgs);
    }


    public List<ThreadInfo> queryThreadInfoByFileId(int fileId) {

        String sql = "select * from " + ThreadTable.TABLE + " where " + ThreadTable.COL_FILE_ID + "=?";
        String[] selectionArgs = {String.valueOf(fileId)};
        return queryThreadInfos(sql, selectionArgs);
    }

    private ThreadInfo querySingleThreadInfo(String sql, String[] selectionArgs) {
        if (null == db) {
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, selectionArgs);
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst) {//有数据
                ThreadInfo info = getThreadInfoFromCursor(cursor);
                return info;
            }
            return null;
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private ThreadInfo getThreadInfoFromCursor(Cursor cursor) {
        int indexId = cursor.getColumnIndex(ThreadTable.COL_ID);
        int indexStartPos = cursor.getColumnIndex(ThreadTable.COL_START_POS);
        int indexDownloadLength = cursor.getColumnIndex(ThreadTable.COL_DOWNLOAD_LENGTH);
        int indexEndPos = cursor.getColumnIndex(ThreadTable.COL_END_POS);
        int indexFilePath = cursor.getColumnIndex(ThreadTable.COL_FILE_PATH);
        int indexUrl = cursor.getColumnIndex(ThreadTable.COL_URL);
        int indexFileId = cursor.getColumnIndex(ThreadTable.COL_FILE_ID);
        int indexStatus = cursor.getColumnIndex(ThreadTable.COL_STATUS);
        int indexOrder = cursor.getColumnIndex(ThreadTable.COL_ORDER);

        int id = cursor.getInt(indexId);
        int startPos = cursor.getInt(indexStartPos);
        int downloadLength = cursor.getInt(indexDownloadLength);
        int endPos = cursor.getInt(indexEndPos);
        String filePath = cursor.getString(indexFilePath);
        String url = cursor.getString(indexUrl);
        int fileId = cursor.getInt(indexFileId);
        int status = cursor.getInt(indexStatus);
        int order = cursor.getInt(indexOrder);

        ThreadInfo info = new ThreadInfo();
        info.id = id;
        info.startPos = startPos;
        info.downloadLength = downloadLength;
        info.endPos = endPos;
        info.filePath = filePath;
        info.url = url;
        info.fileId = fileId;
        info.status = status;
        info.order = order;
        return info;
    }


    public FileInfo queryFileInfoByUrl(String outterUrl) {
        String sql = "select * from " + FileTable.TABLE + " where " + FileTable.COL_URL + "=?";
        String[] selectionArgs = {outterUrl};
        return querySingleFileInfo(sql, selectionArgs);
    }

    private FileInfo querySingleFileInfo(String sql, String[] selectionArgs) {
        if (null == db) {
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, selectionArgs);
            boolean moveToFirst = cursor.moveToFirst();
            if (moveToFirst) {//有数据
                FileInfo info = getFileInfoFromCursor(cursor);
                return info;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public FileInfo queryFileInfoByFilePath(String outterFilePath) {
        String sql = "select * from " + FileTable.TABLE + " where " + FileTable.COL_FILE_PATH + "=?";
        String[] selectionArgs = {outterFilePath};
        return querySingleFileInfo(sql, selectionArgs);
    }


    static class FileTable {
        public static final String TABLE = "FileTable";
        public static final String COL_ID = TABLE + "_id";
        public static final String COL_URL = TABLE + "_url";
        public static final String COL_FILE_PATH = TABLE + "_filePath";
        public static final String COL_DOWNLOAD_LENGTH = TABLE + "_downloadLength";
        public static final String COL_TOTAL_LENGTH = TABLE + "_totalLength";
        public static final String COL_START_DOWNLOAD_TIME = TABLE + "_startDownloadTime";
        public static final String COL_COMPLETE_DOWNLOAD_TIME = TABLE + "_completeDownloadTime";
        public static final String COL_STATUS = TABLE + "_status";
    }

    static class ThreadTable {

        public static final String TABLE = "ThreadTable";
        public static final String COL_ID = TABLE + "_id";
        public static final String COL_START_POS = TABLE + "_startPos";
        public static final String COL_DOWNLOAD_LENGTH = TABLE + "_downloadLength";
        public static final String COL_END_POS = TABLE + "_endPos";
        public static final String COL_FILE_PATH = TABLE + "_filePath";
        public static final String COL_URL = TABLE + "_url";
        public static final String COL_FILE_ID = TABLE + "_file_id";
        public static final String COL_STATUS = TABLE + "_status";
        public static final String COL_ORDER = TABLE + "_order";
    }

    static class DBOpenHelper extends SQLiteOpenHelper {
        public static final String NAME_DB = "file_multithreading_info.db";

        public DBOpenHelper(Context context) {
            super(context, NAME_DB, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = "CREATE TABLE IF NOT EXISTS " + FileTable.TABLE + " (" + FileTable.COL_ID + " integer " +
                    "primary key " +
                    "autoincrement,  " + FileTable.COL_URL
                    + " varchar(150)," + FileTable.COL_FILE_PATH + " varchar(150)," + FileTable.COL_DOWNLOAD_LENGTH +
                    " INTEGER," +
                    FileTable.COL_TOTAL_LENGTH + " INTEGER, "
                    + FileTable.COL_START_DOWNLOAD_TIME + " INTEGER," + FileTable.COL_COMPLETE_DOWNLOAD_TIME + " " +
                    "INTEGER," +
                    FileTable.COL_STATUS + " INTEGER)";
            Log.i("DBOpenHelper", "onCreate()  FileTable.TABLE sql=" + sql);
            db.execSQL(sql);


            sql = "CREATE TABLE IF NOT EXISTS " + ThreadTable.TABLE + " (" + ThreadTable.COL_ID + " integer " +
                    "primary key " +
                    "autoincrement,  " + ThreadTable.COL_START_POS
                    + " INTEGER," + ThreadTable.COL_DOWNLOAD_LENGTH + " INTEGER," + ThreadTable.COL_END_POS +
                    " INTEGER," +
                    ThreadTable.COL_FILE_PATH + " varchar(150), "
                    + ThreadTable.COL_URL + " varchar(150)," + ThreadTable.COL_FILE_ID + " INTEGER," +
                    ThreadTable.COL_STATUS + " INTEGER," + ThreadTable.COL_ORDER + " INTEGER," +
                    "FOREIGN KEY(" + ThreadTable.COL_FILE_ID + ")REFERENCES " + FileTable.TABLE + "(" + FileTable
                    .COL_ID + ")" +
                    ")";

            Log.i("DBOpenHelper", "onCreate()  ThreadTable.TABLE sql=" + sql);
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int i, int i1) {
            db.execSQL("DROP TABLE IF EXISTS " + ThreadTable.TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + FileTable.TABLE);
            onCreate(db);
        }
    }
}
