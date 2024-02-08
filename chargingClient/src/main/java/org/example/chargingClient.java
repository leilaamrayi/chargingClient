package org.example;
import java.io.IOException;

public class chargingClient {
    public static void main(String[] args)throws IOException, InterruptedException {
        chargingClientManagement clientManager = new chargingClientManagement();
               clientManager.runChargingScenario();
}}