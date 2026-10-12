package com.ankkun.ecommerce.config;
import com.ankkun.ecommerce.module.audit.entity.AuditLog;
import com.ankkun.ecommerce.module.audit.repository.AuditLogRepository;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.*;
import java.util.*;
@Configuration @RequiredArgsConstructor
public class AuditConfig implements WebMvcConfigurer {
    private final AuditLogRepository logs;
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public void afterCompletion(HttpServletRequest req,HttpServletResponse res,Object handler,Exception ex) {
                var auth=SecurityContextHolder.getContext().getAuthentication();
                if(auth==null || !auth.isAuthenticated() || !auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                    || Set.of("GET","HEAD","OPTIONS").contains(req.getMethod()) || res.getStatus()>=400 || ex!=null) return;
                String path=req.getRequestURI(); if(!path.startsWith("/api/admin/") && !path.startsWith("/api/coupons")) return;
                String[] parts=path.replaceFirst("^/api/(admin/)?","").split("/");
                String type=parts[0].replaceFirst("s$","");String action=switch(req.getMethod()){case "POST" -> "create";case "DELETE" -> "delete";default -> "update";};
                logs.save(AuditLog.builder().userId(auth.getPrincipal().toString()).action(action).entityType(type)
                    .entityId(parts.length>1?parts[1]:null).path(path).build());
            }
        }).addPathPatterns("/api/**");
    }
}
