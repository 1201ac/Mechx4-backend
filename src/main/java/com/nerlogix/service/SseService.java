package com.nerlogix.service;

import com.nerlogix.entity.User;
import com.nerlogix.model.enums.Role;
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
    private final Map<Long, Role> userRoles = new ConcurrentHashMap<>();
    private final List<SseEmitter> anonymousEmitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter(User currentUser) {
        SseEmitter emitter = new SseEmitter(1800_000L); // 30 minutes

        if (currentUser != null) {
            userEmitters.computeIfAbsent(currentUser.getId(), k -> new CopyOnWriteArrayList<>()).add(emitter);
            if (currentUser.getRole() != null) {
                userRoles.put(currentUser.getId(), currentUser.getRole());
            }
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
                        userRoles.remove(currentUser.getId());
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
        // Send to anonymous
        for (SseEmitter emitter : new ArrayList<>(anonymousEmitters)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                anonymousEmitters.remove(emitter);
            }
        }

        // Send to users
        for (Map.Entry<Long, List<SseEmitter>> entry : userEmitters.entrySet()) {
            List<SseEmitter> list = entry.getValue();
            for (SseEmitter emitter : new ArrayList<>(list)) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                } catch (Exception e) {
                    list.remove(emitter);
                }
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

    public void sendToRole(String eventName, Object data, Role... roles) {
        if (roles == null || roles.length == 0) {
            broadcast(eventName, data);
            return;
        }

        Set<Role> allowedRoles = Set.of(roles);
        for (Map.Entry<Long, Role> entry : userRoles.entrySet()) {
            if (allowedRoles.contains(entry.getValue())) {
                sendToUser(entry.getKey(), eventName, data);
            }
        }
    }

    @Scheduled(fixedRate = 20000)
    public void sendHeartbeat() {
        long now = System.currentTimeMillis();

        for (SseEmitter emitter : new ArrayList<>(anonymousEmitters)) {
            try {
                emitter.send(SseEmitter.event().name("ping").data(now));
            } catch (Exception e) {
                anonymousEmitters.remove(emitter);
            }
        }

        for (Map.Entry<Long, List<SseEmitter>> entry : userEmitters.entrySet()) {
            List<SseEmitter> list = entry.getValue();
            for (SseEmitter emitter : new ArrayList<>(list)) {
                try {
                    emitter.send(SseEmitter.event().name("ping").data(now));
                } catch (Exception e) {
                    list.remove(emitter);
                }
            }
        }
    }

    public int getConnectedClientsCount() {
        int userCount = userEmitters.values().stream().mapToInt(List::size).sum();
        return anonymousEmitters.size() + userCount;
    }
}
