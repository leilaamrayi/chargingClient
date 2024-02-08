package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.example.model.HourValuePair;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class chargingClientManagement {
    // Base URL and endpoints for API requests
    private static final String BASE_URL = "http://127.0.0.1:5001";
    private static final String BASELOAD_ENDPOINT = "/baseload";
    private static final String PRICE_PER_HOUR_ENDPOINT = "/priceperhour";
    private static final String CHARGE_ENDPOINT = "/charge";
    // Arrays to store hourly consumptions and prices
    private double[] hourlyConsumptions;
    private double[] hourlyPrice;
    // Battery capacity percentage
    private double evBattCapacityPercent;

    // Lowest consumption and price variables
    private double lowestConsumption;
    private double lowestPrice;

    // Scanner object for user input
    private Scanner scanner;

    // Simulation time variables
    private int simTimeHour;
    private int simTimeMin;

    // Constructor
    public chargingClientManagement() {
        scanner = new Scanner(System.in);
    }

    // Method to retrieve hourly prices from the API
    public double[] getHourlyPrices() throws IOException, InterruptedException {
        String response = sendGETRequest(BASE_URL + PRICE_PER_HOUR_ENDPOINT);
        JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
        hourlyPrice = new double[jsonArray.size()];
        System.out.println("\nHourly price of area 3 Stockholm: ");
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement element = jsonArray.get(i);
            hourlyPrice[i] = element.getAsDouble();
            System.out.print(hourlyPrice[i] + " ");

        }
        return hourlyPrice;
    }

    // Method to retrieve hourly consumptions from the API
    public double[] getHourlyConsumptions() throws IOException, InterruptedException {
        String response = sendGETRequest(BASE_URL + BASELOAD_ENDPOINT);
        JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
        hourlyConsumptions = new double[jsonArray.size()];
        System.out.println("\nThe household's energy consumption during a 24-hour period:");
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement element = jsonArray.get(i);
            hourlyConsumptions[i] = element.getAsDouble();
            System.out.print(hourlyConsumptions[i] + " ");
        }
        return hourlyConsumptions;
    }



    // Method to find the lowest price from an array of prices
    private double findLowestPrice(double[] hourlyPrice) {
        lowestPrice = Double.MAX_VALUE;

        for (double price : hourlyPrice) {
            if (price < lowestPrice) {
                lowestPrice = price;
            }
        }
        return lowestPrice;
    }

    // Method to find the lowest consumption from an array of consumptions
    private double findLowestConsumption(double[] hourlyConsumptions) {
        lowestConsumption = Double.MAX_VALUE;
        for (double consumption : hourlyConsumptions) {
            if (consumption < lowestConsumption) {
                lowestConsumption = consumption;
            }
        }
        return lowestConsumption;
    }

    // Method to retrieve the battery capacity percentage from the API
    double getEvBatteryCapacityPercent() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + CHARGE_ENDPOINT))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            String responseBody = response.body();
            evBattCapacityPercent = Double.parseDouble(responseBody);
            System.out.println("\nBattery charge is: " + evBattCapacityPercent);
            return evBattCapacityPercent;
        } else {
            System.out.println("Failed to retrieve battery capacity percentage. Status code: " + response.statusCode());
            return -1.0;
        }
    }

    // Method to control charging based on user input
    private void controlCharging() throws IOException, InterruptedException {
        System.out.println("Do you want to start or stop charging the EV's battery? (1=start/2=stop)");
        String choice = scanner.next().toLowerCase();
        if ("1".equals(choice)) {
            startCharging();
            getEvBatteryCapacityPercent();
        } else if ("2".equals(choice)) {
            stopCharging();
        } else {
            System.out.println("Invalid choice. Please enter '1' to start or '2' to stop charging.");
        }
    }


    void chargingBatteryWithLowestConsumption() throws IOException, InterruptedException {
        var oldBatteryCapacityValue = InfoAgent.getInfo().getBatteryCapacityKWh();
        var currentBatteryCapacityPercent = getEvBatteryCapacityPercent();
        hourlyConsumptions = getHourlyConsumptions();
        lowestConsumption = findLowestConsumption(hourlyConsumptions);
        double totalConsumption = 0;
        if (currentBatteryCapacityPercent >= 20.0 && currentBatteryCapacityPercent < 80.0) {
            totalConsumption = lowestConsumption;
            simTimeHour = 1; // maybe dynamic later
            //
            startCharging();
            System.out.println("Charging started for the hour with the lowest consumption value: " + lowestConsumption);
            System.out.println("Charging started at: " + formatTime(simTimeHour, simTimeMin)); // Print the start time

            //
            // old_battery_capacity_value has to be retrieved already (via GET /info endpoint) to be able to proceed calculation
            // get the current_battery_capacity_value via GET /info endpoint
            // get the current_battery_capacity_percent via GET /charge endpoint
            // totalConsumption += (current_battery_capacity_value - old_battery_capacity)
            // stop charge and maybe log, if totalConsumption exceeds 11kw or current_battery_capacity_percent > 80
            // else log the current status
            //
            while (true) {
                Thread.sleep(500);
                var currentBatteryCapacityValue = InfoAgent.getInfo().getBatteryCapacityKWh();
                currentBatteryCapacityPercent = getEvBatteryCapacityPercent();
                totalConsumption += currentBatteryCapacityValue - oldBatteryCapacityValue;
                totalConsumption = Math.round(totalConsumption * 100.0) / 100.0;
                System.out.println("Total energy consumption : " + totalConsumption);

                if (totalConsumption >= 11.0 || currentBatteryCapacityPercent >= 80.0) {
                    stopCharging();
                    System.out.println("Charging stopped. Total energy consumption exceeds 11.0 kWh or battery charge exceeds 80%.");
                    break;
                }
            }

        }
    }

    void chargingBatteryWithLowestPrice() throws IOException, InterruptedException {
        var oldBatteryCapacityValue = InfoAgent.getInfo().getBatteryCapacityKWh();
        var currentBatteryCapacityPercent = getEvBatteryCapacityPercent();
        hourlyConsumptions = getHourlyConsumptions();
        hourlyPrice = getHourlyPrices();
        lowestPrice = findLowestPrice(hourlyPrice);
        double totalConsumption = 0;
        double consumptionForLowestPrice = -1;
        // Find the index of the lowest price
        int lowestPriceIndex = -1;
        for (int i = 0; i < hourlyPrice.length; i++) {
            if (hourlyPrice[i] == lowestPrice) {
                lowestPriceIndex = i;
                break;
            }
        }

        // Start charging if the lowest price is found
        if (lowestPriceIndex != -1) {
            consumptionForLowestPrice = hourlyConsumptions[lowestPriceIndex];
            totalConsumption = consumptionForLowestPrice;
            simTimeHour = lowestPriceIndex + 1; // Adjust simTimeHour accordingly

            startCharging();
            System.out.println("Charging started at: " + formatTime(simTimeHour, simTimeMin)); // Print the start time
            System.out.println("The lowest price is: " + lowestPrice);


            // Charging loop
            while (true) {
                Thread.sleep(500);
                var currentBatteryCapacityValue = InfoAgent.getInfo().getBatteryCapacityKWh();
                currentBatteryCapacityPercent = getEvBatteryCapacityPercent();
                totalConsumption += currentBatteryCapacityValue - oldBatteryCapacityValue;
                totalConsumption = Math.round(totalConsumption * 100.0) / 100.0;
                System.out.println("Total energy consumption : " + totalConsumption);

                // Check stopping conditions
                if (totalConsumption >= 11.0 || currentBatteryCapacityPercent >= 80.0) {
                    stopCharging();
                    System.out.println("Charging stopped. Total energy consumption exceeds 11.0 kWh or battery charge exceeds 80%.");
                    break;
                }

            }
        }
    }

        void chargingBatteryWithBestTime() throws IOException, InterruptedException {
        chargingClientManagement c = new chargingClientManagement();
        var hourlyConsumptions = c.getHourlyConsumptions();
        var hourlyPrices = c.getHourlyPrices();
        // convert to HourValuePair array
        AtomicInteger atomicHour = new AtomicInteger();

        var arrHourlyConsumption = Arrays.stream(hourlyConsumptions)
                .mapToObj(value -> new HourValuePair(atomicHour.incrementAndGet(), value))
                .sorted(Comparator.comparing(HourValuePair::getValue))
                .toList();
        atomicHour.set(0);
        var arrHourlyPrice = Arrays.stream(hourlyPrices)
                .mapToObj(value -> new HourValuePair(atomicHour.incrementAndGet(), value))
                .sorted(Comparator.comparing(HourValuePair::getValue))
                .toList();

        // score is calculated based on hour index in both arrays
        List<HourValuePair> arrHourValuePairFinalScore2 = new ArrayList<>();
        for (int consumptionIndex = 0; consumptionIndex < arrHourlyConsumption.size(); consumptionIndex++) {
            var pair = arrHourlyConsumption.get(consumptionIndex);
            var hour = pair.getHour();
            double priceIndex = findIndexByHour(hour, arrHourlyPrice);
            var score = priceIndex + consumptionIndex;
            arrHourValuePairFinalScore2.add(new HourValuePair(pair.getHour(),score));
        }

        arrHourValuePairFinalScore2 = arrHourValuePairFinalScore2.stream().sorted(Comparator.comparing(HourValuePair::getValue)).collect(Collectors.toList());

        var hour = arrHourValuePairFinalScore2.get(0).getHour();
        var hourlyPrice = findByHour(hour, arrHourlyPrice).getValue();
        var hourlyConsumption = findByHour(hour, arrHourlyConsumption).getValue();
        var batteryCapacityKWh = InfoAgent.getInfo().getBatteryCapacityKWh();
        var batteryCapacityPercent = getEvBatteryCapacityPercent();

        System.out.println("Battery Capacity KWh: "+batteryCapacityKWh+", Battery Capacity Percent: "+batteryCapacityPercent);
        System.out.println("Optimum choice is: Hour "+hour+" , Hourly Price: "+hourlyPrice+", Hourly Consumption: "+hourlyConsumption );
    }

    private HourValuePair findByHour(int hour, List<HourValuePair> list) {
        return list.stream().filter(pair -> pair.getHour() == hour).findFirst().get();
    }

    private int findIndexByHour(int hour, List<HourValuePair> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getHour() == hour) {
                return i;
            }
        }
        return -1;
    }

       // Method to start charging
    private void startCharging() throws IOException, InterruptedException {
        String response = sendPOSTRequest(BASE_URL + CHARGE_ENDPOINT, "{\"charging\": \"on\"}");
        System.out.println("\n"+response);
    }

    // Method to stop charging
    private void stopCharging() throws IOException, InterruptedException {
        String response = sendPOSTRequest(BASE_URL + CHARGE_ENDPOINT, "{\"charging\": \"off\"}");
        System.out.println(response);
    }

    // Method to send a GET request to the API
    private String sendGETRequest(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    // Method to send a POST request to the API
    private String sendPOSTRequest(String url, String jsonBody) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    // Method to format time in HH:mm format
    private String formatTime(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }

    // Method to run the charging scenario
    public void runChargingScenario() throws IOException, InterruptedException {
        System.out.println("Welcome to the Charging Management System.");
        while (true) {
            System.out.println("\nPlease select an option:");
            System.out.println("1. Download hourly price information for electricity in area 3 Stockholm.");
            System.out.println("2. Retrieve household's energy consumption during a 24-hour period.");
            System.out.println("3. Start and stop charging.");
            System.out.println("4. Start charging with lowest consumption.");
            System.out.println("5. Start charging with lowest price.");
            System.out.println("6. Show optimized loading.");
            System.out.println("7. Exit.");

            // Read user input
            int choice = scanner.nextInt();
            switch (choice) {
                case 1:
                    getHourlyPrices();
                    break;
                case 2:
                    getHourlyConsumptions();
                    break;
                case 3:
                    controlCharging();
                    break;
                case 4:
                    chargingBatteryWithLowestConsumption();
                    break;
                case 5:
                    chargingBatteryWithLowestPrice();
                    break;
                case 6:
                    chargingBatteryWithBestTime();
                    break;
                case 7:
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Invalid choice. Please select again.");
            }
        }
    }
}