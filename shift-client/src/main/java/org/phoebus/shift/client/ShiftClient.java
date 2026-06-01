package org.phoebus.shift.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.shift.client.model.Shift;
import org.phoebus.shift.client.model.ShiftType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

/**
 * REST client for the shift service API.
 *
 * <p>Obtain an instance via the builder:
 * <pre>
 *   ShiftClient client = ShiftClient.builder()
 *       .baseUrl("http://shift-service/Shift/resources")
 *       .username("user")
 *       .password("pass")
 *       .build();
 * </pre>
 */
public class ShiftClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String authHeader;
    private final Duration readTimeout;

    private ShiftClient(Builder builder) {
        String url = builder.baseUrl;
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(builder.connectTimeout)
                .build();
        this.objectMapper = new ObjectMapper();
        this.readTimeout = builder.readTimeout;

        if (builder.username != null && !builder.username.isEmpty()) {
            String credentials = builder.username + ":" + builder.password;
            this.authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes());
        } else {
            this.authHeader = null;
        }
    }

    /**
     * Returns the currently active shift for the given type, or null if none is active.
     */
    public Shift getShift(String type) throws ShiftClientException {
        String body = get("/shift/" + type);
        if (body == null) return null;
        try {
            return objectMapper.readValue(body, Shift.class);
        } catch (Exception e) {
            throw new ShiftClientException("Failed to parse shift response for type: " + type, e);
        }
    }

    /**
     * Returns all shifts known to the service.
     */
    public List<Shift> listShifts() throws ShiftClientException {
        String body = get("/shifts");
        if (body == null) return List.of();
        try {
            return objectMapper.readValue(body, new TypeReference<List<Shift>>() {});
        } catch (Exception e) {
            throw new ShiftClientException("Failed to parse shifts response", e);
        }
    }

    /**
     * Returns all shift types configured in the service.
     */
    public List<ShiftType> listTypes() throws ShiftClientException {
        String body = get("/shiftTypes");
        if (body == null) return List.of();
        try {
            return objectMapper.readValue(body, new TypeReference<List<ShiftType>>() {});
        } catch (Exception e) {
            throw new ShiftClientException("Failed to parse shift types response", e);
        }
    }

    private String get(String path) throws ShiftClientException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(readTimeout)
                .GET();

        if (authHeader != null) {
            requestBuilder.header("Authorization", authHeader);
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status == 200) return response.body();
            if (status == 404) return null;
            throw new ShiftClientException(
                    "Shift service returned HTTP " + status + " for GET " + path);

        } catch (ShiftClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ShiftClientException(
                    "Failed to reach shift service at " + baseUrl + path, e);
        }
    }

    /** Returns the base URL this client is configured with. Useful for tests. */
    public String baseUrl() {
        return baseUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String baseUrl = "http://localhost:8080/Shift/resources";
        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration readTimeout = Duration.ofSeconds(5);
        private String username;
        private String password = "";

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        public Builder readTimeout(Duration timeout) {
            this.readTimeout = timeout;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public ShiftClient build() {
            return new ShiftClient(this);
        }
    }
}
