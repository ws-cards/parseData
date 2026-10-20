package ext.parse.parsedata;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deck Log 牌組查詢：改打官方 JSON API，避免每次冷啟動 Chrome（原先約 25 秒）。
 */
@CrossOrigin(origins = "https://ws-cards.cloud")
@RestController
public class SeleniumController {

    private static final String DECKLOG_API = "https://decklog.bushiroad.com/system/app/api/view/";
    private static final Pattern DECK_ID_PATTERN = Pattern.compile("^[A-Za-z0-9]{1,10}$");
    private static final String[] CARD_LIST_FIELDS = {"list", "p_list", "sub_list"};

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final JsonMapper objectMapper = JsonMapper.shared();

    @GetMapping("/health")
    public String healthCheck() {
        return "SeleniumController is healthy and running![update date: 2026-10-20 version: 1.1.0]";
    }

    @GetMapping("/view/{id}")
    public Map<String, Object> scrapeDeck(@PathVariable String id) {
        if (id == null || !DECK_ID_PATTERN.matcher(id).matches()) {
            return errorResult("無效的 Deck ID");
        }

        try {
            long started = System.currentTimeMillis();
            JsonNode deckJson = fetchDeckJson(id);
            if (deckJson == null || !deckJson.hasNonNull("id")) {
                return errorResult("找不到牌組或回傳資料無效");
            }

            Map<String, Integer> deck = parseDeck(deckJson);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("deckCode", id);

            Map<String, String> cardList = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> e : deck.entrySet()) {
                cardList.put(e.getKey(), String.valueOf(e.getValue()));
            }
            result.put("cardList", cardList);

            System.out.println("search cardlist via API: " + DECKLOG_API + id
                    + " (" + (System.currentTimeMillis() - started) + " ms, cards=" + cardList.size() + ")");
            return result;
        } catch (Exception e) {
            return errorResult("發生錯誤: " + e.getMessage());
        }
    }

    private JsonNode fetchDeckJson(String id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DECKLOG_API + id))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Content-Type", "application/json")
                .header("Referer", "https://decklog.bushiroad.com/view/" + id)
                .header("Origin", "https://decklog.bushiroad.com")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Deck Log API HTTP " + response.statusCode());
        }

        String body = response.body();
        if (body == null || body.isBlank() || "[]".equals(body.trim())) {
            return null;
        }
        return objectMapper.readTree(body);
    }

    private static Map<String, Object> errorResult(String message) {
        Map<String, Object> errorResult = new LinkedHashMap<>();
        errorResult.put("error", message);
        return errorResult;
    }

    static Map<String, Integer> parseDeck(JsonNode deckJson) {
        Map<String, Integer> deck = new LinkedHashMap<>();
        // 與舊版 Selenium 掃描頁面全部 .card-controller-inner 行為對齊：
        // 主牌庫 + ride/partner + 副牌庫
        for (String field : CARD_LIST_FIELDS) {
            JsonNode list = deckJson.get(field);
            if (list == null || !list.isArray()) {
                continue;
            }
            for (JsonNode item : list) {
                String cardNo = textOrEmpty(item.get("card_number")).trim();
                if (cardNo.isEmpty()) {
                    continue;
                }
                int qty = item.path("num").asInt(0);
                if (qty <= 0) {
                    qty = item.path("_num").asInt(0);
                }
                if (qty <= 0) {
                    continue;
                }
                deck.put(cardNo, deck.getOrDefault(cardNo, 0) + qty);
            }
        }
        return deck;
    }

    private static String textOrEmpty(JsonNode node) {
        return node == null || node.isNull() ? "" : node.asText("");
    }

    public static void main(String[] args) {
        SeleniumController controller = new SeleniumController();
        String testDeckId = args.length > 0 ? args[0] : "25CWH";
        System.out.println("開始測試爬取 Deck ID: " + testDeckId);

        long started = System.currentTimeMillis();
        Map<String, Object> result = controller.scrapeDeck(testDeckId);
        long elapsed = System.currentTimeMillis() - started;

        System.out.println("====== 爬取結果 (" + elapsed + " ms) ======");
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}
