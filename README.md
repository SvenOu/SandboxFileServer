**用法:**  
javascript:  
`plugins.sandboxFileServer.openPrivateFtpServer(function(){}, function(){}, {applicationId: 'com.sv.test', serverPort: 8888}) `
上面的脚本启动ftp服务器,并跳转到ftp服务器开关界面

例子：  
1.获取app的沙盒私有数据信息
`http://10.0.200.24:8888/com.sv.test/appfile/getFileInfo`

2.下载所有的文件
`http://10.0.200.24:8888/com.sv.test/appfile/downloadFile`

3.下载指定文件
`http://10.0.200.24:8888/com.sv.test/appfile/downloadFile?path=/data/data/io.cordova.hellocordova/app_webview`
  





