package ext.parse.parsedata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SeleniumControllerTest {

    private final JsonMapper objectMapper = JsonMapper.shared();

    @Test
    void parseDeckMergesMainPartnerAndSubLists() throws Exception {
        String json = """
                {
                  "id": 1,
                  "list": [
                    {"card_number": "S78/W62-001", "num": 4},
                    {"card_number": "S78/W62-002", "num": 2}
                  ],
                  "p_list": [
                    {"card_number": "S78/W62-001", "num": 1}
                  ],
                  "sub_list": [
                    {"card_number": "S78/W62-P01", "num": 1}
                  ]
                }
                """;

        Map<String, Integer> deck = SeleniumController.parseDeck(objectMapper.readTree(json));

        assertEquals(4 + 1, deck.get("S78/W62-001"));
        assertEquals(2, deck.get("S78/W62-002"));
        assertEquals(1, deck.get("S78/W62-P01"));
        assertEquals(3, deck.size());
    }

    @Test
    void scrapeDeckRejectsInvalidId() {
        SeleniumController controller = new SeleniumController();
        Map<String, Object> result = controller.scrapeDeck("../etc");
        assertTrue(result.containsKey("error"));
        assertFalse(result.containsKey("cardList"));
    }
}
