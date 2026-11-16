package com.nikitarybalko.wargamebroker.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.support.TestApiKeys;
import com.nikitarybalko.wargamebroker.support.TestEndpoints;
import com.nikitarybalko.wargamebroker.web.dto.SubmitUnitsRequest;
import com.nikitarybalko.wargamebroker.web.dto.UnitDto;
import com.nikitarybalko.wargamebroker.web.security.ApiKeyAuthFilter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Prepares the battlefield of the running stack through the public REST API, so dashboard tests are deterministic.
 */
final class BrokerApiClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String JSON = "application/json";
    private static final int FIRST_ERROR_STATUS = 400;
    private static final Map<Side, String> SIDE_KEYS = Map.of(Side.BLUE, TestApiKeys.BLUE, Side.RED, TestApiKeys.RED);

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    BrokerApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    boolean isReachable() {
        try {
            return send(get(TestEndpoints.STATUS, TestApiKeys.ADMIN)).statusCode() < FIRST_ERROR_STATUS;
        } catch (IOException e) {
            return false;
        }
    }

    void startNewBattle() throws IOException {
        expectSuccess(post(TestEndpoints.RESET, TestApiKeys.ADMIN, ""));
        expectSuccess(post(TestEndpoints.INIT, TestApiKeys.ADMIN, ""));
        expectSuccess(post(TestEndpoints.REGISTER, TestApiKeys.BLUE, ""));
        expectSuccess(post(TestEndpoints.REGISTER, TestApiKeys.RED, ""));
    }

    void playTick(long tick, List<UnitDto> blueUnits, List<UnitDto> redUnits) throws IOException {
        submit(Side.BLUE, tick, blueUnits);
        submit(Side.RED, tick, redUnits);
    }

    JsonNode status() throws IOException {
        return objectMapper.readTree(expectSuccess(get(TestEndpoints.STATUS, TestApiKeys.ADMIN)).body());
    }

    private void submit(Side side, long tick, List<UnitDto> units) throws IOException {
        String body = objectMapper.writeValueAsString(SubmitUnitsRequest.builder().tickNumber(tick).units(units).build());
        expectSuccess(post(TestEndpoints.SUBMIT, SIDE_KEYS.get(side), body));
    }

    private HttpRequest get(String path, String apiKey) {
        return request(path, apiKey).GET().build();
    }

    private HttpRequest post(String path, String apiKey, String body) {
        return request(path, apiKey).header("Content-Type", JSON).POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }

    private HttpRequest.Builder request(String path, String apiKey) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header(ApiKeyAuthFilter.API_KEY_HEADER, apiKey);
    }

    private HttpResponse<String> expectSuccess(HttpRequest request) throws IOException {
        HttpResponse<String> response = send(request);
        if (response.statusCode() >= FIRST_ERROR_STATUS) {
            throw new IOException(request.method() + " " + request.uri() + " failed with " + response.statusCode() + ": " + response.body());
        }
        return response;
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while calling the broker", e);
        }
    }
}
