package br.com.ascend.the_system.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {
    private MockHttpServletResponse preflight(String origin) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/me");
        request.setServletPath("/api/auth/me");
        request.setScheme("https");
        request.setServerName("api.example");
        request.setServerPort(443);
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new CorsConfig().corsFilter().doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test void permitsProductionAndCapacitorLocalhost() throws Exception {
        for (String origin : new String[]{"https://ascend22.vercel.app", "http://localhost", "https://localhost", "http://localhost:4183"}) {
            assertEquals(origin, preflight(origin).getHeader("Access-Control-Allow-Origin"));
        }
    }

    @Test void rejectsUntrustedNetworkAndPreviewOriginsByDefault() throws Exception {
        for (String origin : new String[]{"http://192.168.0.10:8080", "https://ascend22-untrusted.vercel.app", "https://attacker.example"}) {
            assertNull(preflight(origin).getHeader("Access-Control-Allow-Origin"));
        }
    }
}
