package sales_savvy_backend.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class AiService {

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.model:claude-haiku-5-5}")
    private String model;

    private final HttpClient client = HttpClient.newHttpClient();

    public String generateDescription(String productName, String category) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("AI is not configured on the server");
        }

        String prompt = "Write a product description of two short sentences for an online store. "
                + "Product name: " + productName + ". Category: " + category + ". "
                + "Use plain text only. Do not invent specifications, numbers or brand claims "
                + "that were not given.";

        JSONObject message = new JSONObject().put("role", "user").put("content", prompt);
        JSONObject body = new JSONObject()
                .put("model", model)
                .put("max_tokens", 200)
                .put("messages", new JSONArray().put(message));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("The AI service returned an error (" + response.statusCode() + ")");
            }
            JSONObject json = new JSONObject(response.body());
            return json.getJSONArray("content").getJSONObject(0).getString("text").trim();
        } catch (IOException e) {
            throw new RuntimeException("The AI service could not be reached");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("The AI request was interrupted");
        }
    }
}
