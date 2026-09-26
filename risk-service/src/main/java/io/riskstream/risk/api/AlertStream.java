package io.riskstream.risk.api;

import io.riskstream.common.Alert;
import io.riskstream.common.Topics;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Bridges the alerts topic to browsers over Server-Sent Events. Each service instance uses a
 * unique consumer group so that every instance receives every alert and can serve its own
 * connected dashboards, and reads from the latest offset since history is served from memory.
 */
@Component
public class AlertStream {

    private static final int MAX_RECENT = 200;

    private final Deque<Alert> recent = new ConcurrentLinkedDeque<>();
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    @KafkaListener(
            topics = Topics.ALERTS,
            groupId = "#{'alerts-ui-' + T(java.util.UUID).randomUUID()}",
            properties = {
                    "spring.json.value.default.type=io.riskstream.common.Alert",
                    "auto.offset.reset=latest"
            })
    public void onAlert(Alert alert) {
        recent.addFirst(alert);
        while (recent.size() > MAX_RECENT) {
            recent.pollLast();
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("alert").data(alert, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public List<Alert> recentAlerts() {
        return new ArrayList<>(recent);
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout; heartbeats keep proxies open
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    @Scheduled(fixedRate = 15_000)
    void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }
}
