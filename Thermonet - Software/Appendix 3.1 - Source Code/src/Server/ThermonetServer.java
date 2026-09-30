package Server;

import Shared.ControlCommand;
import Shared.HeatPumpData;
import Shared.SystemConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ThermonetServer
{
  private final ElectricityPriceService priceService;
  private final ControlDecisionService decisionService;

  private final Map<String, HeatPumpData> clientDataMap = new ConcurrentHashMap<>();
  private final Map<String, ControlCommand> pendingCommands = new ConcurrentHashMap<>();

  // GUI listeners, notified whenever something changes
  private final List<ServerEventListener>  listeners = new ArrayList<>();

  private boolean running = false;

  public ThermonetServer(ElectricityPriceService priceService)
  {
    this.priceService  = priceService;
    this.decisionService = new ControlDecisionService();
  }

  public void addListener(ServerEventListener listener) { listeners.add(listener); }

  public void start(int port)
  {
    running = true;
    startDecisionLoop();

    // Run accept loop so the GUI stays responsive
    Thread acceptThread = new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        try (ServerSocket serverSocket = new ServerSocket(port))
        {
          System.out.println("[Server] Started on port " + port);
          while (running)
          {
            Socket clientSocket = serverSocket.accept();
            System.out.println("[Server] New client: " + clientSocket.getInetAddress());
            Thread clientThread = new Thread(new ClientHandler(clientSocket, ThermonetServer.this));
            clientThread.setDaemon(true);
            clientThread.start();
          }
        }
        catch (IOException e)
        {
          if (running) System.out.println("[Server] Error: " + e.getMessage());
        }
      }
    }, "ServerAcceptThread");
    acceptThread.setDaemon(true);
    acceptThread.start();
  }

  private void startDecisionLoop()
  {
    Thread t = new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        while (running)
        {
          try
          {
            Thread.sleep(10_000);
            runDecisionCycle();
          }
          catch (InterruptedException e)
          {
            Thread.currentThread().interrupt();
          }
        }
      }
    }, "DecisionLoop");
    t.setDaemon(true);
    t.start();
  }

  private void runDecisionCycle()
  {
    if (clientDataMap.isEmpty()) return;

    double price = priceService.getCurrentPrice();
    List<HeatPumpData> snapshot = new ArrayList<>(clientDataMap.values());
    Map<String, ControlCommand> decisions = decisionService.decideAll(snapshot, price);
    pendingCommands.putAll(decisions);

    // Notify GUI
    for (ServerEventListener l : new ArrayList<>(listeners))
      l.onDecisionCycleCompleted(price);

      System.out.println("[Cycle] Month: " + priceService.getCurrentMonthName()
              + " | Price: " + price
              + " | Decisions: " + decisions);
  }

  public void updateClientData(String clientId, HeatPumpData data)
  {
    boolean isNew = !clientDataMap.containsKey(clientId);
    clientDataMap.put(clientId, data);

    if (isNew)
    {
      System.out.println("[Server] Registered: " + clientId);
      Thread decisionThread = new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          runDecisionCycle();
        }
      });
      decisionThread.setDaemon(true);
      decisionThread.start();
    }

    ControlCommand cmd = getCommandFor(clientId);

    // Notify GUI
    for (ServerEventListener l : new ArrayList<>(listeners))
      l.onClientUpdated(data, cmd);
  }

  public ControlCommand getCommandFor(String clientId)
  {
    if (!pendingCommands.containsKey(clientId))
    {
      HeatPumpData data = clientDataMap.get(clientId);
      ControlCommand cmd = decisionService.decide(data, priceService.getCurrentPrice(), 0);
      pendingCommands.put(clientId, cmd);
    }
    return pendingCommands.get(clientId);
  }

  public void removeClient(String clientId)
  {
    clientDataMap.remove(clientId);
    pendingCommands.remove(clientId);
    System.out.println("[Server] Removed: " + clientId);

    for (ServerEventListener l : new ArrayList<>(listeners))
      l.onClientRemoved(clientId);
  }
}
