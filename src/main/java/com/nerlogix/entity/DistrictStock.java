package com.nerlogix.entity;

import com.nerlogix.model.enums.CargoCategory;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "district_stock", uniqueConstraints = {
    @UniqueConstraint(name = "uk_district_stock", columnNames = {"district_id", "category"})
})
public class DistrictStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CargoCategory category;

    @Column(name = "stock_units", nullable = false)
    private Double stockUnits = 0.0;

    @Column(name = "daily_demand_units", nullable = false)
    private Double dailyDemandUnits = 1.0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public DistrictStock() {}

    public DistrictStock(District district, CargoCategory category, Double stockUnits, Double dailyDemandUnits) {
        this.district = district;
        this.category = category;
        this.stockUnits = stockUnits;
        this.dailyDemandUnits = dailyDemandUnits;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public District getDistrict() { return district; }
    public void setDistrict(District district) { this.district = district; }

    public CargoCategory getCategory() { return category; }
    public void setCategory(CargoCategory category) { this.category = category; }

    public Double getStockUnits() { return stockUnits; }
    public void setStockUnits(Double stockUnits) { this.stockUnits = stockUnits; }

    public Double getDailyDemandUnits() { return dailyDemandUnits; }
    public void setDailyDemandUnits(Double dailyDemandUnits) { this.dailyDemandUnits = dailyDemandUnits; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
