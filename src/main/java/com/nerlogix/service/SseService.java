package com.nerlogix.service;

import com.nerlogix.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);
    
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final List<SseEmitter> anonymousEmitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter(User currentUser) {
        SseEmitter emitter = new SseEmitter(1800_000L); // 30 minutes

        if (currentUser != null) {
            userEmitters.computeIfAbsent(currentUser.getId(), k -> new CopyOnWriteArrayList<>()).add(emitter);
        } else {
            anonymousEmitters.add(emitter);
        }

        Runnable remove = () -> {
            if (currentUser != null) {
                List<SseEmitter> list = userEmitters.get(currentUser.getId());
                if (list != null) {
                    list.remove(emitter);
                    if (list.isEmpty()) {
                        userEmitters.remove(currentUser.getId());
                    }
                }
            } else {
                anonymousEmitters.remove(emitter);
            }
        };

        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data("connected"));
        } catch (IOException e) {
            remove.run();
        }

        return emitter;
    }

    public void broadcast(String eventName, Object data) {
        List<SseEmitter> allEmitters = new ArrayList<>(anonymousEmitters);
        for (List<SseEmitter> list : userEmitters.values()) {
            allEmitters.addAll(list);
        }

        for (SseEmitter emitter : allEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                // Ignore, onCompletion/onError cleanup will handle
            }
        }
    }

    public void sendToUser(Long userId, String eventName, Object data) {
        if (userId == null) return;
        List<SseEmitter> list = userEmitters.get(userId);
        if (list == null || list.isEmpty()) return;

        for (SseEmitter emitter : new ArrayList<>(list)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                list.remove(emitter);
            }
        }
    }

    @Scheduled(fixedRate = 20000)
    public void sendHeartbeat() {
        for (SseEmitter emitter : new ArrayList<>(anonymousEmitters)) {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception e) {
                anonymousEmitters.remove(emitter);
            }
        }

        for (Map.Entry<Long, List<SseEmitter>> entry : userEmitters.entrySet()) {
            List<SseEmitter> list = entry.getValue();
            for (SseEmitter emitter : new ArrayList<>(list)) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (Exception e) {
                    list.remove(emitter);
                }
            }
        }
    }
}
