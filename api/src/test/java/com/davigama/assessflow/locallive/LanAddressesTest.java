package com.davigama.assessflow.locallive;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.locallive.domain.JoinUrls;
import com.davigama.assessflow.locallive.domain.LanAddresses;
import org.junit.jupiter.api.Test;

class LanAddressesTest {
    @Test
    void classifiesPrivateIpv4AndOrigins() {
        assertThat(LanAddresses.privateIpv4("192.168.1.50")).isTrue();
        assertThat(LanAddresses.privateIpv4("10.0.0.8")).isTrue();
        assertThat(LanAddresses.privateIpv4("172.16.0.2")).isTrue();
        assertThat(LanAddresses.privateIpv4("8.8.8.8")).isFalse();
        assertThat(LanAddresses.privateIpv4("127.0.0.1")).isTrue();
        assertThat(LanAddresses.allowedOrigin("http://192.168.1.50:8080", 8080)).isTrue();
        assertThat(LanAddresses.allowedOrigin("http://192.168.1.50:8080", 9090)).isFalse();
        assertThat(LanAddresses.allowedOrigin("http://example.com:8080", 8080)).isFalse();
        assertThat(LanAddresses.allowedOrigin(null, 8080)).isTrue();
    }

    @Test
    void buildsJoinAndWifiPayloads() {
        assertThat(JoinUrls.join("192.168.1.50/", 8080, "x7k92p"))
                .isEqualTo("http://192.168.1.50:8080/join/X7K92P");
        assertThat(JoinUrls.wifi("AssessFlow-X7K9", "ab;c")).isEqualTo("WIFI:T:WPA;S:AssessFlow-X7K9;P:ab\\;c;;");
    }
}
