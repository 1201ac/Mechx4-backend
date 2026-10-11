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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeatherService {

    private final WeatherLogRepository weatherLogRepository;
    private final DistrictRepository districtRepository;
    private final RiskEngine riskEngine;
    private final SseService sseService;
    private final SystemCacheService systemCacheService;

    public WeatherService(
            WeatherLogRepository weatherLogRepository,
            DistrictRepository districtRepository,
            RiskEngine riskEngine,
            SseService sseService,
            SystemCacheService systemCacheService) {
        this.weatherLogRepository = weatherLogRepository;
        this.districtRepository = districtRepository;
        this.riskEngine = riskEngine;
        this.sseService = sseService;
        this.systemCacheService = systemCacheService;
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
        return systemCacheService.getOrCompute("weather.districts", this::computeDistrictWeatherList);
    }

    private List<DistrictWeatherDto> computeDistrictWeatherList() {
        List<District> districts = districtRepository.findAll();
        List<WeatherLog> allLogs = weatherLogRepository.findAll();

        Map<Long, WeatherLog> latestLogByDistrict = new HashMap<>();
        for (WeatherLog log : allLogs) {
            if (log.getDistrict() != null && log.getDistrict().getId() != null) {
                Long dId = log.getDistrict().getId();
                WeatherLog current = latestLogByDistrict.get(dId);
                if (current == null || (log.getLoggedAt() != null && current.getLoggedAt() != null && log.getLoggedAt().isAfter(current.getLoggedAt()))) {
                    latestLogByDistrict.put(dId, log);
                }
            }
        }

        List<DistrictWeatherDto> list = new ArrayList<>();

        for (District d : districts) {
            WeatherLog w = latestLogByDistrict.get(d.getId());
            if (w != null) {
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

        // Evict cache on weather change
        systemCacheService.evictWeatherRelated();

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
