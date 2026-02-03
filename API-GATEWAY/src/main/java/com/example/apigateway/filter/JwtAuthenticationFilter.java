package com.example.apigateway.filter;

import com.example.apigateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Value("${security.public-urls}")
    private List<String> publicUrls;

    @Value("${jwt.header}")
    private String authHeader;

    @Value("${jwt.prefix}")
    private String prefix;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // 1️⃣ Public URL check
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 2️⃣ Authorization header
        String token = null;
        String header = exchange.getRequest().getHeaders().getFirst(authHeader);

        if (header != null && header.startsWith(prefix + " ")) {
             token = header.substring((prefix + " ").length());
        } else {
             // 3️⃣ Cookie fallback
             if (exchange.getRequest().getCookies().containsKey("JWT")) {
                 token = exchange.getRequest().getCookies().getFirst("JWT").getValue();
             }
        }
        
        if (token == null) {
              exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
              return exchange.getResponse().setComplete();
        }

        // 4️⃣ Validate token
        if (!jwtUtil.isTokenValid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        Long userId = jwtUtil.extractUserId(token);

        // 4️⃣ Forward userId
        System.out.println("GATEWAY → sending X-USER-ID = " + userId);
        ServerHttpRequest modifiedRequest = exchange.getRequest()
                .mutate()
                .header("X-USER-ID", String.valueOf(userId))
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isPublicPath(String path) {
        return publicUrls.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -1;
    }

}
