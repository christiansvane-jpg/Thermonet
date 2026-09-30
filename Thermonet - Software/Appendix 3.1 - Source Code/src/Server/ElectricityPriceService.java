package Server;

import Shared.SystemConfig;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ElectricityPriceService
{
    private final Map<String, Double> prices;

    private static final double DEFAULT_PRICE = 0.61;

    private int demoMonth = 1;

    private static final String[] DANISH_MONTHS = {
            "",
            "Januar", "Februar", "Marts", "April", "Maj", "Juni",
            "Juli", "August", "September", "Oktober", "November", "December"
    };

    public ElectricityPriceService(Map<String, Double> prices)
    {
        this.prices = prices;

        System.out.println("[PriceService] Available CSV keys: " + prices.keySet());

        startDemoMonthTimer();
    }

    private void startDemoMonthTimer()
    {
        Timer timer = new Timer(true);

        timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                demoMonth++;

                if (demoMonth > 12)
                    demoMonth = 1;

                System.out.println("[PriceService] Demo month changed to: "
                        + getCurrentMonthName()
                        + " | price: "
                        + getCurrentPrice());
            }

        }, 30000, 30000);
    }

    public double getCurrentPrice()
    {
        String key = getCurrentMonthName();

        System.out.println("[PriceService] Looking for month: '" + key + "'");
        System.out.println("[PriceService] Available CSV keys: " + prices.keySet());

        if (prices.containsKey(key))
            return prices.get(key);

        System.out.println("[PriceService] No price found for '" + key
                + "' - using default: " + DEFAULT_PRICE);

        return DEFAULT_PRICE;
    }

    public String getCurrentMonthName()
    {
        return DANISH_MONTHS[demoMonth];
    }

    public double getPriceForKey(String key)
    {
        return prices.getOrDefault(key, DEFAULT_PRICE);
    }

    public boolean isCheap()
    {
        return getCurrentPrice() <= SystemConfig.CHEAP_PRICE_THRESHOLD;
    }

    public boolean isExpensive()
    {
        return getCurrentPrice() >= SystemConfig.EXPENSIVE_PRICE_THRESHOLD;
    }

}
