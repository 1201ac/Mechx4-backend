package com.nerlogix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "districts")
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(name = "population_band", nullable = false)
    private String populationBand;

    @Column(name = "connectivity_score")
    private Double connectivityScore = 100.0;

    private String status = "GOOD";

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public District() {}

    public District(String state, String name, Double lat, Double lng, String populationBand) {
        this.state = state;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.populationBand = populationBand;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public String getPopulationBand() { return populationBand; }
    public void setPopulationBand(String populationBand) { this.populationBand = populationBand; }

    public Double getConnectivityScore() { return connectivityScore; }
    public void setConnectivityScore(Double connectivityScore) { this.connectivityScore = connectivityScore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
