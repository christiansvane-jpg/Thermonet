package Server;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ElectricityPriceServiceTest
{
  private Map<String, Double> priceMap(String key, double price)
  {
    Map<String, Double> prices = new HashMap<>();
    prices.put(key, price);
    return prices;
  }

  @Test
  void testCurrentPriceUsesJanuaryAtStartup()
  {
    ElectricityPriceService service =
        new ElectricityPriceService(priceMap("Januar", 0.7309));

    assertEquals("Januar", service.getCurrentMonthName());
    assertEquals(0.7309, service.getCurrentPrice(), 0.001);
  }

  @Test
  void testCurrentPriceFallsBackToDefaultWhenMonthIsMissing()
  {
    ElectricityPriceService service =
        new ElectricityPriceService(priceMap("Maj", 0.4792));

    assertEquals(0.61, service.getCurrentPrice(), 0.001);
  }

  @Test
  void testGetPriceForKeyReturnsConfiguredPrice()
  {
    ElectricityPriceService service =
        new ElectricityPriceService(priceMap("Maj", 0.4792));

    assertEquals(0.4792, service.getPriceForKey("Maj"), 0.001);
  }

  @Test
  void testIsCheapUsesCheapThreshold()
  {
    ElectricityPriceService service =
        new ElectricityPriceService(priceMap("Januar", 0.55));

    assertTrue(service.isCheap());
    assertFalse(service.isExpensive());
  }

  @Test
  void testIsExpensiveUsesExpensiveThreshold()
  {
    ElectricityPriceService service =
        new ElectricityPriceService(priceMap("Januar", 0.75));

    assertTrue(service.isExpensive());
    assertFalse(service.isCheap());
  }
}
