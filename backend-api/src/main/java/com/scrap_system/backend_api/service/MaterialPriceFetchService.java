package com.scrap_system.backend_api.service;

import com.scrap_system.backend_api.model.MaterialPrice;
import com.scrap_system.backend_api.repository.MaterialPriceRepository;
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

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final String CURRENCY = "CNY";
    private static final String UNIT = "CNY/TON";

    private static final List<Source> SOURCES = List.of(
            new Source("steel", "生意社-螺纹钢", "https://hrb.100ppi.com/", Pattern.compile("螺纹钢参考价为\\s*([0-9.]+).*?(\\d{4}-\\d{2}-\\d{2})", Pattern.DOTALL)),
            new Source("aluminum", "生意社-铝", "https://al.100ppi.com/", Pattern.compile("铝参考价为\\s*([0-9.]+).*?(\\d{4}-\\d{2}-\\d{2})", Pattern.DOTALL)),
            new Source("copper", "生意社-铜", "https://cu.100ppi.com/", Pattern.compile("铜参考价为\\s*([0-9.]+).*?(\\d{4}-\\d{2}-\\d{2})", Pattern.DOTALL)),
            new Source("battery", "生意社-碳酸锂(电池级)", "https://tsl.100ppi.com/", Pattern.compile("碳酸锂-电池级参考价为\\s*([0-9.]+).*?(\\d{4}-\\d{2}-\\d{2})", Pattern.DOTALL)),
            new Source("plastic", "生意社-PP(拉丝)", "https://pp.100ppi.com/", Pattern.compile("PP\\(拉丝\\)参考价为\\s*([0-9.]+).*?(\\d{4}-\\d{2}-\\d{2})", Pattern.DOTALL)),
            new Source("rubber", "生意社-天然橡胶", "https://nr.100ppi.com/", Pattern.compile("天然橡胶参考价为\\s*([0-9.]+).*?(\\d{4}-\\d{2}-\\d{2})", Pattern.DOTALL))
    );

    public FetchResult fetchAndUpsertAll() {
        int updated = 0;
        int inserted = 0;
        int failed = 0;

        for (Source source : SOURCES) {
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
        Optional<MaterialPrice> existing = materialPriceRepository.findByType(source.type());
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

    private record Source(String type, String sourceName, String url, Pattern pattern) {
    }

    private record Fetched(BigDecimal pricePerTon, BigDecimal pricePerKg, LocalDate effectiveDate, LocalDateTime fetchedAt) {
    }

    public record FetchResult(int inserted, int updated, int failed) {
    }
}
