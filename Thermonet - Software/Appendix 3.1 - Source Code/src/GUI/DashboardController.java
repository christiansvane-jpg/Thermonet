package GUI;

import Client.HeatPumpClient;
import Server.ElectricityPriceReader;
import Server.ElectricityPriceService;
import Server.ServerEventListener;
import Server.ThermonetServer;
import Shared.ControlCommand;
import Shared.HeatPumpData;
import Shared.SystemConfig;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class DashboardController implements ServerEventListener
{
  private boolean serverRunning = false;

  private final Map<String, HeatPumpCard> pumpCards = new HashMap<>();
  private final Map<String, HeatPumpClient> clients  = new HashMap<>();

  @FXML private Label priceLabel;
  @FXML private Label cheapPriceStatusLabel;
  @FXML private Label  mediumPriceStatusLabel;
  @FXML private Label expensivePriceStatusLabel;
  @FXML private Label connectedCountLabel;
  @FXML private Label serverStatusOfflineLabel;
  @FXML private Label serverStatusOnlineLabel;
  @FXML private FlowPane  cardsPane;
  @FXML private TextArea  logArea;
  @FXML private Button startServerBtn;
  @FXML private Label priceHeading;

  private ElectricityPriceService priceService;

  @FXML
  private void startServer()
  {
    if (serverRunning) return;

    log("Loading electricity prices from CSV...");
    ElectricityPriceReader reader = new ElectricityPriceReader();
    Map<String, Double> prices = reader.readPrices(SystemConfig.ELECTRICITY_CSV_PATH);
    priceService = new ElectricityPriceService(prices);

    ThermonetServer server = new ThermonetServer(priceService);
    server.addListener(this);
    server.start(SystemConfig.SERVER_PORT);

    serverRunning = true;
    startServerBtn.setDisable(true);
    startServerBtn.setText("Server Running");
    serverStatusOnlineLabel.setText("Online (port " + SystemConfig.SERVER_PORT + ")");
    showServerOnlineStatus();

    double price = priceService.getCurrentPrice();
    updatePriceDisplay(price);

    log("Server started on port " + SystemConfig.SERVER_PORT);
    log(String.format("Electricity price this month: %.4f DKK/kWh", price));
  }

  @FXML private void spawnHp01() { spawnClient("HP-01"); }
  @FXML private void spawnHp02() { spawnClient("HP-02"); }
  @FXML private void spawnHp03() { spawnClient("HP-03"); }
  @FXML private void spawnHp04() { spawnClient("HP-04"); }

  private void spawnClient(String clientId)
  {
    if (!serverRunning)
    {
      log("Start the server first before spawning clients.");
      return;
    }

    if (clients.containsKey(clientId))
    {
      log(clientId + " is already running.");
      return;
    }

    double startTemp, targetTemp, bufferTemp;
    switch (clientId)
    {
      case "HP-01": startTemp = 19.0; targetTemp = 21.0; bufferTemp = 38.0; break;
      case "HP-02": startTemp = 17.5; targetTemp = 21.0; bufferTemp = 42.0; break;
      case "HP-03": startTemp = 21.2; targetTemp = 21.0; bufferTemp = 36.0; break;
      case "HP-04": startTemp = 16.0; targetTemp = 21.0; bufferTemp = 35.0; break;
      default: startTemp = 20.0; targetTemp = 21.0; bufferTemp = 40.0; break;
    }

    HeatPumpClient client = new HeatPumpClient(
        clientId, "localhost", SystemConfig.SERVER_PORT,
        startTemp, targetTemp, bufferTemp
    );

    clients.put(clientId, client);

    Thread clientThread = new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        client.start();
      }
    }, clientId + "-Thread");
    clientThread.setDaemon(true);
    clientThread.start();

    log("Spawned client: " + clientId
        + " (indoor=" + startTemp + " C, target=" + targetTemp + " C, buffer=" + bufferTemp + " C)");
  }

  void disconnectClient(String clientId)
  {
    HeatPumpClient client = clients.remove(clientId);
    client.stopClient();
    log("Client manually disconnected: " + clientId);
  }

  @Override
  public void onClientUpdated(HeatPumpData data, ControlCommand lastCommand)
  {
    Platform.runLater(new Runnable()
    {
      @Override
      public void run()
      {
        String id = data.getClientId();
        if (!pumpCards.containsKey(id))
        {
          HeatPumpCard card = new HeatPumpCard(id, DashboardController.this);
          pumpCards.put(id, card);
          cardsPane.getChildren().add(card);
          log("Client connected: " + id);
          updateConnectedCount();
        }
        pumpCards.get(id).update(data, lastCommand);
      }
    });
  }

  @Override
  public void onClientRemoved(String clientId)
  {
    Platform.runLater(new Runnable()
    {
      @Override
      public void run()
      {
        clients.remove(clientId);
        HeatPumpCard card = pumpCards.remove(clientId);
        cardsPane.getChildren().remove(card);
        log("Client disconnected: " + clientId);
        updateConnectedCount();
      }
    });
  }

  @Override
  public void onDecisionCycleCompleted(double price)
  {
    Platform.runLater(new Runnable()
    {
      @Override
      public void run()
      {
        updatePriceDisplay(price);
        log(String.format("Decision cycle. Price: %.4f DKK/kWh", price));
      }
    });
  }

  private void updatePriceDisplay(double price)
  {
    priceHeading.setText("Electricity price (" + priceService.getCurrentMonthName() + "):");
    priceLabel.setText(String.format("%.4f DKK/kWh", price));

    if (price <= SystemConfig.CHEAP_PRICE_THRESHOLD)
    {
      showPriceStatus(cheapPriceStatusLabel);
    }
    else if (price >= SystemConfig.EXPENSIVE_PRICE_THRESHOLD)
    {
      showPriceStatus(expensivePriceStatusLabel);
    }
    else
    {
      showPriceStatus(mediumPriceStatusLabel);
    }
  }

  private void showServerOnlineStatus()
  {
    serverStatusOfflineLabel.setVisible(false);
    serverStatusOfflineLabel.setManaged(false);
    serverStatusOnlineLabel.setVisible(true);
    serverStatusOnlineLabel.setManaged(true);
  }

  private void showPriceStatus(Label statusLabel)
  {
    cheapPriceStatusLabel.setVisible(false);
    cheapPriceStatusLabel.setManaged(false);
    mediumPriceStatusLabel.setVisible(false);
    mediumPriceStatusLabel.setManaged(false);
    expensivePriceStatusLabel.setVisible(false);
    expensivePriceStatusLabel.setManaged(false);

    statusLabel.setVisible(true);
    statusLabel.setManaged(true);
  }

  void log(String message)
  {
    String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    Platform.runLater(new Runnable()
    {
      @Override
      public void run()
      {
        logArea.appendText("[" + time + "] " + message + "\n");
      }
    });
  }

  private void updateConnectedCount()
  {
    int n = pumpCards.size();
    connectedCountLabel.setText(n == 1 ? "1 pump connected" : n + " pumps connected");
  }
}
