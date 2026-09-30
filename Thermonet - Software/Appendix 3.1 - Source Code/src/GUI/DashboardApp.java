package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardApp extends Application
{
  @Override
  public void start(Stage stage) throws IOException
  {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("DashboardView.fxml"));
    Parent root = loader.load();
    DashboardController controller = loader.getController();

    stage.setTitle("Thermonet Central Server - Dashboard");
    stage.setMinWidth(960);
    stage.setMinHeight(650);
    stage.setScene(new Scene(root, 1150, 720));
    stage.show();

    controller.log("Dashboard ready. Press 'Start Server' to begin.");
  }
}
