package com.nerlogix.controller;

import com.nerlogix.dto.RouteOptionDto;
import com.nerlogix.dto.RoutePlanRequest;
import com.nerlogix.service.routing.RoutePlannerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RoutePlannerService routePlannerService;

    public RouteController(RoutePlannerService routePlannerService) {
        this.routePlannerService = routePlannerService;
    }

    @PostMapping("/plan")
    public ResponseEntity<List<RouteOptionDto>> planRoutes(@Valid @RequestBody RoutePlanRequest request) {
        List<RouteOptionDto> options = routePlannerService.planRoutes(request);
        return ResponseEntity.ok(options);
    }
}
