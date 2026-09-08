package demo.readonlyagent;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResultSanitizerTest {
    @Test
    void removesCredentialsAndConnectionDetails() {
        Map<String, Object> result = ResultSanitizer.device(Map.of(
                "deviceId", "DEVICE-001",
                "name", "Camera A",
                "status", "ONLINE",
                "password", "secret",
                "accessToken", "token",
                "connectionUrl", "protocol://internal-host"));

        assertEquals("DEVICE-001", result.get("deviceId"));
        assertFalse(result.containsKey("password"));
        assertFalse(result.containsKey("accessToken"));
        assertFalse(result.containsKey("connectionUrl"));
    }
}
