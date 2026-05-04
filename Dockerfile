# 使用包含 JDK 17 的 Ubuntu 基礎映像檔
FROM eclipse-temurin:17-jdk-jammy

# 安裝 wget, gnupg2 以及 Google Chrome
RUN apt-get update && \
    apt-get install -y wget gnupg2 unzip && \
    wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    apt-get install -y ./google-chrome-stable_current_amd64.deb && \
    rm google-chrome-stable_current_amd64.deb && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 設定工作目錄
WORKDIR /app

# 將編譯好的 jar 檔複製到容器中 
# (請確保你已經先執行過 mvn clean package)
COPY target/*.jar app.jar

# Cloud Run 預設使用 8080 port
ENV PORT=8080
EXPOSE 8080

# 啟動 Spring Boot 應用程式
ENTRYPOINT ["java", "-jar", "app.jar"]