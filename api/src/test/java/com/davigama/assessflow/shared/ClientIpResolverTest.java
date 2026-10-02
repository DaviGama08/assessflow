package com.davigama.assessflow.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.davigama.assessflow.shared.config.ProxyMode;
import com.davigama.assessflow.shared.ratelimit.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {
    @Test
    void ignoresClientHeadersUnlessProxyModeTrustsTheEdge() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.8");
        request.addHeader("CF-Connecting-IP", "198.51.100.2");

        assertThat(new ClientIpResolver("none", false).clientIp(request)).isEqualTo("10.0.0.8");
        assertThat(new ClientIpResolver("forwarded", false).clientIp(request)).isEqualTo("203.0.113.10");
        assertThat(new ClientIpResolver("cloudflare", false).clientIp(request)).isEqualTo("198.51.100.2");
        assertThat(new ClientIpResolver("none", true).proxyMode()).isEqualTo(ProxyMode.FORWARDED);
    }
}
