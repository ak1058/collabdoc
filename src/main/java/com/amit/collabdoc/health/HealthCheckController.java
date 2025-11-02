package com.amit.collabdoc.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;

/**
 * A simple controller to check if the application is running.
 */
@RestController
public class HealthCheckController {

    /**
     * @return A simple JSON object indicating the server status.
     */
    @GetMapping("/api/health")
    public Map<String, String> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "CollabDoc service is running!");
        return response;
    }
}