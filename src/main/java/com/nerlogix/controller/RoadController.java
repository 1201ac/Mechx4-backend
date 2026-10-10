package com.nerlogix.controller;

import com.nerlogix.dto.PageResponse;
import com.nerlogix.dto.RoadSegmentDetailDto;
import com.nerlogix.dto.RoadSegmentDto;
import com.nerlogix.dto.RoadStatusUpdateRequest;
import com.nerlogix.entity.User;
import com.nerlogix.model.enums.AssetType;
import com.nerlogix.model.enums.SegmentStatus;
import com.nerlogix.service.RoadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roads")
public class RoadController {

    private final RoadService roadService;

    public RoadController(RoadService roadService) {
        this.roadService = roadService;
    }

    @GetMapping
    public PageResponse<RoadSegmentDto> getRoads(
            @RequestParam(required = false) SegmentStatus status,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) AssetType assetType,
            @RequestParam(required = false) String risk,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        return PageResponse.from(roadService.findRoads(status, state, assetType, risk, q, pageable));
    }

    @GetMapping("/{id}")
    public RoadSegmentDetailDto getRoadDetail(@PathVariable Long id) {
        return roadService.getRoadDetail(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public RoadSegmentDto updateRoadStatus(
            @PathVariable Long id,
            @Valid @RequestBody RoadStatusUpdateRequest request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        return roadService.updateRoadStatus(id, request.status(), request.reason(), currentUser, ipAddress);
    }
}
