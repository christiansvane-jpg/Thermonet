package Server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ElectricityPriceReaderTest
{
  private final ElectricityPriceReader reader = new ElectricityPriceReader();

  @Test
  void testReadsPricesCorrectly(@TempDir Path tempDir) throws IOException
  {
    Path csv = tempDir.resolve("prices.csv");
    Files.writeString(csv,
        "Month;DK1 (West) øre/kWh;\n" +
        "Januar;73.09;\n"             +
        "Februar;84.18;\n"            +
        "Maj;47.92;\n");

    Map<String, Double> prices = reader.readPrices(csv.toString());

    assertEquals(3, prices.size());
    assertEquals(0.7309, prices.get("Januar"), 0.001);
    assertEquals(0.8418, prices.get("Februar"), 0.001);
    assertEquals(0.4792, prices.get("Maj"), 0.001);
  }

  @Test
  void testOereIsConvertedToDkk(@TempDir Path tempDir) throws IOException
  {
    Path csv = tempDir.resolve("convert.csv");
    Files.writeString(csv, "Month;price;\nJuni;43.85;\n");

    Map<String, Double> prices = reader.readPrices(csv.toString());

    assertEquals(0.4385, prices.get("Juni"), 0.001);
  }

  @Test
  void testAarsgennemsnitsRowIsSkipped(@TempDir Path tempDir) throws IOException
  {
    Path csv = tempDir.resolve("avg.csv");
    Files.writeString(csv,
        "Month;price;\n"        +
        "Januar;73.09;\n"       +
        "Aarsgennemsnit;60.67;\n");

    Map<String, Double> prices = reader.readPrices(csv.toString());

    assertEquals(1, prices.size());
    assertTrue(prices.containsKey("Januar"));
    assertFalse(prices.containsKey("Årsgennemsnit"));
  }

  @Test
  void testHeaderRowIsNotParsedAsData(@TempDir Path tempDir) throws IOException
  {
    Path csv = tempDir.resolve("header.csv");
    Files.writeString(csv, "Month;price;\nJanuar;73.09;\n");

    Map<String, Double> prices = reader.readPrices(csv.toString());

    assertEquals(1, prices.size());
    assertFalse(prices.containsKey("Month"));
  }

  @Test
  void testMissingFileReturnsEmptyMap()
  {
    assertTrue(reader.readPrices("does_not_exist.csv").isEmpty());
  }
}
