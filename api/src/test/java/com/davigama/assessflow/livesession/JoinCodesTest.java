package com.davigama.assessflow.livesession;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.livesession.domain.JoinCodes;
import org.junit.jupiter.api.Test;

class JoinCodesTest {
    @Test
    void generatesNormalizedUnambiguousCodes() {
        String code = JoinCodes.generate();
        assertThat(code).hasSize(6);
        assertThat(JoinCodes.valid(code)).isTrue();
        assertThat(JoinCodes.normalize("  ab23kp ")).isEqualTo("AB23KP");
        assertThat(JoinCodes.valid("IIIIII")).isFalse();
    }
}
