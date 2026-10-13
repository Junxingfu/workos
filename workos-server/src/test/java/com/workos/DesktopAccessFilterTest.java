package com.workos;

import com.workos.desktop.DesktopAccessFilter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;

class DesktopAccessFilterTest {
    private static final String TOKEN = "0123456789abcdef0123456789abcdef";
    private final DesktopAccessFilter filter = new DesktopAccessFilter(TOKEN);
    private MockHttpServletRequest request(String path) {
        var request = new MockHttpServletRequest("GET", path);
        request.setLocalPort(45123); request.addHeader("Host", "127.0.0.1:45123");
        return request;
    }
    @Test void rejectsUnauthenticatedAndForeignOriginsEvenWithValidCookie() throws Exception {
        var request = request("/api/items");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(401, response.getStatus());
        request = request("/api/items"); request.setCookies(new Cookie("workos_desktop", TOKEN));
        request.addHeader("Origin", "https://example.org"); response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain()); assertEquals(403, response.getStatus());
        request = request("/api/items"); request.addHeader("X-WorkOS-Token", TOKEN);
        request.removeHeader("Host"); request.addHeader("Host", "attacker.example:45123");
        response = new MockHttpServletResponse(); filter.doFilter(request, response, new MockFilterChain());
        assertEquals(403, response.getStatus());
    }
    @Test void exchangesSessionForHttpOnlyCookieThenAllowsSameOriginRequests() throws Exception {
        var request = request("/desktop/connect"); request.addParameter("token", TOKEN);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals("/today", response.getRedirectedUrl());
        assertTrue(response.getHeader("Set-Cookie").contains("HttpOnly; SameSite=Strict"));
        assertEquals("no-referrer", response.getHeader("Referrer-Policy"));
        request = request("/api/items"); request.setCookies(new Cookie("workos_desktop", TOKEN));
        request.addHeader("Origin", "http://127.0.0.1:45123");
        var chain = new MockFilterChain(); filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertNotNull(chain.getRequest());
    }
    @Test void refusesWeakSessionSecrets() { assertThrows(IllegalArgumentException.class, () -> new DesktopAccessFilter("short")); }
}
