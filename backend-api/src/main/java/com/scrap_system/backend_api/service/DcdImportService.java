package com.scrap_system.backend_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrap_system.backend_api.dto.BatchUpsertResult;
import com.scrap_system.backend_api.model.ExternalVehicleTrim;
import com.scrap_system.backend_api.repository.ExternalVehicleTrimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DcdImportService {

    private final ExternalVehicleTrimRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Transactional
    public BatchUpsertResult importSeries(List<Long> seriesIds, String cityName) {
        if (seriesIds == null || seriesIds.isEmpty()) return new BatchUpsertResult(0, 0, 0);
        String city = cityName == null || cityName.trim().isEmpty() ? "北京" : cityName.trim();

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        for (Long seriesId : seriesIds) {
            if (seriesId == null || seriesId <= 0) {
                skipped++;
                continue;
            }
            try {
                String url = "https://www.dongchedi.com/motor/pc/car/series/car_list?series_id=" + seriesId +
                        "&city_name=" + urlEncode(city) +
                        "&aid=1839&app_name=auto_web_pc";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .header("User-Agent", "vehicle-recycle-system/backend-api")
                        .header("Accept", "application/json,text/plain,*/*")
                        .build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    skipped++;
                    continue;
                }
                JsonNode root = objectMapper.readTree(resp.body());
                List<Map<String, Object>> cars = extractCars(root);
                for (Map<String, Object> car : cars) {
                    String carId = asString(car.get("car_id"));
                    String carName = asString(car.get("car_name"));
                    String brand = asString(car.get("brand_name"));
                    String series = asString(car.get("series_name"));
                    Integer year = asInt(car.get("year"));
                    String energyType = asString(car.get("energy_type"));
                    BigDecimal officialPrice = asBigDecimal(car.get("official_price"));
                    String coverUrl = asString(car.get("cover_url"));
                    String pageUrl = car.get("series_id") == null ? null : ("https://www.dongchedi.com/motor/series/" + asString(car.get("series_id")));

                    if (isBlank(carId) || isBlank(brand)) {
                        skipped++;
                        continue;
                    }
                    String marketName = buildMarketName(brand, series, year, carName);

                    Optional<ExternalVehicleTrim> existing = repository.findBySourceAndSourceTrimId("DCD", carId);
                    ExternalVehicleTrim t = existing.orElseGet(ExternalVehicleTrim::new);
                    t.setSource("DCD");
                    t.setSourceTrimId(carId);
                    t.setBrand(brand);
                    t.setSeriesName(trimOrNull(series));
                    t.setMarketName(trimOrNull(marketName));
                    t.setModelYear(year);
                    t.setEnergyType(trimOrNull(energyType));
                    t.setOfficialPrice(officialPrice);
                    t.setCoverUrl(normalizeCoverUrl(coverUrl));
                    t.setPageUrl(pageUrl);
                    t.setRawJson(objectMapper.writeValueAsString(car));
                    repository.save(t);
                    if (existing.isEmpty()) inserted++;
                    else updated++;
                }
            } catch (Exception e) {
                skipped++;
            }
        }

        return new BatchUpsertResult(inserted, updated, skipped);
    }

    private static List<Map<String, Object>> extractCars(JsonNode node) {
        List<Map<String, Object>> out = new ArrayList<>();
        walk(node, out);
        return out;
    }

    private static void walk(JsonNode node, List<Map<String, Object>> out) {
        if (node == null) return;
        if (node.isObject()) {
            JsonNode carId = node.get("car_id");
            JsonNode carName = node.get("car_name");
            JsonNode brandName = node.get("brand_name");
            JsonNode seriesName = node.get("series_name");
            if (carId != null && carName != null && brandName != null && seriesName != null) {
                Map<String, Object> m = new HashMap<>();
                node.fields().forEachRemaining(e -> {
                    if (e.getValue().isValueNode()) {
                        if (e.getValue().isNumber()) m.put(e.getKey(), e.getValue().numberValue());
                        else if (e.getValue().isBoolean()) m.put(e.getKey(), e.getValue().booleanValue());
                        else m.put(e.getKey(), e.getValue().asText());
                    }
                });
                out.add(m);
            }
            node.fields().forEachRemaining(e -> walk(e.getValue(), out));
        } else if (node.isArray()) {
            for (JsonNode n : node) walk(n, out);
        }
    }

    private static String buildMarketName(String brand, String series, Integer year, String carName) {
        StringBuilder sb = new StringBuilder();
        if (!isBlank(brand)) sb.append(brand.trim()).append(" ");
        if (!isBlank(series)) sb.append(series.trim()).append(" ");
        if (year != null) sb.append(year).append("款 ");
        if (!isBlank(carName)) sb.append(carName.trim());
        return sb.toString().trim();
    }

    private static String normalizeCoverUrl(String coverUrl) {
        if (isBlank(coverUrl)) return null;
        String u = coverUrl.trim();
        if (u.startsWith("http://") || u.startsWith("https://")) return u;
        return null;
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String asString(Object o) {
        if (o == null) return null;
        return String.valueOf(o);
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal asBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return new BigDecimal(n.toString());
        try {
            return new BigDecimal(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }
}
