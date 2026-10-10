package com.nerlogix.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerlogix.entity.*;
import com.nerlogix.model.enums.*;
import com.nerlogix.repository.*;
import com.nerlogix.util.GeoUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SeedService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;
    private final RoadNodeRepository roadNodeRepository;
    private final RoadSegmentRepository roadSegmentRepository;
    private final RoadStatusHistoryRepository roadStatusHistoryRepository;
    private final WeatherLogRepository weatherLogRepository;
    private final VehicleRepository vehicleRepository;
    private final VehiclePositionRepository vehiclePositionRepository;
    private final ShipmentRepository shipmentRepository;
    private final RoutePlanRepository routePlanRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final FieldReportRepository fieldReportRepository;
    private final ReportImageRepository reportImageRepository;
    private final AlertRepository alertRepository;
    private final NotificationRepository notificationRepository;
    private final DistrictStockRepository districtStockRepository;
    private final RuleConfigRepository ruleConfigRepository;
    private final SettingRepository settingRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public SeedService(
            UserRepository userRepository,
            DistrictRepository districtRepository,
            RoadNodeRepository roadNodeRepository,
            RoadSegmentRepository roadSegmentRepository,
            RoadStatusHistoryRepository roadStatusHistoryRepository,
            WeatherLogRepository weatherLogRepository,
            VehicleRepository vehicleRepository,
            VehiclePositionRepository vehiclePositionRepository,
            ShipmentRepository shipmentRepository,
            RoutePlanRepository routePlanRepository,
            IncidentRepository incidentRepository,
            IncidentEventRepository incidentEventRepository,
            FieldReportRepository fieldReportRepository,
            ReportImageRepository reportImageRepository,
            AlertRepository alertRepository,
            NotificationRepository notificationRepository,
            DistrictStockRepository districtStockRepository,
            RuleConfigRepository ruleConfigRepository,
            SettingRepository settingRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.districtRepository = districtRepository;
        this.roadNodeRepository = roadNodeRepository;
        this.roadSegmentRepository = roadSegmentRepository;
        this.roadStatusHistoryRepository = roadStatusHistoryRepository;
        this.weatherLogRepository = weatherLogRepository;
        this.vehicleRepository = vehicleRepository;
        this.vehiclePositionRepository = vehiclePositionRepository;
        this.shipmentRepository = shipmentRepository;
        this.routePlanRepository = routePlanRepository;
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.fieldReportRepository = fieldReportRepository;
        this.reportImageRepository = reportImageRepository;
        this.alertRepository = alertRepository;
        this.notificationRepository = notificationRepository;
        this.districtStockRepository = districtStockRepository;
        this.ruleConfigRepository = ruleConfigRepository;
        this.settingRepository = settingRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            return; // Idempotent: run only if empty
        }

        Random rand = new Random(42);

        // 1. Seed Rule Config & Settings
        seedRulesAndSettings();

        // 2. Seed Users
        String encodedPass = passwordEncoder.encode("Demo@1234");
        User admin = userRepository.save(new User("admin@nerlogix.com", encodedPass, "Central Admin", Role.ADMIN));
        User moderator = userRepository.save(new User("moderator@nerlogix.com", encodedPass, "Disaster Ops Moderator", Role.MODERATOR));
        User mainUser = userRepository.save(new User("user@nerlogix.com", encodedPass, "Field Driver User", Role.USER));

        List<User> extraUsers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            extraUsers.add(userRepository.save(new User("driver" + i + "@nerlogix.com", encodedPass, "Driver " + i, Role.USER)));
        }

        // 3. Seed Districts (≥ 32 across 8 states)
        Map<String, List<String>> stateDistricts = new LinkedHashMap<>();
        stateDistricts.put("Assam", List.of("Kamrup Metropolitan", "Nagaon", "Cachar", "Dibrugarh", "Jorhat", "Tinsukia", "Bongaigaon"));
        stateDistricts.put("Meghalaya", List.of("East Khasi Hills", "West Garo Hills", "West Jaintia Hills", "Ri-Bhoi"));
        stateDistricts.put("Tripura", List.of("West Tripura", "Unakoti", "Gomati", "South Tripura"));
        stateDistricts.put("Mizoram", List.of("Aizawl", "Lunglei", "Champhai", "Kolasib"));
        stateDistricts.put("Manipur", List.of("Imphal East", "Churachandpur", "Senapati", "Thoubal"));
        stateDistricts.put("Nagaland", List.of("Kohima", "Dimapur", "Mokokchung", "Mon"));
        stateDistricts.put("Arunachal Pradesh", List.of("Papum Pare", "Lower Subansiri", "East Siang", "West Kameng", "Tawang"));
        stateDistricts.put("Sikkim", List.of("Gangtok", "Namchi", "Gyalshing", "Mangan"));

        Map<String, District> districtMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : stateDistricts.entrySet()) {
            String state = entry.getKey();
            for (String dName : entry.getValue()) {
                double baseLat = 24.0 + rand.nextDouble() * 4.0;
                double baseLng = 88.5 + rand.nextDouble() * 7.0;
                District d = new District(state, dName, baseLat, baseLng, "100k-500k");
                d.setConnectivityScore(80.0 + rand.nextDouble() * 20.0);
                d = districtRepository.save(d);
                districtMap.put(dName, d);

                // Stock categories (about 20% cells below 3 days of cover)
                for (CargoCategory cat : CargoCategory.values()) {
                    double demand = 100.0 + rand.nextInt(200);
                    double stock = (rand.nextDouble() < 0.2) ? demand * (1.0 + rand.nextDouble() * 1.5) : demand * (4.0 + rand.nextDouble() * 6.0);
                    districtStockRepository.save(new DistrictStock(d, cat, stock, demand));
                }
            }
        }

        // 4. Seed Road Nodes (Coordinates from Section 4.2)
        Map<String, double[]> nodeCoords = new LinkedHashMap<>();
        nodeCoords.put("Guwahati", new double[]{26.1445, 91.7362});
        nodeCoords.put("Shillong", new double[]{25.5788, 91.8933});
        nodeCoords.put("Tura", new double[]{25.5142, 90.2024});
        nodeCoords.put("Jowai", new double[]{25.4333, 92.2000});
        nodeCoords.put("Nagaon", new double[]{26.3500, 92.6833});
        nodeCoords.put("Bongaigaon", new double[]{26.4780, 90.5580});
        nodeCoords.put("Tezpur", new double[]{26.6528, 92.7926});
        nodeCoords.put("Jorhat", new double[]{26.7509, 94.2037});
        nodeCoords.put("Sivasagar", new double[]{26.9833, 94.6333});
        nodeCoords.put("Dibrugarh", new double[]{27.4728, 94.9120});
        nodeCoords.put("Tinsukia", new double[]{27.4922, 95.3558});
        nodeCoords.put("Lumding", new double[]{25.7500, 93.1667});
        nodeCoords.put("Haflong", new double[]{25.1667, 93.0167});
        nodeCoords.put("Silchar", new double[]{24.8333, 92.7789});
        nodeCoords.put("Kailashahar", new double[]{24.3333, 92.0000});
        nodeCoords.put("Agartala", new double[]{23.8315, 91.2868});
        nodeCoords.put("Udaipur-Tripura", new double[]{23.5333, 91.4833});
        nodeCoords.put("Aizawl", new double[]{23.7271, 92.7176});
        nodeCoords.put("Lunglei", new double[]{22.8870, 92.7330});
        nodeCoords.put("Imphal", new double[]{24.8170, 93.9368});
        nodeCoords.put("Churachandpur", new double[]{24.3333, 93.6833});
        nodeCoords.put("Dimapur", new double[]{25.9063, 93.7276});
        nodeCoords.put("Kohima", new double[]{25.6751, 94.1086});
        nodeCoords.put("Mokokchung", new double[]{26.3220, 94.5150});
        nodeCoords.put("Mon", new double[]{26.7333, 95.0333});
        nodeCoords.put("Itanagar", new double[]{27.0844, 93.6053});
        nodeCoords.put("Ziro", new double[]{27.5450, 93.8300});
        nodeCoords.put("Pasighat", new double[]{28.0660, 95.3260});
        nodeCoords.put("Along", new double[]{28.1667, 94.8000});
        nodeCoords.put("Bomdila", new double[]{27.2645, 92.4157});
        nodeCoords.put("Tawang", new double[]{27.5860, 91.8594});
        nodeCoords.put("Siliguri", new double[]{26.7271, 88.3953});
        nodeCoords.put("Gangtok", new double[]{27.3314, 88.6138});
        nodeCoords.put("Namchi", new double[]{27.1667, 88.3500});
        nodeCoords.put("Mangan", new double[]{27.5167, 88.5333});

        Map<String, RoadNode> nodeMap = new HashMap<>();
        District defaultDistrict = districtRepository.findAll().get(0);
        for (Map.Entry<String, double[]> entry : nodeCoords.entrySet()) {
            String name = entry.getKey();
            double[] c = entry.getValue();
            District d = districtRepository.findByName(name).orElse(defaultDistrict);
            RoadNode node = roadNodeRepository.save(new RoadNode(name, c[0], c[1], d, NodeKind.CITY));
            nodeMap.put(name, node);
        }

        // Add 3-5 hospital/depot nodes per state
        String[] specialNodes = {
            "Guwahati Central Depot", "Shillong Civil Hospital", "Imphal Regional Depot",
            "Gangtok STNM Hospital", "Agartala Medical Depot", "Aizawl Civil Hospital",
            "Itanagar General Hospital", "Dimapur Logistics Hub"
        };
        for (String sName : specialNodes) {
            String cityRef = sName.split(" ")[0];
            RoadNode refNode = nodeMap.getOrDefault(cityRef, nodeMap.get("Guwahati"));
            NodeKind kind = sName.contains("Hospital") ? NodeKind.HOSPITAL : NodeKind.DEPOT;
            RoadNode node = roadNodeRepository.save(new RoadNode(sName, refNode.getLat() + 0.02, refNode.getLng() + 0.02, refNode.getDistrict(), kind));
            nodeMap.put(sName, node);
        }

        // 5. Seed Road Segments (≥ 60 segments, ≥ 6 BRIDGE, ≥ 6 PASS)
        List<RoadSegmentDef> segmentDefs = createSegmentDefinitions();
        List<RoadSegment> allSegments = new ArrayList<>();
        int segmentCount = 0;
        for (RoadSegmentDef def : segmentDefs) {
            RoadNode from = nodeMap.get(def.from);
            RoadNode to = nodeMap.get(def.to);
            if (from == null || to == null) continue;

            double dist = GeoUtils.haversineDistanceKm(from.getLat(), from.getLng(), to.getLat(), to.getLng());
            RoadSegment seg = new RoadSegment();
            seg.setFromNode(from);
            seg.setToNode(to);
            seg.setName(def.name != null ? def.name : (from.getName() + " - " + to.getName() + " Corridor"));
            seg.setHighwayCode(def.highwayCode);
            seg.setAssetType(def.assetType);
            seg.setLengthKm(Math.max(5.0, Math.round(dist * 1.1 * 10.0) / 10.0));
            seg.setBaseSpeedKmph(def.assetType == AssetType.PASS ? 30.0 : (def.assetType == AssetType.BRIDGE ? 40.0 : 50.0));
            seg.setTerrainClass(def.assetType == AssetType.PASS ? 5 : (def.assetType == AssetType.BRIDGE ? 3 : 2));
            seg.setLandslideProne(def.assetType == AssetType.PASS || rand.nextBoolean());
            seg.setFloodProne(def.assetType == AssetType.BRIDGE || rand.nextBoolean());
            seg.setStatus(SegmentStatus.OPEN);
            seg.setRiskScore(10.0 + rand.nextDouble() * 30.0);
            seg.setPolyline(generatePolyline(from, to, rand));

            allSegments.add(seg);
            segmentCount++;
        }

        // Ensure 1 BLOCKED and 3 RESTRICTED
        if (allSegments.size() > 5) {
            allSegments.get(2).setStatus(SegmentStatus.BLOCKED);
            allSegments.get(2).setStatusReason("Major landslide reported near corridor");
            allSegments.get(4).setStatus(SegmentStatus.RESTRICTED);
            allSegments.get(4).setStatusReason("Heavy rainfall causing waterlogging");
            allSegments.get(6).setStatus(SegmentStatus.RESTRICTED);
            allSegments.get(8).setStatus(SegmentStatus.RESTRICTED);
        }

        for (RoadSegment seg : allSegments) {
            roadSegmentRepository.save(seg);
        }

        // 6. Seed Weather Logs for every district
        for (District d : districtRepository.findAll()) {
            WeatherCondition cond = (rand.nextDouble() < 0.2) ? WeatherCondition.HEAVY : WeatherCondition.LIGHT;
            double rain = cond == WeatherCondition.HEAVY ? 110.0 : 15.0;
            weatherLogRepository.save(new WeatherLog(d, cond, rain, "Monsoon field log", WeatherSource.SEED, admin));
        }

        // 7. Seed 30 Vehicles
        List<Vehicle> vehicles = new ArrayList<>();
        VehicleType[] vTypes = VehicleType.values();
        for (int i = 1; i <= 30; i++) {
            RoadNode startNode = nodeMap.get("Guwahati");
            Vehicle v = new Vehicle("AS-01-C-" + (1000 + i), vTypes[i % vTypes.length], 2000.0 + i * 500.0, startNode.getLat(), startNode.getLng());
            v.setSpeedKmph(45.0);
            vehicles.add(vehicleRepository.save(v));
        }

        // 8. Seed 12 Shipments (4 IN_TRANSIT, 2 DELAYED, 3 ASSIGNED, 3 DELIVERED)
        RoadNode orig = nodeMap.get("Guwahati");
        RoadNode dest = nodeMap.get("Imphal");

        // Shipment 1: IN_TRANSIT with driver mainUser
        Shipment s1 = createShipment("SHP-1001", CargoCategory.MEDICINE, "Essential Vaccines", 2500.0, ShipmentPriority.HIGH, orig, dest, vehicles.get(0), mainUser, ShipmentStatus.IN_TRANSIT);
        shipmentRepository.save(s1);
        vehicles.get(0).setStatus(VehicleStatus.ON_TRIP);
        vehicleRepository.save(vehicles.get(0));
        createRoutePlanForShipment(s1, allSegments.subList(0, 5), RoutePlanStatus.ACTIVE);

        // Shipment 2..4: IN_TRANSIT
        for (int i = 2; i <= 4; i++) {
            Shipment s = createShipment("SHP-100" + i, CargoCategory.FOOD, "Ration Supplies", 3000.0, ShipmentPriority.NORMAL, orig, dest, vehicles.get(i), extraUsers.get(i % extraUsers.size()), ShipmentStatus.IN_TRANSIT);
            shipmentRepository.save(s);
            vehicles.get(i).setStatus(VehicleStatus.ON_TRIP);
            vehicleRepository.save(vehicles.get(i));
            createRoutePlanForShipment(s, allSegments.subList(0, 5), RoutePlanStatus.ACTIVE);
        }

        // Shipment 5..6: DELAYED
        for (int i = 5; i <= 6; i++) {
            Shipment s = createShipment("SHP-100" + i, CargoCategory.CONSTRUCTION, "Bridge Repairs Material", 4000.0, ShipmentPriority.EMERGENCY, orig, dest, vehicles.get(i), extraUsers.get(i % extraUsers.size()), ShipmentStatus.DELAYED);
            s.setDelayMin(45);
            shipmentRepository.save(s);
        }

        // Shipment 7..9: ASSIGNED
        for (int i = 7; i <= 9; i++) {
            Shipment s = createShipment("SHP-100" + i, CargoCategory.AGRI, "Fertilizers", 1500.0, ShipmentPriority.NORMAL, orig, dest, vehicles.get(i), extraUsers.get(i % extraUsers.size()), ShipmentStatus.ASSIGNED);
            shipmentRepository.save(s);
        }

        // Shipment 10..12: DELIVERED
        for (int i = 10; i <= 12; i++) {
            Shipment s = createShipment("SHP-101" + (i - 10), CargoCategory.MEDICINE, "First Aid Kits", 1200.0, ShipmentPriority.NORMAL, orig, dest, vehicles.get(i), extraUsers.get(i % extraUsers.size()), ShipmentStatus.DELIVERED);
            s.setDeliveredAt(LocalDateTime.now().minusHours(2));
            shipmentRepository.save(s);
        }

        // 9. Seed 15 Incidents with History Events
        RoadSegment targetSeg = allSegments.get(2);
        for (int i = 1; i <= 15; i++) {
            Incident inc = new Incident();
            inc.setType(i % 2 == 0 ? IncidentType.LANDSLIDE : IncidentType.FLOOD);
            inc.setSeverity((i % 4) + 1);
            inc.setLat(targetSeg.getFromNode().getLat());
            inc.setLng(targetSeg.getFromNode().getLng());
            inc.setSegment(targetSeg);
            inc.setDistrict(targetSeg.getFromNode().getDistrict());
            inc.setDescription("Road blockage due to heavy rainfall and terrain collapse");
            inc.setStatus(i <= 3 ? IncidentStatus.VERIFIED : IncidentStatus.REPORTED);
            inc.setSource(IncidentSource.FIELD);
            inc.setCreatedBy(mainUser);
            inc = incidentRepository.save(inc);

            incidentEventRepository.save(new IncidentEvent(inc, "REPORTED", mainUser, "Incident logged from field app"));
        }

        // 10. Seed 10 Field Reports with generated sample PNG images
        File uploadFolder = new File("./uploads");
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
        }

        for (int i = 1; i <= 10; i++) {
            FieldReport fr = new FieldReport();
            fr.setClientId("CLIENT-UUID-" + i);
            fr.setUser(mainUser);
            fr.setLat(26.1 + i * 0.05);
            fr.setLng(91.7 + i * 0.05);
            fr.setAccuracy(5.0);
            fr.setCapturedAt(LocalDateTime.now().minusHours(i));
            fr.setType(IncidentType.LANDSLIDE);
            fr.setSeverity(3);
            fr.setNote("Debris falling on road highway segment " + i);
            fr.setStatus(ReportStatus.PENDING);
            fr = fieldReportRepository.save(fr);

            // Generate small PNG sample image
            String imgFileName = "sample_report_" + i + ".png";
            File imgFile = new File(uploadFolder, imgFileName);
            generateSamplePng(imgFile);
            reportImageRepository.save(new ReportImage(fr, "/api/uploads/" + imgFileName));
        }

        // 11. Seed 10 Alerts
        Map<String, String> titleMap = Map.of(
            "en", "NH-27 Blocked Near Nagaon",
            "hi", "नगांव के पास एनएच-27 बंद है",
            "as", "নগাঁওৰ ওচৰত NH-27 বন্ধ হৈ পৰিছে",
            "bn", "নগাঁওয়ে NH-27 অবরুদ্ধ"
        );
        Map<String, String> msgMap = Map.of(
            "en", "Landslide reported near Nagaon. Diversion active.",
            "hi", "नगांव के पास भूस्खलन की सूचना। मार्ग परिवर्तन सक्रिय।",
            "as", "নগাঁওৰ ওচৰত ভূমিস্খলন। বিকল্প পথ সক্রিয়।",
            "bn", "নগাঁওয়ে ভূমিধসের খবর। বিকল্প রুট সক্রিয়।"
        );
        String titleJson = objectMapper.writeValueAsString(titleMap);
        String msgJson = objectMapper.writeValueAsString(msgMap);

        for (int i = 1; i <= 10; i++) {
            Alert alert = new Alert();
            alert.setType(i == 1 ? AlertType.ROAD_BLOCKED : AlertType.HIGH_RISK_CORRIDOR);
            alert.setSeverity(i == 1 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING);
            alert.setEntityType("ROAD_SEGMENT");
            alert.setEntityId(allSegments.get(i).getId());
            alert.setOpenKey("SEGMENT_BLOCKED_" + allSegments.get(i).getId());
            alert.setStatus(AlertStatus.OPEN);
            alert.setTitle(titleJson);
            alert.setMessage(msgJson);
            alert = alertRepository.save(alert);

            notificationRepository.save(new Notification(mainUser, alert, titleJson, msgJson));
            notificationRepository.save(new Notification(moderator, alert, titleJson, msgJson));
        }
    }

    private void seedRulesAndSettings() {
        // RuleConfig defaults are populated by RuleConfigService.initDefaults()
        saveSettingIfAbsent("emergency_mode", "false");
        saveSettingIfAbsent("weather.autoFetch", "true");
        saveSettingIfAbsent("simulation.enabled", "true");
    }

    private void saveSettingIfAbsent(String key, String value) {
        if (settingRepository.findBySettingKey(key).isEmpty()) {
            settingRepository.save(new Setting(key, value));
        }
    }

    private Shipment createShipment(String code, CargoCategory cat, String desc, double weight, ShipmentPriority prio, RoadNode orig, RoadNode dest, Vehicle v, User driver, ShipmentStatus status) {
        Shipment s = new Shipment();
        s.setCode(code);
        s.setCargoCategory(cat);
        s.setCargoDescription(desc);
        s.setWeightKg(weight);
        s.setPriority(prio);
        s.setOriginNode(orig);
        s.setDestinationNode(dest);
        s.setVehicle(v);
        s.setDriverUser(driver);
        s.setStatus(status);
        s.setPlannedEta(LocalDateTime.now().plusHours(6));
        s.setCurrentEta(LocalDateTime.now().plusHours(6));
        s.setProgressPct(status == ShipmentStatus.DELIVERED ? 100.0 : (status == ShipmentStatus.IN_TRANSIT ? 25.0 : 0.0));
        return s;
    }

    private void createRoutePlanForShipment(Shipment s, List<RoadSegment> segs, RoutePlanStatus status) {
        RoutePlan rp = new RoutePlan();
        rp.setShipment(s);
        List<Long> ids = segs.stream().map(RoadSegment::getId).toList();
        try {
            rp.setSegmentIds(objectMapper.writeValueAsString(ids));
        } catch (Exception e) {}
        rp.setDistanceKm(150.0);
        rp.setEtaMin(180.0);
        rp.setRiskScore(25.0);
        rp.setStatus(status);
        rp.setGeometry(generateMultiNodeGeometry(segs));
        routePlanRepository.save(rp);
    }

    private String generatePolyline(RoadNode from, RoadNode to, Random rand) {
        List<double[]> points = new ArrayList<>();
        int numPoints = 6;
        for (int i = 0; i <= numPoints; i++) {
            double ratio = (double) i / numPoints;
            double lat = from.getLat() + (to.getLat() - from.getLat()) * ratio + (rand.nextDouble() - 0.5) * 0.005;
            double lng = from.getLng() + (to.getLng() - from.getLng()) * ratio + (rand.nextDouble() - 0.5) * 0.005;
            points.add(new double[]{lat, lng});
        }
        try {
            return objectMapper.writeValueAsString(points);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String generateMultiNodeGeometry(List<RoadSegment> segs) {
        List<double[]> pts = new ArrayList<>();
        for (RoadSegment s : segs) {
            pts.add(new double[]{s.getFromNode().getLat(), s.getFromNode().getLng()});
            pts.add(new double[]{s.getToNode().getLat(), s.getToNode().getLng()});
        }
        try {
            return objectMapper.writeValueAsString(pts);
        } catch (Exception e) {
            return "[]";
        }
    }

    private void generateSamplePng(File file) {
        try {
            BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(20, 89, 122));
            g.fillRect(0, 0, 100, 100);
            g.setColor(Color.WHITE);
            g.drawString("Field Report", 15, 55);
            g.dispose();
            ImageIO.write(img, "png", file);
        } catch (Exception ignored) {}
    }

    private static class RoadSegmentDef {
        String from;
        String to;
        String highwayCode;
        AssetType assetType;
        String name;

        RoadSegmentDef(String from, String to, String highwayCode, AssetType assetType, String name) {
            this.from = from;
            this.to = to;
            this.highwayCode = highwayCode;
            this.assetType = assetType;
            this.name = name;
        }
    }

    private List<RoadSegmentDef> createSegmentDefinitions() {
        List<RoadSegmentDef> list = new ArrayList<>();

        // Siliguri–Bongaigaon–Guwahati
        list.add(new RoadSegmentDef("Siliguri", "Bongaigaon", "NH-27", AssetType.ROAD, "Siliguri-Bongaigaon Highway"));
        list.add(new RoadSegmentDef("Bongaigaon", "Guwahati", "NH-27", AssetType.BRIDGE, "Saraighat Bridge Corridor"));

        // Path A: Guwahati–Nagaon–Lumding–Dimapur–Kohima–Imphal
        list.add(new RoadSegmentDef("Guwahati", "Nagaon", "NH-27", AssetType.ROAD, "Guwahati-Nagaon Highway"));
        list.add(new RoadSegmentDef("Nagaon", "Lumding", "NH-27", AssetType.ROAD, "Nagaon-Lumding Section"));
        list.add(new RoadSegmentDef("Lumding", "Dimapur", "NH-29", AssetType.PASS, "Lumding-Dimapur Hill Pass"));
        list.add(new RoadSegmentDef("Dimapur", "Kohima", "NH-29", AssetType.PASS, "Dimapur-Kohima Ghat"));
        list.add(new RoadSegmentDef("Kohima", "Imphal", "NH-2", AssetType.ROAD, "Kohima-Imphal Highway"));

        // Path B: Guwahati–Shillong–Jowai–Haflong–Silchar–Churachandpur–Imphal
        list.add(new RoadSegmentDef("Guwahati", "Shillong", "NH-6", AssetType.PASS, "Umiam Gorge Pass"));
        list.add(new RoadSegmentDef("Shillong", "Jowai", "NH-6", AssetType.ROAD, "Shillong-Jowai Bypass"));
        list.add(new RoadSegmentDef("Jowai", "Haflong", "NH-27", AssetType.PASS, "Jowai-Haflong Ridge"));
        list.add(new RoadSegmentDef("Haflong", "Silchar", "NH-27", AssetType.BRIDGE, "Haflong Valley Bridge"));
        list.add(new RoadSegmentDef("Silchar", "Churachandpur", "NH-37", AssetType.PASS, "Barak Valley Hills"));
        list.add(new RoadSegmentDef("Churachandpur", "Imphal", "NH-128", AssetType.ROAD, "Churachandpur-Imphal Highway"));

        // Guwahati–Nagaon–Jorhat–Sivasagar–Dibrugarh–Tinsukia
        list.add(new RoadSegmentDef("Nagaon", "Jorhat", "NH-715", AssetType.ROAD, "Nagaon-Jorhat Segment"));
        list.add(new RoadSegmentDef("Jorhat", "Sivasagar", "NH-715", AssetType.ROAD, "Jorhat-Sivasagar Highway"));
        list.add(new RoadSegmentDef("Sivasagar", "Dibrugarh", "NH-15", AssetType.BRIDGE, "Dibru River Bridge"));
        list.add(new RoadSegmentDef("Dibrugarh", "Tinsukia", "NH-15", AssetType.ROAD, "Dibrugarh-Tinsukia Link"));

        // Silchar–Aizawl & Aizawl–Lunglei
        list.add(new RoadSegmentDef("Silchar", "Aizawl", "NH-306", AssetType.PASS, "Silchar-Aizawl Pass"));
        list.add(new RoadSegmentDef("Aizawl", "Lunglei", "NH-2", AssetType.ROAD, "Aizawl-Lunglei Corridor"));

        // Silchar–Kailashahar–Agartala–Udaipur
        list.add(new RoadSegmentDef("Silchar", "Kailashahar", "NH-8", AssetType.ROAD, "Silchar-Kailashahar Highway"));
        list.add(new RoadSegmentDef("Kailashahar", "Agartala", "NH-8", AssetType.ROAD, "Kailashahar-Agartala Highway"));
        list.add(new RoadSegmentDef("Agartala", "Udaipur-Tripura", "NH-8", AssetType.ROAD, "Agartala-Udaipur Highway"));

        // Haflong–Lumding
        list.add(new RoadSegmentDef("Haflong", "Lumding", "NH-27", AssetType.PASS, "Haflong-Lumding Hill Road"));

        // Dimapur–Mokokchung–Mon
        list.add(new RoadSegmentDef("Dimapur", "Mokokchung", "NH-702", AssetType.PASS, "Dimapur-Mokokchung Hills"));
        list.add(new RoadSegmentDef("Mokokchung", "Mon", "NH-702", AssetType.PASS, "Mokokchung-Mon Ridge"));

        // Guwahati–Tezpur–Bomdila–Tawang
        list.add(new RoadSegmentDef("Guwahati", "Tezpur", "NH-15", AssetType.BRIDGE, "Kalia Bhomora Bridge"));
        list.add(new RoadSegmentDef("Tezpur", "Bomdila", "NH-13", AssetType.PASS, "Bomdila High Pass"));
        list.add(new RoadSegmentDef("Bomdila", "Tawang", "NH-13", AssetType.PASS, "Sela Pass Summit"));

        // Tezpur–Itanagar–Ziro
        list.add(new RoadSegmentDef("Tezpur", "Itanagar", "NH-415", AssetType.ROAD, "Tezpur-Itanagar Highway"));
        list.add(new RoadSegmentDef("Itanagar", "Ziro", "NH-13", AssetType.PASS, "Ziro Valley Ascent"));

        // Jorhat–Pasighat–Along
        list.add(new RoadSegmentDef("Jorhat", "Pasighat", "NH-515", AssetType.BRIDGE, "Bogibeel Bridge Corridor"));
        list.add(new RoadSegmentDef("Pasighat", "Along", "NH-13", AssetType.PASS, "Siang River Pass"));

        // Siliguri–Gangtok–Mangan & Siliguri–Namchi
        list.add(new RoadSegmentDef("Siliguri", "Gangtok", "NH-10", AssetType.ROAD, "Teesta Valley Highway"));
        list.add(new RoadSegmentDef("Gangtok", "Mangan", "NH-10", AssetType.PASS, "North Sikkim Highway"));
        list.add(new RoadSegmentDef("Siliguri", "Namchi", "NH-710", AssetType.ROAD, "Namchi Route"));

        // Guwahati–Tura
        list.add(new RoadSegmentDef("Guwahati", "Tura", "NH-217", AssetType.ROAD, "Guwahati-Tura Link"));

        // Add 25+ more interconnected cross-corridor segments to reach ≥ 60 segments total
        list.add(new RoadSegmentDef("Bongaigaon", "Tezpur", "NH-127", AssetType.ROAD, "North Assam Corridor"));
        list.add(new RoadSegmentDef("Shillong", "Tura", "NH-217", AssetType.PASS, "Garo Hills Highway"));
        list.add(new RoadSegmentDef("Nagaon", "Tezpur", "NH-715", AssetType.BRIDGE, "Kalia Bhomora Link"));
        list.add(new RoadSegmentDef("Jowai", "Silchar", "NH-6", AssetType.PASS, "Meghalaya South Ridge"));
        list.add(new RoadSegmentDef("Kailashahar", "Aizawl", "NH-108", AssetType.PASS, "Tripura-Mizoram Border Pass"));
        list.add(new RoadSegmentDef("Agartala", "Aizawl", "NH-108", AssetType.PASS, "Agartala-Aizawl Link"));
        list.add(new RoadSegmentDef("Churachandpur", "Aizawl", "NH-102B", AssetType.PASS, "Manipur-Mizoram Border Pass"));
        list.add(new RoadSegmentDef("Kohima", "Mokokchung", "NH-2", AssetType.PASS, "Nagaland Central Spine"));
        list.add(new RoadSegmentDef("Mon", "Along", "NH-202", AssetType.BRIDGE, "Arunachal-Nagaland River Crossing"));
        list.add(new RoadSegmentDef("Itanagar", "Pasighat", "NH-13", AssetType.ROAD, "Trans-Arunachal Highway"));
        list.add(new RoadSegmentDef("Ziro", "Along", "NH-13", AssetType.PASS, "Subansiri-Siang Link"));
        list.add(new RoadSegmentDef("Bomdila", "Itanagar", "NH-13", AssetType.PASS, "Kameng-Papum Link"));
        list.add(new RoadSegmentDef("Gangtok", "Namchi", "NH-710", AssetType.ROAD, "Sikkim Central Road"));
        list.add(new RoadSegmentDef("Namchi", "Mangan", "NH-10", AssetType.PASS, "South-North Sikkim Bypass"));

        // Special hospital/depot connection segments
        list.add(new RoadSegmentDef("Guwahati", "Guwahati Central Depot", "LOCAL", AssetType.ROAD, "Guwahati Depot Access"));
        list.add(new RoadSegmentDef("Shillong", "Shillong Civil Hospital", "LOCAL", AssetType.ROAD, "Shillong Hospital Access"));
        list.add(new RoadSegmentDef("Imphal", "Imphal Regional Depot", "LOCAL", AssetType.ROAD, "Imphal Depot Access"));
        list.add(new RoadSegmentDef("Gangtok", "Gangtok STNM Hospital", "LOCAL", AssetType.ROAD, "Gangtok Hospital Access"));
        list.add(new RoadSegmentDef("Agartala", "Agartala Medical Depot", "LOCAL", AssetType.ROAD, "Agartala Depot Access"));
        list.add(new RoadSegmentDef("Aizawl", "Aizawl Civil Hospital", "LOCAL", AssetType.ROAD, "Aizawl Hospital Access"));
        list.add(new RoadSegmentDef("Itanagar", "Itanagar General Hospital", "LOCAL", AssetType.ROAD, "Itanagar Hospital Access"));
        list.add(new RoadSegmentDef("Dimapur", "Dimapur Logistics Hub", "LOCAL", AssetType.ROAD, "Dimapur Hub Access"));

        // Additional cross-corridor links to ensure >= 60 segments
        list.add(new RoadSegmentDef("Bongaigaon", "Guwahati", "NH-127B", AssetType.ROAD, "Bongaigaon South Link"));
        list.add(new RoadSegmentDef("Shillong", "Haflong", "NH-6", AssetType.PASS, "Shillong-Haflong Direct Ridge"));
        list.add(new RoadSegmentDef("Tinsukia", "Pasighat", "NH-515", AssetType.BRIDGE, "Tinsukia-Pasighat Bridge Link"));
        list.add(new RoadSegmentDef("Dimapur", "Haflong", "NH-27", AssetType.PASS, "Dimapur-Haflong Pass"));
        list.add(new RoadSegmentDef("Kohima", "Churachandpur", "NH-102A", AssetType.PASS, "Kohima-Churachandpur Ridge"));

        return list;
    }
}
