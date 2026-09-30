package Shared;

public class SystemConfig
{
  public static final int SERVER_PORT  = 8080;
  public static final double SAFETY_FALLBACK_MARGIN = 2.0;
  public static final double BUFFER_MAX_TEMP = 55.0;
  public static final double BUFFER_MIN_TEMP = 35.0;

  // Prices in DKK/kWh. The reader converts from øre/kWh automatically.

  public static final double CHEAP_PRICE_THRESHOLD = 0.55;
  public static final double EXPENSIVE_PRICE_THRESHOLD = 0.75;

  public static final int MAX_RUNNING_HEAT_PUMPS = 2;
  public static final String ELECTRICITY_CSV_PATH  = "HeatPumpSystem/resources/elpriser_2025_danmark.csv";
}
