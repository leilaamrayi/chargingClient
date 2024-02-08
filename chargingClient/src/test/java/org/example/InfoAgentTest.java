package org.example;

import org.example.model.HourValuePair;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class InfoAgentTest {

    @Test
    void getInfo() throws IOException, InterruptedException {
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

        // score is calculated based on hourly consumption + price
        var arrHourValuePairFinalScore = arrHourlyConsumption.stream().map(pair -> {
            var hour = pair.getHour();
            double hourPrice = findValueByHour(hour, arrHourlyPrice);
            var score = hourPrice + pair.getValue();
            return new HourValuePair(pair.getHour(),score);
        }).
        sorted(Comparator.comparing(HourValuePair::getValue))
        .toList();
        //
        // score is calculated based on hour index in both arrays
        List<HourValuePair> arrHourValuePairFinalScore2 = new ArrayList<>();
        for (int i = 0; i < arrHourlyConsumption.size(); i++) {
            var consumptionIndex = i;
            var pair = arrHourlyConsumption.get(i);
            var hour = pair.getHour();
            double priceIndex = findIndexByHour(hour, arrHourlyPrice);
            var score = priceIndex + consumptionIndex;
            arrHourValuePairFinalScore2.add(new HourValuePair(pair.getHour(),score));
        }

//        List<HourValuePair> arrHourValuePairFinalScore = new ArrayList<>();
//
//        arrHourlyConsumption.forEach(pair -> {
//            var hour = pair.getHour();
//            double hourPrice = findValueByHour(hour, arrHourlyPrice);
//            var score = hourPrice + pair.getValue();
//            arrHourValuePairFinalScore.add(new HourValuePair(pair.getHour(),score));
//        });

        arrHourValuePairFinalScore2 = arrHourValuePairFinalScore2.stream().sorted(Comparator.comparing(HourValuePair::getValue)).collect(Collectors.toList());

        System.out.println(arrHourValuePairFinalScore);

    }

    private double findValueByHour(int hour, List<HourValuePair> list) {
        return list.stream().filter(pair -> pair.getHour() == hour).findFirst().get().getValue();
    }

    private int findIndexByHour(int hour, List<HourValuePair> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getHour() == hour) {
                return i;
            }
        }
        return -1;
    }

}