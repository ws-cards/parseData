package ext.parse.parsedata;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeleniumController {

    @GetMapping("/scrape")
    public String scrapeTitle() {
        // 設定 Chrome 啟動參數
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new"); // 使用新版 Headless 模式
        options.addArguments("--no-sandbox"); // 必須在 Docker 容器內使用
        options.addArguments("--disable-dev-shm-usage"); // 解決容器內 shared memory 不足的問題
        options.addArguments("--disable-gpu"); 
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = null;
        try {
            // 啟動 WebDriver
            driver = new ChromeDriver(options);
            
            driver.get("https://www.google.com.tw");
            
            // 取得網頁標題
            String title = driver.getTitle();
            
            return "成功抓取網頁標題: " + title;

        } catch (Exception e) {
            return "發生錯誤: " + e.getMessage();
        } finally {
            // ⚠️ 非常重要：一定要呼叫 quit() 關閉瀏覽器與釋放記憶體
            // 否則在 Cloud Run 中會產生 Zombie Process 導致記憶體爆滿
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
