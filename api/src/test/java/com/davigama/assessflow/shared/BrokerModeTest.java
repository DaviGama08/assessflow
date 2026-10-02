package com.davigama.assessflow.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.davigama.assessflow.shared.config.BrokerMode;
import org.junit.jupiter.api.Test;

class BrokerModeTest {
    @Test
    void parsesSimpleAndRelayAndRejectsUnknown() {
        assertThat(BrokerMode.from("simple")).isEqualTo(BrokerMode.SIMPLE);
        assertThat(BrokerMode.from("RELAY")).isEqualTo(BrokerMode.RELAY);
        assertThat(BrokerMode.from(null)).isEqualTo(BrokerMode.SIMPLE);
        assertThatThrownBy(() -> BrokerMode.from("kafka"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("simple, relay");
    }
}
