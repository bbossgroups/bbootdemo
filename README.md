#开发调试
设置jvm启动参数
-DdocBase=E:/workspace/bbossgroups/bbootdemo/WebRoot 
-DcontextPath=demoproject 
-Dport=8080


# 打运行包：

gradle releaseVersion

# 启动应用
## windows
startup.bat/restart.bat

## linux/unix/macos

chmod +x startup.sh restart.sh
startup.sh/restart.sh

# 停止应用
## windows
stop.bat

## linux/unix/macos
chmod +x stop.sh

# 访问地址：

http://localhost/contextpath/examples/index.page