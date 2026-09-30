package Shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.*;

// Unit tests for HeatPumpData: parsing, serialization and heat deficit.
public class HeatPumpDataTest
{
  @Test
  void testParseValidString()
  {
    String raw = "CLIENT_ID=HP1;CURRENT_TEMP=19.2;TARGET_TEMP=21.0;BUFFER_TEMP=42.0;RUNNING=false";
    HeatPumpData data = HeatPumpData.parse(raw);

    assertNotNull(data);
    assertEquals("HP1",  data.getClientId());
    assertEquals(19.2,   data.getCurrentIndoorTemp(), 0.001);
    assertEquals(21.0,   data.getTargetIndoorTemp(),  0.001);
    assertEquals(42.0,   data.getBufferTankTemp(),    0.001);
    assertFalse(data.isRunning());
  }

  @Test
  void testParseRunningTrue()
  {
    String raw = "CLIENT_ID=HP2;CURRENT_TEMP=20.0;TARGET_TEMP=21.0;BUFFER_TEMP=40.0;RUNNING=true";
    HeatPumpData data = HeatPumpData.parse(raw);
    assertNotNull(data);
    assertTrue(data.isRunning());
  }

  @Test
  void testHeatDeficitPositiveWhenCold()
  {
    HeatPumpData data = new HeatPumpData("HP1", 18.0, 21.0, 40.0, false);
    assertEquals(3.0, data.getHeatDeficit(), 0.001);
  }

  @Test
  void testHeatDeficitNegativeWhenWarm()
  {
    // House is above target, so deficit is negative.
    HeatPumpData data = new HeatPumpData("HP1", 22.0, 21.0, 40.0, false);
    assertEquals(-1.0, data.getHeatDeficit(), 0.001);
  }

  @Test
  void testParseBlankStringThrowsException()
  {
    assertThrows(IllegalArgumentException.class, new Executable()
    {
      @Override
      public void execute()
      {
        HeatPumpData.parse("   ");
      }
    });
  }

  @Test
  void testSerializeAndParseRoundTrip()
  {
    HeatPumpData original = new HeatPumpData("HP3", 20.5, 21.0, 45.0, true);
    HeatPumpData parsed = HeatPumpData.parse(original.serialize());

    assertNotNull(parsed);
    assertEquals(original.getClientId(),  parsed.getClientId());
    assertEquals(original.getCurrentIndoorTemp(), parsed.getCurrentIndoorTemp(), 0.001);
    assertEquals(original.getTargetIndoorTemp(), parsed.getTargetIndoorTemp(),  0.001);
    assertEquals(original.getBufferTankTemp(), parsed.getBufferTankTemp(),    0.001);
    assertEquals(original.isRunning(),  parsed.isRunning());
  }
}
