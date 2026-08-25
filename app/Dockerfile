# JDK8 轻量镜像
FROM openjdk:8-jre-slim

# 设置时区东八区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 替换成你自己target下jar包名字
COPY target/app-1.0.0.jar app.jar

EXPOSE ${PORT}

# 限制堆内存，适配免费版512M内存，防止OOM崩溃
ENTRYPOINT ["java","-Xmx384m","-Xms128m","-jar","app.jar","--server.port=${PORT}"]
