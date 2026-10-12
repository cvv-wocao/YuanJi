# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# 用 BuildKit 缓存挂载保存 Maven 本地仓库，重复构建时不必重新下载依赖
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/Demo35-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]