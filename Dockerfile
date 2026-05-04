# 使用 Debian Bookworm 版本的 JDK，相容性較佳
FROM eclipse-temurin:17-jdk-bookworm

# 安裝 Chromium 瀏覽器、ChromeDriver 與 wget
RUN apt-get update && \
    apt-get install -y chromium chromium-driver wget && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 下載 Selenium Standalone Server JAR 檔 (包含 Selenium 執行所需的所有類別)
# 這裡使用 4.18.1 版本作為範例
RUN wget https://github.com/SeleniumHQ/selenium/releases/download/selenium-4.18.1/selenium-server-4.18.1.jar -O selenium.jar

# 複製本地的 Java 檔
COPY HelloWorld.java .

# 編譯 Java 檔，並透過 -cp 將 Selenium JAR 加入 Classpath
RUN javac -cp selenium.jar HelloWorld.java

# 啟動 HTTP Server，執行時同樣需要載入 Selenium JAR
# 注意：在 Linux 環境中 Classpath 的分隔符號是冒號 ":"，代表載入當前目錄與 selenium.jar
CMD ["java", "-cp", ".:selenium.jar", "HelloWorld"]
