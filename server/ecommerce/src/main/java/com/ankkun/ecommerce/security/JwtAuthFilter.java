package com.ankkun.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final com.ankkun.ecommerce.module.user.repository.UserRepository users;
    @org.springframework.beans.factory.annotation.Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (!java.util.Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod()) && origin != null
                && !java.util.List.of(frontendUrl, "http://localhost:3000", "http://localhost:8080", "http://localhost:8081").contains(origin)) {
            response.sendError(403); return;
        }
        String token = extractToken(request);

        if (token != null && jwtTokenProvider.validateAccessToken(token)) {
            Claims claims = jwtTokenProvider.parseAccessToken(token);
            String userId = claims.getSubject();
            var user = users.findById(userId).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getIsActive())) { response.sendError(401); return; }
            Number version=claims.get("ver",Number.class);
            if(version==null || version.intValue()!=user.getTokenVersion()) {response.sendError(401);return;}
            String role = user.getRole().name();

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ((request.getRequestURI().startsWith("/api/admin/") || "admin".equals(request.getHeader("X-Client-App"))
                        ? "adminAccessToken" : "accessToken").equals(cookie.getName())) return cookie.getValue();
            }
        }
        return null;
    }
}
