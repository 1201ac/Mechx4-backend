package com.nerlogix.event;

import com.nerlogix.service.SseService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SseEventListener {

    private final SseService sseService;

    public SseEventListener(SseService sseService) {
        this.sseService = sseService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleSseDomainEvent(SseDomainEvent event) {
        if (event.getTargetUserId() != null) {
            sseService.sendToUser(event.getTargetUserId(), event.getEventName(), event.getData());
        } else {
            sseService.broadcast(event.getEventName(), event.getData());
        }
    }
}
