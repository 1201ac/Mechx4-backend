package com.nerlogix.service.risk;

import com.nerlogix.dto.RiskFactorDto;
import com.nerlogix.entity.District;
import com.nerlogix.entity.RoadSegment;

import java.util.List;

public interface RiskEngine {

    record RiskCalculationResult(
        double score,
        List<RiskFactorDto> factors
    ) {}

    RiskCalculationResult computeSegmentRisk(RoadSegment segment);

    double computeDistrictConnectivity(District district);

    void recomputeAffectedSegmentsForDistrict(Long districtId);

    void recomputeAll();
}
