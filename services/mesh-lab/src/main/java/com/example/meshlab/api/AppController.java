package com.example.meshlab.api;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Validated
public class AppController {

    private final String appName;
    private final String appVersion;

    public AppController(
            @Value("${app.name}") String appName,
            @Value("${app.version}") String appVersion
    ) {
        this.appName = appName;
        this.appVersion = appVersion;
    }

    @GetMapping("/status")
    public ServiceStatusResponse status() {
        return new ServiceStatusResponse(
                appName,
                appVersion,
                "ready for Kubernetes, Helm, and Argo CD"
        );
    }

    @PostMapping("/echo")
    public EchoResponse echo(@RequestBody @Valid EchoRequest request) {
        return new EchoResponse(appName, request.message());
    }
}
