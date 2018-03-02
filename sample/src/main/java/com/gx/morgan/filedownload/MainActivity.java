package com.gx.morgan.filedownload;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.gx.morgan.downloadlib.FileDownloader;
import com.gx.morgan.downloadlib.callback.FileDownloadCallback;
import com.gx.morgan.downloadlib.entity.FileInfo;

public class MainActivity extends AppCompatActivity {


    private static final String URL="http://mapdownload.autonavi.com/mobileapk/apk/Amap_V8.3.0.2128_android_C3060_(Build1518084792).apk";
    private static final String TAG = "MainActivity";

    private TextView tvContent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvContent=findViewById(R.id.tv_content);
    }


    public void onStartClick(View view){
        /**
         * 详见downloadlib module下的ReadMe.md
         */
        FileDownloader.start(URL, this, new FileDownloadCallback() {
            @Override
            public void onDownloadPreExecute(FileInfo info) {

            }

            @Override
            public void onDownloadSuccess(FileInfo info, int fileTotalLength) {
                Log.e(TAG, "onDownloadSuccess: 下载成功，path="+info.filePath );
                tvContent.setText("下载完成\n 路径为"+info.filePath );
            }

            @Override
            public void onDownloadFailure(FileInfo info, int errorNo, String msg) {
                Log.e(TAG, "onDownloadFailure: 下载失败 errorNo="+errorNo+",msg="+msg );
            }

            @Override
            public void onDownloading(FileInfo info, int current, int count) {
                int progress= (int) (current*100.0/count);
                tvContent.setText(progress+"%");
            }
        });
    }
}
