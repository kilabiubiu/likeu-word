# ===== 阶段 1：Maven 构建 =====
FROM maven:3.9-eclipse-temurin-8 AS build

WORKDIR /src
# 利用 Docker 缓存：先下载依赖
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再复制源码编译
COPY src src
RUN mvn -B -q -DskipTests clean package

# ===== 阶段 2：运行时 =====
FROM eclipse-temurin:8-jre-alpine

# 时区（Asia/Shanghai）
RUN apk add --no-cache tzdata \
 && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
 && echo "Asia/Shanghai" > /etc/timezone \
 && apk del tzdata

WORKDIR /app

# 从构建阶段拷贝 fat jar
COPY --from=build /src/target/likeu-word.jar app.jar

# 健康检查：Spring Actuator /actuator/health
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1

EXPOSE 8080

# 容器固定走 prod profile；所有敏感值由云托管环境变量注入
# Spring 会依次读取：application.yml（基础）→ application-prod.yml（prod 覆盖）→ 环境变量（最高优先级）
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
