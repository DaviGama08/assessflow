package com.davigama.assessflow.locallive.api;

import com.davigama.assessflow.identity.domain.User;
import com.davigama.assessflow.locallive.api.LocalLiveDtos.HotspotResponse;
import com.davigama.assessflow.locallive.api.LocalLiveDtos.PackageImportResponse;
import com.davigama.assessflow.locallive.api.LocalLiveDtos.StatusResponse;
import com.davigama.assessflow.locallive.application.AssessmentPackageService;
import com.davigama.assessflow.locallive.application.LocalLiveService;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/local-live")
@ConditionalOnProperty(name = "app.local-live.enabled", havingValue = "true")
public class LocalLiveController {
    private final LocalLiveService localLive;
    private final AssessmentPackageService packages;

    public LocalLiveController(LocalLiveService localLive, AssessmentPackageService packages) {
        this.localLive = localLive;
        this.packages = packages;
    }

    @GetMapping("/status")
    public StatusResponse status() {
        return localLive.status();
    }

    @GetMapping("/join-url/{code}")
    public Map<String, String> joinUrl(@PathVariable String code) {
        return Map.of("url", localLive.joinUrl(code));
    }

    @GetMapping("/hotspot")
    public HotspotResponse hotspot() {
        return localLive.hotspot();
    }

    @PostMapping("/hotspot/start")
    public HotspotResponse startHotspot() {
        return localLive.startHotspot();
    }

    @PostMapping("/hotspot/stop")
    public HotspotResponse stopHotspot() {
        return localLive.stopHotspot();
    }

    @PostMapping("/packages")
    public PackageImportResponse importPackage(@RequestBody Map<String, Object> body, Authentication authentication) {
        return packages.importPackage((User) authentication.getPrincipal(), body);
    }
}
