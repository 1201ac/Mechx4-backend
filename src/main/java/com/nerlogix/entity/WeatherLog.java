package com.nerlogix.entity;

import com.nerlogix.model.enums.WeatherCondition;
import com.nerlogix.model.enums.WeatherSource;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_logs")
public class WeatherLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeatherCondition condition;

    @Column(name = "rain_mm")
    private Double rainMm = 0.0;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeatherSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_by")
    private User loggedBy;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt = LocalDateTime.now();

    public WeatherLog() {}

    public WeatherLog(District district, WeatherCondition condition, Double rainMm, String note, WeatherSource source, User loggedBy) {
        this.district = district;
        this.condition = condition;
        this.rainMm = rainMm;
        this.note = note;
        this.source = source;
        this.loggedBy = loggedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public District getDistrict() { return district; }
    public void setDistrict(District district) { this.district = district; }

    public WeatherCondition getCondition() { return condition; }
    public void setCondition(WeatherCondition condition) { this.condition = condition; }

    public Double getRainMm() { return rainMm; }
    public void setRainMm(Double rainMm) { this.rainMm = rainMm; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public WeatherSource getSource() { return source; }
    public void setSource(WeatherSource source) { this.source = source; }

    public User getLoggedBy() { return loggedBy; }
    public void setLoggedBy(User loggedBy) { this.loggedBy = loggedBy; }

    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}
