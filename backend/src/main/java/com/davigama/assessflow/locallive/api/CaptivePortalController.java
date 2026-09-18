package com.davigama.assessflow.locallive.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "app.local-live.enabled", havingValue = "true")
public class CaptivePortalController {
    @GetMapping({
            "/generate_204",
            "/gen_204",
            "/hotspot-detect.html",
            "/connecttest.txt",
            "/ncsi.txt",
            "/success.txt",
            "/library/test/success.html",
            "/canonical.html"
    })
    public ResponseEntity<Void> captive() {
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "/join").build();
    }
}
