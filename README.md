#开发调试
开发调测时需在resources/application.properties中配置web.docBase属性，指定应用WebRoot目录，如：

web.docBase=C:/workspace/bbossgroups/bboss-demos/bbootdemo/WebRoot

实际发布包中可以注释或者去掉web.docBase属性

可以通过Main类启动和调试应用：org.frameworkset.test.Main
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

智能问答访问地址：http://127.0.0.1/demoproject/chatBackuppressSession.html

mvc案例访问地址：http://localhost/contextpath/examples/index.page

