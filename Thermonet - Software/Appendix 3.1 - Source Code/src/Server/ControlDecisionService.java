package Server;

import Shared.ControlCommand;
import Shared.HeatPumpData;
import Shared.SystemConfig;

import java.util.*;

public class ControlDecisionService
{
  public ControlCommand decide(HeatPumpData data, double price, int currentlyRunning)
  {
    double deficit = data.getHeatDeficit();

    // Rule 1: Safety fallback, always heat if too cold
    if (deficit >= SystemConfig.SAFETY_FALLBACK_MARGIN)
      return ControlCommand.START_HEATING;

    boolean cheap = price <= SystemConfig.CHEAP_PRICE_THRESHOLD;
    boolean expensive = price >= SystemConfig.EXPENSIVE_PRICE_THRESHOLD;
    boolean needsHeat  = deficit > 0;
    boolean bufferNotFull= data.getBufferTankTemp() < SystemConfig.BUFFER_MAX_TEMP;
    boolean hasCapacity = currentlyRunning < SystemConfig.MAX_RUNNING_HEAT_PUMPS;

    // Rule 2: Cheap + needs heat + capacity
    if (cheap && needsHeat && hasCapacity)    return ControlCommand.START_HEATING;

    // Rule 3: Cheap + warm + buffer not full + capacity
    if (cheap && !needsHeat && bufferNotFull && hasCapacity) return ControlCommand.STORE_HEAT_IN_BUFFER;

    // Rule 4: Expensive and warm: stop or wait
    if (expensive && !needsHeat)
    {
      if (data.isRunning())
        return ControlCommand.STOP_HEATING;
      else
        return ControlCommand.WAIT;
    }

    // Rule 5: Expensive + small deficit, wait
    if (expensive) return ControlCommand.WAIT;

    return ControlCommand.WAIT;
  }

  public Map<String, ControlCommand> decideAll(List<HeatPumpData> clients, double price)
  {
    Map<String, ControlCommand> commands = new LinkedHashMap<>();
    List<HeatPumpData> safety = new ArrayList<>();
    List<HeatPumpData> normal = new ArrayList<>();

    for (HeatPumpData c : clients)
    {
      if (c.getHeatDeficit() >= SystemConfig.SAFETY_FALLBACK_MARGIN) safety.add(c);
      else normal.add(c);
    }

    int running = 0;
    for (HeatPumpData c : safety)
    {
      commands.put(c.getClientId(), ControlCommand.START_HEATING);
      running++;
    }

    sortByLargestHeatDeficit(normal);
    for (HeatPumpData c : normal)
    {
      ControlCommand cmd = decide(c, price, running);
      commands.put(c.getClientId(), cmd);
      if (cmd == ControlCommand.START_HEATING || cmd == ControlCommand.STORE_HEAT_IN_BUFFER)
        running++;
    }
    return commands;
  }

  private void sortByLargestHeatDeficit(List<HeatPumpData> clients)
  {
    for (int i = 0; i < clients.size() - 1; i++)
    {
      for (int j = i + 1; j < clients.size(); j++)
      {
        HeatPumpData first = clients.get(i);
        HeatPumpData second = clients.get(j);

        if (second.getHeatDeficit() > first.getHeatDeficit())
        {
          clients.set(i, second);
          clients.set(j, first);
        }
      }
    }
  }
}
