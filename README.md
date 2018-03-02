# FileDownload
这是一个轻量级的文件下载库。

一、使用场景：适用于轻量级的文件下载,当然也可以下载大文件，支持https，支持下载链接重定向。

二、说明：该库实现了多线程 断点下载。内部采用线程池管理，httpurlconnection下载，Sqlite记录文件下载的信息和各个线程的状态。

三、关键核心类：
RecordManager 记录文件下载的各个状态和信息，以及当前该文件的下载线程的信息，可通过该类查询该url的文件下载信息和线程状态信息
FileDownloader 调用入口，如果已经下载完成就会直接回调，如果没有下载完成，就会继续原来的下载进度下载
ThreadDownloadTask 各个线程下载的任务


四、使用方法：
1.FileDownloader downloader=FileDownloader.start(String url,Context context,FileDownloadCallback callback);
说明：所有参数不可为空
文件存放路径:/data/data/<application package>/files/一串md5字符串.apk
如果返回的downloader为null，说明已经有同一个url或者同一个filePath的任务正在下载中


2. FileDownloader downloader=FileDownloader.start(String url, String filePath, Context context,FileDownloadCallback callback);
说明：filePath可为null，其余参数不可为空
filePath为null或者空字符串时，文件存放路径为：/data/data/<application package>/files/一串md5字符串.apk
如果返回的downloader为null，说明已经有同一个url或者同一个filePath的任务正在下载中


3.FileDownloader downloader=FileDownloader.start(FileDownloadConfig config, String url, String filePath, Context context,FileDownloadCallback callback) ;
说明：config为可自定义配置下载，config、filePath可为null，其余参数不可为空
config为null时,采用FileDownloadConfig.getDefaultConfig();
filePath为null或者空字符串时，文件存放路径为：/data/data/<application package>/files/一串md5字符串.apk
如果返回的downloader为null，说明已经有同一个url或者同一个filePath的任务正在下载中
