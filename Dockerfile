# 第一階段：使用 Maven 映像檔來編譯（Builder）
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
# 複製 pom.xml 和原始碼進去
COPY pom.xml .
COPY src ./src
# 在容器內進行打包（跳過測試可加 -DskipTests）
RUN mvn clean package -DskipTests

# 第二階段：實際運行的輕量級環境（已改打 Deck Log API，不再需要 Chrome）
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 直接從第一階段 (builder) 把編譯好的 jar 檔偷拉過來
COPY --from=builder /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
