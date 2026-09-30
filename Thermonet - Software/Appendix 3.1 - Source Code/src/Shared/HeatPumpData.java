package Shared;

public class HeatPumpData
{
  private final String  clientId;
  private final double  currentIndoorTemp;
  private final double  targetIndoorTemp;
  private final double  bufferTankTemp;
  private final boolean isRunning;

  public HeatPumpData(String clientId, double currentIndoorTemp,
                      double targetIndoorTemp, double bufferTankTemp, boolean isRunning)
  {
    this.clientId = clientId;
    this.currentIndoorTemp = currentIndoorTemp;
    this.targetIndoorTemp  = targetIndoorTemp;
    this.bufferTankTemp  = bufferTankTemp;
    this.isRunning   = isRunning;
  }

  public double getHeatDeficit() { return targetIndoorTemp - currentIndoorTemp; }

  public static HeatPumpData parse(String raw)
  {
    if (raw.trim().isEmpty())
      throw new IllegalArgumentException("Status line is blank");

    String  clientId  = extract(raw, "CLIENT_ID");
    double  currentTemp = Double.parseDouble(extract(raw, "CURRENT_TEMP"));
    double  targetTemp = Double.parseDouble(extract(raw, "TARGET_TEMP"));
    double  bufferTemp = Double.parseDouble(extract(raw, "BUFFER_TEMP"));
    boolean running  = Boolean.parseBoolean(extract(raw, "RUNNING"));
    return new HeatPumpData(clientId, currentTemp, targetTemp, bufferTemp, running);
  }

  private static String extract(String raw, String key)
  {
    for (String part : raw.split(";"))
    {
      String[] kv = part.split("=", 2);
      if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(key))
        return kv[1].trim();
    }
    throw new IllegalArgumentException("Key not found: " + key);
  }

  public String serialize()
  {
    return "CLIENT_ID=" + clientId
        + ";CURRENT_TEMP=" + currentIndoorTemp
        + ";TARGET_TEMP=" + targetIndoorTemp
        + ";BUFFER_TEMP="  + bufferTankTemp
        + ";RUNNING=" + isRunning;
  }

  public String  getClientId() { return clientId; }
  public double  getCurrentIndoorTemp() { return currentIndoorTemp; }
  public double  getTargetIndoorTemp() { return targetIndoorTemp; }
  public double  getBufferTankTemp()  { return bufferTankTemp; }
  public boolean isRunning()  { return isRunning; }

}
