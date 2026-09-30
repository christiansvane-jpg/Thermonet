package Server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ElectricityPriceReader
{
    public Map<String, Double> readPrices(String csvFilePath)
    {
        Map<String, Double> prices = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath)))
        {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null)
            {
                line = line.replace("\uFEFF", "").trim();

                if (line.isEmpty())
                {
                    continue;
                }

                if (firstLine)
                {
                    firstLine = false;
                    continue;
                }

                String[] parts = line.split(";");

                if (parts.length < 2)
                {
                    continue;
                }

                String key = parts[0]
                        .replace("\uFEFF", "")
                        .replace("\u00A0", " ")
                        .trim();

                String value = parts[1]
                        .replace("\uFEFF", "")
                        .replace("\u00A0", " ")
                        .replace(",", ".")
                        .trim();

                if (key.isEmpty() || value.isEmpty())
                {
                    continue;
                }

                String keyLower = key.toLowerCase();

                if (keyLower.contains("average")
                        || keyLower.contains("gennemsnit")
                        || keyLower.contains("årsgennemsnit")
                        || keyLower.contains("aarsgennemsnit"))
                {
                    continue;
                }

                try
                {
                    double orePerKwh = Double.parseDouble(value);
                    double dkkPerKwh = orePerKwh / 100.0;

                    prices.put(key, dkkPerKwh);

                    System.out.println("[CSV] Loaded row: " + key + " = " + dkkPerKwh);
                }
                catch (NumberFormatException e)
                {
                    System.out.println("[CSV] Skipping row with invalid value: " + line);
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("[CSV] Could not read file: " + csvFilePath + " -> " + e.getMessage());
        }

        System.out.println("[CSV] Loaded " + prices.size() + " price entries from " + csvFilePath);
        System.out.println("[CSV] Final prices: " + prices);

        return prices;
    }
}
