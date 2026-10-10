package com.nerlogix.controller;

import com.nerlogix.dto.UpdateDistrictStockRequest;
import com.nerlogix.entity.District;
import com.nerlogix.entity.DistrictStock;
import com.nerlogix.entity.User;
import com.nerlogix.exception.ResourceNotFoundException;
import com.nerlogix.repository.DistrictRepository;
import com.nerlogix.repository.DistrictStockRepository;
import com.nerlogix.service.AuditService;
import com.nerlogix.service.SseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/districts")
public class DistrictController {

    private final DistrictRepository districtRepository;
    private final DistrictStockRepository districtStockRepository;
    private final AuditService auditService;
    private final SseService sseService;

    public DistrictController(
            DistrictRepository districtRepository,
            DistrictStockRepository districtStockRepository,
            AuditService auditService,
            SseService sseService) {
        this.districtRepository = districtRepository;
        this.districtStockRepository = districtStockRepository;
        this.auditService = auditService;
        this.sseService = sseService;
    }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDistrictStockRequest request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest servletRequest) {

        District district = districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District not found: " + id));

        DistrictStock stock = districtStockRepository.findByDistrictIdAndCategory(id, request.category())
                .orElseGet(() -> new DistrictStock(district, request.category(), 0.0, 1.0));

        stock.setStockUnits(request.stockUnits());
        stock.setDailyDemandUnits(request.dailyDemandUnits());
        stock.setUpdatedAt(LocalDateTime.now());
        districtStockRepository.save(stock);

        auditService.log(
                currentUser,
                "UPDATE_DISTRICT_STOCK",
                "DISTRICT",
                id,
                "Updated stock for " + district.getName() + " (" + request.category() + "): " + request.stockUnits() + " units",
                servletRequest.getRemoteAddr()
        );

        sseService.broadcast("district.updated", Map.of("districtId", id, "action", "stock_updated"));

        double daysCover = stock.getDailyDemandUnits() > 0 ? (stock.getStockUnits() / stock.getDailyDemandUnits()) : 999.0;
        return ResponseEntity.ok(Map.of(
                "id", stock.getId(),
                "districtId", district.getId(),
                "districtName", district.getName(),
                "category", stock.getCategory().name(),
                "stockUnits", stock.getStockUnits(),
                "dailyDemandUnits", stock.getDailyDemandUnits(),
                "daysOfCover", Math.round(daysCover * 10.0) / 10.0,
                "isSupplyGap", daysCover < 3.0
        ));
    }
}
