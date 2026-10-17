package com.workos.desktop;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 随机回环端口仍需会话认证，防止任意本机网页调用桌面数据接口。 */
@Component
@Profile("desktop")
@ConditionalOnWebApplication
@Order(-100)
public class DesktopAccessFilter extends OncePerRequestFilter {
  private final String token;

  public DesktopAccessFilter(@Value("${workos.desktop.token}") String token) {
    if (token.length() < 32) {
      throw new IllegalArgumentException("桌面会话密钥未配置");
    }
    this.token = token;
  }

  private boolean matches(String value) {
    return value != null
        && MessageDigest.isEqual(
            token.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    response.setHeader("Referrer-Policy", "no-referrer");
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "DENY");
    String host = "127.0.0.1:" + request.getLocalPort();
    if (!host.equals(request.getHeader("Host"))) {
      response.sendError(403);
      return;
    }
    String origin = request.getHeader("Origin");
    if (origin != null && !origin.equals("http://" + host)) {
      response.sendError(403);
      return;
    }
    if ("/desktop/connect".equals(request.getRequestURI())
        && matches(request.getParameter("token"))) {
      response.setHeader(
          "Set-Cookie", "workos_desktop=" + token + "; HttpOnly; SameSite=Strict; Path=/");
      response.setHeader("Cache-Control", "no-store");
      response.sendRedirect(
          "focus".equals(request.getParameter("view"))
              ? "/focus"
              : "1".equals(request.getParameter("welcome")) ? "/desktop-data" : "/today");
      return;
    }
    boolean accepted = matches(request.getHeader("X-WorkOS-Token"));
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if (cookie.getName().equals("workos_desktop") && matches(cookie.getValue())) {
          accepted = true;
        }
      }
    }
    if (!accepted) {
      response.sendError(401);
      return;
    }
    chain.doFilter(request, response);
  }
}
