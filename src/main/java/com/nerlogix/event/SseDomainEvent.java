package com.nerlogix.event;

public class SseDomainEvent {
    private final String eventName;
    private final Object data;
    private final Long targetUserId;

    public SseDomainEvent(String eventName, Object data) {
        this(eventName, data, null);
    }

    public SseDomainEvent(String eventName, Object data, Long targetUserId) {
        this.eventName = eventName;
        this.data = data;
        this.targetUserId = targetUserId;
    }

    public String getEventName() { return eventName; }
    public Object getData() { return data; }
    public Long getTargetUserId() { return targetUserId; }
}
