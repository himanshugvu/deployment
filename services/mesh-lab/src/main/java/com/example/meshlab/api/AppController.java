package com.example.meshlab.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api")
@Validated
public class AppController {

    private final String appName;
    private final String appVersion;
    private final int chaosErrorRate;

    public AppController(
            @Value("${app.name}") String appName,
            @Value("${app.version}") String appVersion,
            @Value("${chaos.error-rate:0}") int chaosErrorRate
    ) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.chaosErrorRate = chaosErrorRate;
    }

    @GetMapping("/status")
    public ServiceStatusResponse status() {
        maybeInjectFailure();
        return new ServiceStatusResponse(
                appName,
                appVersion,
                "ready for Kubernetes, Helm, and Argo CD"
        );
    }

    @PostMapping("/echo")
    public EchoResponse echo(@RequestBody @Valid EchoRequest request) {
        maybeInjectFailure();
        return new EchoResponse(appName, request.message());
    }

    void maybeInjectFailure() {
        if (chaosErrorRate > 0 && ThreadLocalRandom.current().nextInt(100) < chaosErrorRate) {
            throw new ChaosException();
        }
    }
}
