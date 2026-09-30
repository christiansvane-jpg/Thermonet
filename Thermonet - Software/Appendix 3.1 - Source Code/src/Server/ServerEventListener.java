package Server;

import Shared.ControlCommand;
import Shared.HeatPumpData;


 //ServerEventListener lets the gui react to events from ThermonetServer.
 //The server calls these methods whenever something changes.

public interface ServerEventListener
{
  // Called when a client sends a new status update
  void onClientUpdated(HeatPumpData data, ControlCommand lastCommand);

  //Called when a client disconnects
  void onClientRemoved(String clientId);

  //Called after each decision cycle with the current electricity price
  void onDecisionCycleCompleted(double electricityPrice);
}
