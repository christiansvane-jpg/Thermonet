package Client;

import Shared.ControlCommand;
import Shared.HeatPumpData;
import Shared.HeatPumpStatus;
import Shared.SystemConfig;

import java.io.*;
import java.net.Socket;


//  HeatPumpClient simulates one household heat pump.

 //  HP-01 starts at 19.0 C  (small deficit)
 //  HP-02 starts at 17.5 C  (larger deficit)
 //  HP-03 starts at 21.2 C  (already warm, buffer storage demo)
 //  HP-04 starts at 16.0 C  (triggers safety fallback)

public class HeatPumpClient
{
  private final String clientId;
  private final String host;
  private final int    port;

  private double  currentIndoorTemp;
  private double targetIndoorTemp;
  private double  bufferTankTemp;
  private HeatPumpStatus status = HeatPumpStatus.IDLE;

  private static final double HEAT_RATE =  0.3;
  private static final double COOL_RATE  = -0.1;
  private static final double BUFFER_FILL =  0.5;
  private static final double BUFFER_DRAIN= -0.2;
  private static final int CYCLE_MS = 5000;

  public HeatPumpClient(String clientId, String host, int port,
                        double startTemp, double targetTemp, double bufferTemp)
  {
    this.clientId = clientId;
    this.host = host;
    this.port = port;
    this.currentIndoorTemp = startTemp;
    this.targetIndoorTemp = targetTemp;
    this.bufferTankTemp = bufferTemp;
  }

  public void start()
  {
    System.out.println("[" + clientId + "] Connecting to " + host + ":" + port);
    try (Socket socket  = new Socket(host, port);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true))
    {
      System.out.println("[" + clientId + "] Connected!");
      while (running)
      {
        HeatPumpData data = buildStatus();
        out.println(data.serialize());
        System.out.println("[" + clientId + " ->] " + data.serialize());

        String response = in.readLine();
        if (response == null) break;
        System.out.println("[" + clientId + " <-] " + response);

        applyCommand(response.trim());
        simulateTick();
        printState();
        Thread.sleep(CYCLE_MS);
      }
    }
    catch (IOException e)  { System.out.println("[" + clientId + "] Connection error: " + e.getMessage()); }
    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
  }

  private HeatPumpData buildStatus()
  {
    boolean running = status == HeatPumpStatus.HEATING || status == HeatPumpStatus.STORING_BUFFER;
    return new HeatPumpData(clientId, currentIndoorTemp, targetIndoorTemp, bufferTankTemp, running);
  }
    private volatile boolean running = true;

    public void stopClient()
    {
        running = false;
    }

  private void applyCommand(String cmd)
  {
    try
    {
      switch (ControlCommand.valueOf(cmd))
      {
        case START_HEATING:
          status = HeatPumpStatus.HEATING;
          break;
        case STOP_HEATING:
          status = HeatPumpStatus.STOPPED;
          break;
        case WAIT:
          status = HeatPumpStatus.IDLE;
          break;
        case STORE_HEAT_IN_BUFFER:
          status = HeatPumpStatus.STORING_BUFFER;
          break;
      }
    }
    catch (IllegalArgumentException e)
    {
      System.out.println("[" + clientId + "] Unknown cmd: " + cmd);
    }
  }



  private void simulateTick()
  {
    switch (status)
    {
      case HEATING:
        currentIndoorTemp = round(Math.min(targetIndoorTemp + 0.5, currentIndoorTemp + HEAT_RATE));
        break;
      case STORING_BUFFER:
        bufferTankTemp = round(Math.min(SystemConfig.BUFFER_MAX_TEMP, bufferTankTemp + BUFFER_FILL));
        break;
      case IDLE:
      case STOPPED:
        currentIndoorTemp = round(currentIndoorTemp + COOL_RATE);
        bufferTankTemp = round(Math.max(SystemConfig.BUFFER_MIN_TEMP, bufferTankTemp + BUFFER_DRAIN));
        break;
    }
  }

  private void printState()
  {
    System.out.printf("[%s] Indoor: %.1f C (target %.1f C) | Buffer: %.1f C | %s%n",
        clientId, currentIndoorTemp, targetIndoorTemp, bufferTankTemp, status);
  }

  private double round(double v) { return Math.round(v * 10.0) / 10.0; }
}
