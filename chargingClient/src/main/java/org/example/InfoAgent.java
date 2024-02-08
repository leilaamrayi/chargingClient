package org.example;

import com.google.gson.Gson;
import org.example.model.Info;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class InfoAgent {

    private static final String BASE_URL = "http://127.0.0.1:5001";
    private static final String INFO_ENDPOINT = "/info";

    public static Info getInfo() throws IOException, InterruptedException {
        String response = sendGETRequest(BASE_URL + INFO_ENDPOINT);
        Gson gson = new Gson();
        return gson.fromJson(response, Info.class);
    }

    private static String sendGETRequest(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

}
