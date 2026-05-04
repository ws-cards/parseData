package ext.parse.parsedata;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeleniumController {

    @GetMapping("/health")
    public String healthCheck() {
        return "SeleniumController is healthy and running!";
    }

    @GetMapping("/view/{id}")
    public Map<String, String> scrapeDeck(@PathVariable String id) {
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
            
            // 目標網址 decklog ja
            driver.get("https://decklog.bushiroad.com/view/"+id);
            
            // 隨機等待 1 到 3 秒 (1000 ~ 3000 毫秒)，避免遭受阻擋
            long sleepTime = 1000 + (long) (Math.random() * 2000);
            Thread.sleep(sleepTime);
            
            // 取得網頁上的牌組資料
            List<WebElement> cards = driver.findElements(By.cssSelector(".card-item"));
            Map<String, String> result = new LinkedHashMap<>();
            result.put("deckCode", id);
            
            for (WebElement card : cards) {
                try {
                    String titleStr = card.findElement(By.cssSelector("img.card-view-item")).getAttribute("title");
                    String count = card.findElement(By.cssSelector("span.num")).getText();
                    
                    // 從 "OSK/S107-003 : “B小町”MEMちょ" 中擷取卡號 "OSK/S107-003"
                    String cardId = titleStr;
                    if (titleStr.contains(" : ")) {
                        cardId = titleStr.split(" : ")[0].trim();
                    }
                    
                    result.put(cardId, count);
                } catch (Exception ex) {
                    // 忽略單張卡片解析失敗的情況
                }
            }
            
            return result;

        } catch (Exception e) {
            Map<String, String> errorResult = new LinkedHashMap<>();
            errorResult.put("error", "發生錯誤: " + e.getMessage());
            return errorResult;
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
