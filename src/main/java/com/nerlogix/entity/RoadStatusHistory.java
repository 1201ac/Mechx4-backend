package com.nerlogix.entity;

import com.nerlogix.model.enums.SegmentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "road_status_history")
public class RoadStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "segment_id", nullable = false)
    private RoadSegment segment;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private SegmentStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private SegmentStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    public RoadStatusHistory() {}

    public RoadStatusHistory(RoadSegment segment, SegmentStatus oldStatus, SegmentStatus newStatus, String reason, User changedBy) {
        this.segment = segment;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedBy = changedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RoadSegment getSegment() { return segment; }
    public void setSegment(RoadSegment segment) { this.segment = segment; }

    public SegmentStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(SegmentStatus oldStatus) { this.oldStatus = oldStatus; }

    public SegmentStatus getNewStatus() { return newStatus; }
    public void setNewStatus(SegmentStatus newStatus) { this.newStatus = newStatus; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
