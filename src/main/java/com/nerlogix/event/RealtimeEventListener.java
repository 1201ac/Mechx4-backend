package com.nerlogix.event;

import com.nerlogix.service.SseService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RealtimeEventListener {

    private final SseService sseService;

    public RealtimeEventListener(SseService sseService) {
        this.sseService = sseService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoadStatusChanged(RoadStatusChangedEvent event) {
        sseService.broadcast("road.updated", event);
    }
}
