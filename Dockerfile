# 第一階段：使用 Maven 映像檔來編譯（Builder）
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
# 複製 pom.xml 和原始碼進去
COPY pom.xml .
COPY src ./src
# 在容器內進行打包（跳過測試可加 -DskipTests）
RUN mvn clean package -DskipTests

# 第二階段：實際運行的輕量級環境
FROM eclipse-temurin:17-jdk-jammy

# 替換 APT 來源 (切換至台灣 mirror) 以解決 archive.ubuntu.com 連線逾時問題
RUN sed -i 's/archive.ubuntu.com/tw.archive.ubuntu.com/g' /etc/apt/sources.list && \
    sed -i 's/security.ubuntu.com/tw.archive.ubuntu.com/g' /etc/apt/sources.list

# 安裝 Chrome 等相依套件...
RUN apt-get update && \
    apt-get install -y wget gnupg2 unzip && \
    wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    apt-get install -y ./google-chrome-stable_current_amd64.deb && \
    rm google-chrome-stable_current_amd64.deb && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 直接從第一階段 (builder) 把編譯好的 jar 檔偷拉過來
COPY --from=builder /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
