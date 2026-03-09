package com.scrap_system.backend_api.service;

import com.scrap_system.backend_api.dto.MaterialSourceSuggestResult;
import com.scrap_system.backend_api.model.MaterialPrice;
import com.scrap_system.backend_api.model.MaterialSourceConfig;
import com.scrap_system.backend_api.repository.MaterialPriceRepository;
import com.scrap_system.backend_api.repository.MaterialSourceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialPriceFetchService {

    private final MaterialPriceRepository materialPriceRepository;
    private final MaterialSourceConfigRepository materialSourceConfigRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final String CURRENCY = "CNY";
    private static final String UNIT = "CNY/TON";

    private static final List<SourceSeed> DEFAULT_SOURCES = List.of(
            new SourceSeed("steel", "螺纹钢", "https://hrb.100ppi.com/"),
            new SourceSeed("aluminum", "铝", "https://al.100ppi.com/"),
            new SourceSeed("copper", "铜", "https://cu.100ppi.com/"),
            new SourceSeed("battery", "碳酸锂-电池级", "https://tsl.100ppi.com/"),
            new SourceSeed("plastic", "PP(拉丝)", "https://pp.100ppi.com/"),
            new SourceSeed("rubber", "天然橡胶", "https://nr.100ppi.com/")
    );

    public FetchResult fetchAndUpsertAll() {
        ensureDefaultSources();
        int updated = 0;
        int inserted = 0;
        int failed = 0;

        for (Source source : listEnabledSources()) {
            try {
                Fetched f = fetchOne(source);
                boolean isInsert = upsert(source, f);
                if (isInsert) inserted++;
                else updated++;
            } catch (Exception e) {
                failed++;
                log.warn("material price fetch failed: type={}, url={}", source.type(), source.url(), e);
            }
        }

        return new FetchResult(inserted, updated, failed);
    }

    public List<MaterialSourceConfig> listSources() {
        ensureDefaultSources();
        return materialSourceConfigRepository.findAll().stream()
                .sorted(Comparator.comparing(MaterialSourceConfig::getType))
                .toList();
    }

    public MaterialSourceConfig upsertSource(MaterialSourceConfig input) {
        ensureDefaultSources();
        String type = input.getType().trim().toLowerCase(Locale.ROOT);
        MaterialSourceConfig source = materialSourceConfigRepository.findByType(type).orElseGet(MaterialSourceConfig::new);
        source.setType(type);
        source.setDisplayName(input.getDisplayName());
        source.setSourceName(input.getSourceName());
        source.setSourceUrl(input.getSourceUrl());
        source.setParseKeyword(input.getParseKeyword());
        source.setEnabled(Boolean.TRUE.equals(input.getEnabled()));
        return materialSourceConfigRepository.save(source);
    }

    public List<MaterialSourceSuggestResult> suggestFromKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();
        String q = keyword.trim();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.100ppi.com/sf/search.aspx?keyword=" + encode(q)))
                    .GET()
                    .header("User-Agent", "vehicle-recycle-system/backend-api")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                return List.of();
            }
            Charset charset = charsetFrom(resp.headers().firstValue("content-type").orElse(null));
            String body = new String(resp.body(), charset);
            Pattern p = Pattern.compile("<a[^>]+href=[\"'](https?://([a-z0-9]+)\\.100ppi\\.com/)[\"'][^>]*>([^<]{1,64})</a>", Pattern.CASE_INSENSITIVE);
            Matcher matcher = p.matcher(body);
            List<MaterialSourceSuggestResult> results = new ArrayList<>();
            while (matcher.find()) {
                String url = matcher.group(1);
                String type = matcher.group(2).toLowerCase(Locale.ROOT);
                String text = normalizeText(matcher.group(3));
                if (text.isBlank()) continue;
                results.add(MaterialSourceSuggestResult.builder()
                        .type(type)
                        .displayName(text)
                        .sourceName("生意社-" + text)
                        .sourceUrl(url)
                        .parseKeyword(text)
                        .build());
            }
            return results.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            r -> r.getType() + "|" + r.getDisplayName(),
                            r -> r,
                            (a, b) -> a
                    ))
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(MaterialSourceSuggestResult::getDisplayName))
                    .limit(30)
                    .toList();
        } catch (Exception e) {
            log.warn("material source suggest failed: keyword={}", q, e);
            return List.of();
        }
    }

    private Fetched fetchOne(Source source) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(source.url()))
                .GET()
                .header("User-Agent", "vehicle-recycle-system/backend-api")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build();

        HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("unexpected status: " + resp.statusCode());
        }

        Charset charset = charsetFrom(resp.headers().firstValue("content-type").orElse(null));
        String body = new String(resp.body(), charset);
        Matcher m = source.pattern().matcher(body);
        if (!m.find()) {
            throw new IllegalStateException("no match");
        }

        BigDecimal pricePerTon = new BigDecimal(m.group(1));
        LocalDate effectiveDate = LocalDate.parse(m.group(2));
        LocalDateTime fetchedAt = LocalDateTime.now();

        BigDecimal pricePerKg = pricePerTon.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
        return new Fetched(pricePerTon, pricePerKg, effectiveDate, fetchedAt);
    }

    private boolean upsert(Source source, Fetched fetched) {
        Optional<MaterialPrice> existing = materialPriceRepository.findByTypeAndEffectiveDate(source.type(), fetched.effectiveDate());
        MaterialPrice p = existing.orElseGet(MaterialPrice::new);

        p.setType(source.type());
        p.setPricePerKg(fetched.pricePerKg());
        p.setCurrency(CURRENCY);
        p.setUnit(UNIT);
        p.setEffectiveDate(fetched.effectiveDate());
        p.setFetchedAt(fetched.fetchedAt());
        p.setSourceName(source.sourceName());
        p.setSourceUrl(source.url());
        p.setRawPayload(buildRawPayload(source, fetched));

        materialPriceRepository.save(p);
        return existing.isEmpty();
    }

    private List<Source> listEnabledSources() {
        return materialSourceConfigRepository.findByEnabledTrueOrderByTypeAsc().stream()
                .map(this::fromConfig)
                .toList();
    }

    private Source fromConfig(MaterialSourceConfig c) {
        String keyword = c.getParseKeyword();
        if (keyword == null || keyword.isBlank()) {
            keyword = c.getDisplayName();
        }
        String regex = Pattern.quote(keyword.trim()) + "\\s*参考价为\\s*([0-9.]+).*?(\\d{4}-\\d{2}-\\d{2})";
        return new Source(c.getType(), c.getSourceName(), c.getSourceUrl(), Pattern.compile(regex, Pattern.DOTALL));
    }

    private void ensureDefaultSources() {
        for (SourceSeed seed : DEFAULT_SOURCES) {
            Optional<MaterialSourceConfig> existing = materialSourceConfigRepository.findByType(seed.type());
            if (existing.isPresent()) continue;
            MaterialSourceConfig c = new MaterialSourceConfig();
            c.setType(seed.type());
            c.setDisplayName(seed.displayName());
            c.setSourceName("生意社-" + seed.displayName());
            c.setSourceUrl(seed.url());
            c.setParseKeyword(seed.displayName());
            c.setEnabled(true);
            materialSourceConfigRepository.save(c);
        }
    }

    private static String buildRawPayload(Source source, Fetched fetched) {
        return "{\"sourceName\":\"" + jsonEscape(source.sourceName()) + "\"," +
                "\"sourceUrl\":\"" + jsonEscape(source.url()) + "\"," +
                "\"sourcePrice\":\"" + fetched.pricePerTon().toPlainString() + "\"," +
                "\"sourceUnit\":\"" + UNIT + "\"," +
                "\"currency\":\"" + CURRENCY + "\"," +
                "\"effectiveDate\":\"" + fetched.effectiveDate() + "\"," +
                "\"fetchedAt\":\"" + fetched.fetchedAt() + "\"}";
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Charset charsetFrom(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("charset=");
        if (idx < 0) {
            return StandardCharsets.UTF_8;
        }
        String cs = lower.substring(idx + "charset=".length()).trim();
        int semi = cs.indexOf(";");
        if (semi >= 0) cs = cs.substring(0, semi).trim();
        try {
            return Charset.forName(cs);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static String normalizeText(String text) {
        if (text == null) return "";
        return text.replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }

    private static String encode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private record Source(String type, String sourceName, String url, Pattern pattern) {
    }

    private record SourceSeed(String type, String displayName, String url) {
    }

    private record Fetched(BigDecimal pricePerTon, BigDecimal pricePerKg, LocalDate effectiveDate, LocalDateTime fetchedAt) {
    }

    public record FetchResult(int inserted, int updated, int failed) {
    }
}
