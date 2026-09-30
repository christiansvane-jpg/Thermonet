package GUI;

import Shared.ControlCommand;
import Shared.HeatPumpData;
import Shared.SystemConfig;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class HeatPumpCard extends VBox
{
  private final String clientId;
  private final DashboardController dashboardController;

  @FXML private Label idLabel;
  @FXML private Label statusIdleLabel;
  @FXML private Label statusHeatingLabel;
  @FXML private Label statusStoringLabel;
  @FXML private Label statusStoppedLabel;
  @FXML private Label indoorTempLabel;
  @FXML private Label targetTempLabel;
  @FXML private Label deficitColdLabel;
  @FXML private Label deficitMildLabel;
  @FXML private Label deficitOkLabel;
  @FXML private Label bufferTempLabel;
  @FXML private Label commandEmptyLabel;
  @FXML private Label commandStartLabel;
  @FXML private Label commandStopLabel;
  @FXML private Label commandWaitLabel;
  @FXML private Label commandStoreLabel;
  @FXML private Region bufferFillBar;

  public HeatPumpCard(String clientId, DashboardController dashboardController)
  {
    this.clientId = clientId;
    this.dashboardController = dashboardController;
    loadView();
  }

  private void loadView()
  {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("HeatPumpCard.fxml"));
    loader.setRoot(this);
    loader.setController(this);

    try
    {
      loader.load();
    }
    catch (IOException e)
    {
      throw new IllegalStateException("Could not load HeatPumpCard.fxml", e);
    }

    idLabel.setText(clientId);
    showStatus("IDLE");
    showCommand(null);
  }

  @FXML
  private void disconnectClicked()
  {
    dashboardController.disconnectClient(clientId);
  }

  public void update(HeatPumpData data, ControlCommand command)
  {
    indoorTempLabel.setText(String.format("%.1f C", data.getCurrentIndoorTemp()));
    targetTempLabel.setText(String.format("%.1f C", data.getTargetIndoorTemp()));

    double deficit = data.getHeatDeficit();
    String deficitText = String.format("Deficit: %.1f C", deficit);
    deficitColdLabel.setText(deficitText);
    deficitMildLabel.setText(deficitText);
    deficitOkLabel.setText(deficitText);

    if (deficit >= SystemConfig.SAFETY_FALLBACK_MARGIN)
      showDeficitLabel(deficitColdLabel);
    else if (deficit > 0)
      showDeficitLabel(deficitMildLabel);
    else
      showDeficitLabel(deficitOkLabel);

    bufferTempLabel.setText(String.format("%.1f C", data.getBufferTankTemp()));
    double fillRatio = (data.getBufferTankTemp() - SystemConfig.BUFFER_MIN_TEMP)
        / (SystemConfig.BUFFER_MAX_TEMP - SystemConfig.BUFFER_MIN_TEMP);
    if (fillRatio < 0)
      fillRatio = 0;
    else if (fillRatio > 1)
      fillRatio = 1;

    bufferFillBar.setPrefWidth(fillRatio * 200);

    String statusText;
    if (data.isRunning())
    {
      if (command == ControlCommand.STORE_HEAT_IN_BUFFER)
        statusText = "STORING";
      else
        statusText = "HEATING";
    }
    else
    {
      if (command == ControlCommand.WAIT)
        statusText = "IDLE";
      else
        statusText = "STOPPED";
    }

    showStatus(statusText);
    showCommand(command);
  }

  private void showStatus(String status)
  {
    hideStatusLabels();

    if (status.equals("HEATING"))
      showLabel(statusHeatingLabel);
    else if (status.equals("STORING"))
      showLabel(statusStoringLabel);
    else if (status.equals("STOPPED"))
      showLabel(statusStoppedLabel);
    else
      showLabel(statusIdleLabel);
  }

  private void showCommand(ControlCommand command)
  {
    hideCommandLabels();

    if (command == null)
      showLabel(commandEmptyLabel);
    else if (command == ControlCommand.START_HEATING)
      showLabel(commandStartLabel);
    else if (command == ControlCommand.STOP_HEATING)
      showLabel(commandStopLabel);
    else if (command == ControlCommand.STORE_HEAT_IN_BUFFER)
      showLabel(commandStoreLabel);
    else
      showLabel(commandWaitLabel);
  }

  private void showDeficitLabel(Label label)
  {
    hideDeficitLabels();
    showLabel(label);
  }

  private void hideStatusLabels()
  {
    hideLabel(statusIdleLabel);
    hideLabel(statusHeatingLabel);
    hideLabel(statusStoringLabel);
    hideLabel(statusStoppedLabel);
  }

  private void hideDeficitLabels()
  {
    hideLabel(deficitColdLabel);
    hideLabel(deficitMildLabel);
    hideLabel(deficitOkLabel);
  }

  private void hideCommandLabels()
  {
    hideLabel(commandEmptyLabel);
    hideLabel(commandStartLabel);
    hideLabel(commandStopLabel);
    hideLabel(commandWaitLabel);
    hideLabel(commandStoreLabel);
  }

  private void showLabel(Label label)
  {
    label.setVisible(true);
    label.setManaged(true);
  }

  private void hideLabel(Label label)
  {
    label.setVisible(false);
    label.setManaged(false);
  }
}
