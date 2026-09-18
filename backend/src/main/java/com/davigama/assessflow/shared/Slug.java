package com.davigama.assessflow.shared;

import com.davigama.assessflow.shared.exception.DomainException;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.http.HttpStatus;

public final class Slug {
    private Slug() {}

    public static String normalize(String supplied) {
        String slug = Normalizer.normalize(supplied.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (slug.isBlank() || slug.length() > 100) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_SLUG", "Invalid slug.");
        }
        return slug;
    }
}
