package jp.co.soramitsu.iroha.java.health_check;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.net.URL;

public class HealthCheckClient {

    private static final String HEALTH_CHECK_ENDPOINT = "/healthcheck";
    private static final int DEFAULT_HEALTH_CHECK_PORT = 50508;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private final URL healthCheckUrl;

    @SneakyThrows
    public HealthCheckClient(String peerHost) {
        healthCheckUrl = new URL("http", peerHost, DEFAULT_HEALTH_CHECK_PORT, HEALTH_CHECK_ENDPOINT);
    }

    @SneakyThrows
    public HealthCheckClient(String peerHost, int healthCheckPort) {
        healthCheckUrl = new URL("http", peerHost, healthCheckPort, HEALTH_CHECK_ENDPOINT);
    }

    public HealthResponse check() {
        val request = new Request.Builder()
                .get()
                .url(healthCheckUrl)
                .build();

        try (val response = client.newCall(request).execute()) {
            return mapper.readValue(response.body().bytes(), HealthResponse.class);
        } catch (Exception ex) {
            return HealthResponse.unhealthy();
        }
    }

}
