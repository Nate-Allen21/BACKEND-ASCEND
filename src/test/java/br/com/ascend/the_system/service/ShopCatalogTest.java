package br.com.ascend.the_system.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ShopCatalogTest {
    @Test void shouldUseServerPrices() {
        assertEquals(120, ShopCatalog.get("pausa-estrategica").cost());
        assertEquals(777, ShopCatalog.get("reliquia-ascendente").cost());
        assertNull(ShopCatalog.get("produto-inventado"));
    }

    @Test void shouldRotateNineDailyOffersDeterministically() {
        var day=LocalDate.of(2026,10,9);
        var offers=ShopCatalog.offers(day);
        assertEquals(9, offers.size());
        assertEquals("reliquia-ascendente", offers.get(0).id());
        assertEquals("almoco-livre", offers.get(1).id());
        assertEquals(9, offers.stream().map(ShopCatalog.Product::id).distinct().count());
        assertEquals(offers, ShopCatalog.offers(day));
    }
}
