package ext.parse.parsedata;

import com.sun.net.httpserver.HttpServer;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class HelloWorld {
    public static void main(String[] args) throws IOException {
        String portStr = System.getenv("PORT");
        int port = (portStr != null) ? Integer.parseInt(portStr) : 8080;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", exchange -> {
            String response = "";
            WebDriver driver = null;
            
            try {
                System.out.println("收到請求，開始啟動 Selenium...");
                
                // ⚠️ 針對 Cloud Run 容器環境必須設定的 Chrome 參數
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless=new"); // 無頭模式 (不開啟真實視窗)
                options.addArguments("--no-sandbox"); // 停用沙盒 (容器內需要最高權限)
                options.addArguments("--disable-dev-shm-usage"); // 避免容器共用記憶體不足
                options.addArguments("--disable-gpu"); 
                
                driver = new ChromeDriver(options);
                
                // 執行爬蟲動作，這裡以取得 Example 網站標題為例
                driver.get("https://example.com");
                String title = driver.getTitle();
                
                response = "Selenium 執行成功！抓取到的網頁標題是: " + title;
                System.out.println(response);
                
            } catch (Exception e) {
                response = "執行發生錯誤: " + e.getMessage();
                e.printStackTrace();
            } finally {
                // ⚠️ 務必在結束時關閉瀏覽器，否則會造成 Cloud Run 記憶體洩漏與當機
                if (driver != null) {
                    driver.quit();
                }
            }

            // 回傳 HTTP Response
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            byte[] bytes = response.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Server is listening on port " + port);
    }
}