package io.riskstream.risk.api;

import io.riskstream.common.Alert;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertStream stream;

    public AlertController(AlertStream stream) {
        this.stream = stream;
    }

    /** Most recent alerts, newest first (used to populate the dashboard on load). */
    @GetMapping
    public List<Alert> recent() {
        return stream.recentAlerts();
    }

    /** Live alerts as Server-Sent Events. */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return stream.subscribe();
    }
}
