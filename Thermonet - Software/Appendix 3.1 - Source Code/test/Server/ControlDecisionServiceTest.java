package Server;

import Shared.ControlCommand;
import Shared.HeatPumpData;
import Shared.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ControlDecisionServiceTest
{
    private ControlDecisionService service;

    private static final double CHEAP =
            SystemConfig.CHEAP_PRICE_THRESHOLD - 0.01;

    private static final double MEDIUM =
            (SystemConfig.CHEAP_PRICE_THRESHOLD + SystemConfig.EXPENSIVE_PRICE_THRESHOLD) / 2;

    private static final double EXPENSIVE =
            SystemConfig.EXPENSIVE_PRICE_THRESHOLD + 0.01;

    @BeforeEach
    void setUp()
    {
        service = new ControlDecisionService();
    }

    @Test
    void testSafetyFallback_alwaysStartsEvenWhenExpensiveAndNoCapacity()
    {
        HeatPumpData data = new HeatPumpData("HP1", 17.0, 21.0, 40.0, false);
        ControlCommand cmd = service.decide(data, EXPENSIVE, 99);
        assertEquals(ControlCommand.START_HEATING, cmd);
    }

    @Test
    void testSafetyFallback_exactlyAtMargin()
    {
        HeatPumpData data = new HeatPumpData("HP1", 19.0, 21.0, 40.0, false);
        ControlCommand cmd = service.decide(data, EXPENSIVE, 99);
        assertEquals(ControlCommand.START_HEATING, cmd);
    }

    @Test
    void testCheapPrice_needsHeat_startsHeating()
    {
        HeatPumpData data = new HeatPumpData("HP1", 20.0, 21.0, 40.0, false);
        ControlCommand cmd = service.decide(data, CHEAP, 0);
        assertEquals(ControlCommand.START_HEATING, cmd);
    }

    @Test
    void testCheapPrice_houseWarm_bufferNotFull_storesInBuffer()
    {
        HeatPumpData data = new HeatPumpData("HP1", 21.5, 21.0, 40.0, false);
        ControlCommand cmd = service.decide(data, CHEAP, 0);
        assertEquals(ControlCommand.STORE_HEAT_IN_BUFFER, cmd);
    }

    @Test
    void testCheapPrice_bufferFull_doesNotStore()
    {
        HeatPumpData data = new HeatPumpData("HP1", 21.5, 21.0, 55.0, false);
        ControlCommand cmd = service.decide(data, CHEAP, 0);
        assertNotEquals(ControlCommand.STORE_HEAT_IN_BUFFER, cmd);
    }

    @Test
    void testCheapPrice_needsHeatButNoCapacity_waits()
    {
        HeatPumpData data = new HeatPumpData("HP1", 20.0, 21.0, 40.0, false);

        ControlCommand cmd = service.decide(
                data,
                CHEAP,
                SystemConfig.MAX_RUNNING_HEAT_PUMPS
        );

        assertEquals(ControlCommand.WAIT, cmd);
    }

    @Test
    void testCheapPrice_houseWarmBufferNotFullButNoCapacity_waits()
    {
        HeatPumpData data = new HeatPumpData("HP1", 21.5, 21.0, 40.0, false);

        ControlCommand cmd = service.decide(
                data,
                CHEAP,
                SystemConfig.MAX_RUNNING_HEAT_PUMPS
        );

        assertEquals(ControlCommand.WAIT, cmd);
    }

    @Test
    void testMediumPrice_needsHeat_waits()
    {
        HeatPumpData data = new HeatPumpData("HP1", 20.0, 21.0, 40.0, false);
        ControlCommand cmd = service.decide(data, MEDIUM, 0);
        assertEquals(ControlCommand.WAIT, cmd);
    }

    @Test
    void testMediumPrice_houseWarm_waits()
    {
        HeatPumpData data = new HeatPumpData("HP1", 21.5, 21.0, 40.0, false);
        ControlCommand cmd = service.decide(data, MEDIUM, 0);
        assertEquals(ControlCommand.WAIT, cmd);
    }

    @Test
    void testExpensivePrice_pumpRunning_houseWarm_stops()
    {
        HeatPumpData data = new HeatPumpData("HP1", 21.5, 21.0, 40.0, true);
        ControlCommand cmd = service.decide(data, EXPENSIVE, 0);
        assertEquals(ControlCommand.STOP_HEATING, cmd);
    }

    @Test
    void testExpensivePrice_pumpIdle_houseWarm_waits()
    {
        HeatPumpData data = new HeatPumpData("HP1", 21.5, 21.0, 40.0, false);
        ControlCommand cmd = service.decide(data, EXPENSIVE, 0);
        assertEquals(ControlCommand.WAIT, cmd);
    }

    @Test
    void testExpensivePrice_smallDeficit_waits()
    {
        HeatPumpData data = new HeatPumpData("HP1", 20.0, 21.0, 40.0, false);
        ControlCommand cmd = service.decide(data, EXPENSIVE, 0);
        assertEquals(ControlCommand.WAIT, cmd);
    }

    @Test
    void testCapacityLimit_prioritisesHighestDeficit()
    {
        HeatPumpData hpA = new HeatPumpData("HP-A", 20.0, 21.0, 40.0, false);
        HeatPumpData hpB = new HeatPumpData("HP-B", 17.5, 21.0, 40.0, false);
        HeatPumpData hpC = new HeatPumpData("HP-C", 19.5, 21.0, 40.0, false);

        List<HeatPumpData> clients = new ArrayList<>();
        clients.add(hpA);
        clients.add(hpB);
        clients.add(hpC);

        Map<String, ControlCommand> decisions =
                service.decideAll(clients, CHEAP);

        assertEquals(ControlCommand.START_HEATING, decisions.get("HP-B"));
        assertEquals(ControlCommand.START_HEATING, decisions.get("HP-C"));
        assertEquals(ControlCommand.WAIT, decisions.get("HP-A"));
    }

    @Test
    void testDecideAll_noClients_returnsEmptyMap()
    {
        List<HeatPumpData> clients = new ArrayList<>();
        Map<String, ControlCommand> decisions =
                service.decideAll(clients, CHEAP);

        assertTrue(decisions.isEmpty());
    }
}
