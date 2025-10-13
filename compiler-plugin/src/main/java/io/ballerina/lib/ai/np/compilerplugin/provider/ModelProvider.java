package io.ballerina.lib.ai.np.compilerplugin.provider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public interface ModelProvider extends CodeGenerator {
    default String sendRequest(HttpClient httpClient, String url,
                               String jsonPayload, Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

        headers.entrySet().forEach(header -> requestBuilder.header(header.getKey(), header.getValue()));

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode > 300) {
            throw new IOException("HTTP request failed with status code " + statusCode +
                    " and body: " + response.body());
        }

        return response.body();
    }
}
