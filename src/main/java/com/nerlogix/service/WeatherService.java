package com.nerlogix.service;

import com.nerlogix.dto.DistrictWeatherDto;
import com.nerlogix.dto.WeatherLogRequest;
import com.nerlogix.entity.District;
import com.nerlogix.entity.User;
import com.nerlogix.entity.WeatherLog;
import com.nerlogix.exception.ResourceNotFoundException;
import com.nerlogix.model.enums.WeatherCondition;
import com.nerlogix.model.enums.WeatherSource;
import com.nerlogix.repository.DistrictRepository;
import com.nerlogix.repository.WeatherLogRepository;
import com.nerlogix.service.risk.RiskEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WeatherService {

    private final WeatherLogRepository weatherLogRepository;
    private final DistrictRepository districtRepository;
    private final RiskEngine riskEngine;
    private final SseService sseService;

    public WeatherService(
            WeatherLogRepository weatherLogRepository,
            DistrictRepository districtRepository,
            RiskEngine riskEngine,
            SseService sseService) {
        this.weatherLogRepository = weatherLogRepository;
        this.districtRepository = districtRepository;
        this.riskEngine = riskEngine;
        this.sseService = sseService;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DistrictWeatherDto> getWeatherLogs(org.springframework.data.domain.Pageable pageable) {
        return weatherLogRepository.findAllByOrderByLoggedAtDesc(pageable).map(w -> new DistrictWeatherDto(
                w.getDistrict().getId(),
                w.getDistrict().getName(),
                w.getDistrict().getState(),
                w.getCondition(),
                w.getRainMm(),
                w.getNote(),
                w.getSource(),
                w.getLoggedAt()
        ));
    }

    @Transactional(readOnly = true)
    public List<DistrictWeatherDto> getDistrictWeatherList() {
        List<District> districts = districtRepository.findAll();
        List<DistrictWeatherDto> list = new ArrayList<>();

        for (District d : districts) {
            List<WeatherLog> logs = weatherLogRepository.findByDistrictIdOrderByLoggedAtDesc(d.getId());
            if (!logs.isEmpty()) {
                WeatherLog w = logs.get(0);
                list.add(new DistrictWeatherDto(
                        d.getId(),
                        d.getName(),
                        d.getState(),
                        w.getCondition(),
                        w.getRainMm(),
                        w.getNote(),
                        w.getSource(),
                        w.getLoggedAt()
                ));
            } else {
                list.add(new DistrictWeatherDto(
                        d.getId(),
                        d.getName(),
                        d.getState(),
                        WeatherCondition.NONE,
                        0.0,
                        "Normal conditions",
                        WeatherSource.MANUAL,
                        LocalDateTime.now()
                ));
            }
        }

        return list;
    }

    @Transactional
    public DistrictWeatherDto addWeatherLog(WeatherLogRequest req, User loggedBy, WeatherSource source) {
        District district = districtRepository.findById(req.districtId())
                .orElseThrow(() -> new ResourceNotFoundException("District not found with id: " + req.districtId()));

        WeatherLog logEntry = new WeatherLog();
        logEntry.setDistrict(district);
        logEntry.setCondition(req.condition());
        logEntry.setRainMm(req.rainMm());
        logEntry.setNote(req.note());
        logEntry.setSource(source);
        logEntry.setLoggedBy(loggedBy);
        logEntry.setLoggedAt(LocalDateTime.now());

        weatherLogRepository.save(logEntry);

        // Recompute risk for affected road segments touching this district
        riskEngine.recomputeAffectedSegmentsForDistrict(district.getId());

        // Broadcast SSE
        sseService.broadcast("weather.updated", Map.of("districtId", district.getId(), "condition", req.condition().name()));

        return new DistrictWeatherDto(
                district.getId(),
                district.getName(),
                district.getState(),
                logEntry.getCondition(),
                logEntry.getRainMm(),
                logEntry.getNote(),
                logEntry.getSource(),
                logEntry.getLoggedAt()
        );
    }

    @Transactional
    public DistrictWeatherDto triggerDemoRain(Long districtId, WeatherCondition condition) {
        double rainMm = switch (condition) {
            case EXTREME -> 120.0;
            case HEAVY -> 75.0;
            case MODERATE -> 35.0;
            case LIGHT -> 10.0;
            case NONE -> 0.0;
        };

        WeatherLogRequest req = new WeatherLogRequest(
                districtId,
                condition,
                rainMm,
                "Demo simulated rainfall trigger: " + condition.name()
        );

        return addWeatherLog(req, null, WeatherSource.SEED);
    }
}
