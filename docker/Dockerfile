# 单个微服务的运行镜像：构建好的 fat jar 直接塞进 JRE
# 用法：docker build -f docker/Dockerfile --build-arg JAR_FILE=zk-user/target/zk-user.jar -t zkxt/user:latest .
FROM openjdk:11.0-jre-buster

ARG JAR_FILE
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms128m -Xmx320m"

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app
COPY ${JAR_FILE} /app/app.jar

# Nacos 上有带中文注释的共享配置，必须显式指定 UTF-8，否则整份配置会被丢弃
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -jar /app/app.jar"]
