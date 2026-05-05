package ext.parse.parsedata;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeleniumController {

    @GetMapping("/health")
    public String healthCheck() {
        return "SeleniumController is healthy and running![update date: 2026-5-5]";
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

            Document doc = Jsoup.parse(driver.getPageSource());
            Map<String, Integer> deck = parseDeck(doc);
            Map<String, String> result = new LinkedHashMap<>();
            result.put("deckCode", id);
            for (Map.Entry<String, Integer> e : deck.entrySet()) {
                result.put(e.getKey(), String.valueOf(e.getValue()));
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

    private static int parseIntSafe(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Map<String, Integer> parseDeck(Document doc) {
        Map<String, Integer> deck = new LinkedHashMap<>();

        Elements items = doc.select(".card-controller-inner");
        for (Element item : items) {
            Elements spans = item.select("span");
            if (spans.size() < 2) {
                continue;
            }

            String title = spans.get(0).attr("title");
            if (title == null) {
                title = "";
            }
            title = title.trim();

            String[] parts = title.split("\\s*:\\s*", 2);
            String cardNo = (parts.length > 0 ? parts[0] : "").trim();
            if (cardNo.isEmpty()) {
                continue;
            }

            String qtyText = spans.get(1).text() == null ? "" : spans.get(1).text().trim();
            int qty = parseIntSafe(qtyText);

            if (qty <= 0) {
                continue;
            }

            deck.put(cardNo, deck.getOrDefault(cardNo, 0) + qty);
        }

        return deck;
    }    
}
