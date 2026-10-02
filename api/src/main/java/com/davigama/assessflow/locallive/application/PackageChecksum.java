package com.davigama.assessflow.locallive.application;

import com.davigama.assessflow.locallive.api.dto.AssessmentPackageV1;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * SHA-256 package checksum detects corruption.
 * It does not prove package authenticity.
 */
public final class PackageChecksum {
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .disable(SerializationFeature.INDENT_OUTPUT)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private PackageChecksum() {}

    public static String sha256(AssessmentPackageV1.PackageContent content) {
        try {
            byte[] json = MAPPER.writeValueAsBytes(content);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash local event package.", ex);
        }
    }

    public static boolean matches(AssessmentPackageV1 pack) {
        if (pack == null || pack.checksumSha256() == null) {
            return false;
        }
        String expected = pack.checksumSha256().trim().toLowerCase(Locale.ROOT);
        String actual = sha256(pack.content());
        return expected.length() == actual.length()
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    public static String canonicalJson(AssessmentPackageV1.PackageContent content) {
        return new String(MAPPER.writeValueAsBytes(content), StandardCharsets.UTF_8);
    }
}
