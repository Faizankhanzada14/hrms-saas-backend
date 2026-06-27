package com.hrms.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String eventType, String entityName, String entityId, String description, String status) {
        String username = resolveUsername();
        String ip = resolveIp();

        AuditLog log = AuditLog.builder()
                .username(username)
                .eventType(eventType)
                .entityName(entityName)
                .entityId(entityId)
                .description(description)
                .ipAddress(ip)
                .status(status)
                .build();

        auditLogRepository.save(log);
    }

    public void logSuccess(String eventType, String entityName, String entityId, String description) {
        log(eventType, entityName, entityId, description, "SUCCESS");
    }

    public void logFailure(String eventType, String entityName, String entityId, String description) {
        log(eventType, entityName, entityId, description, "FAILURE");
    }

    private String resolveUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "SYSTEM";
        }
        return auth.getName();
    }

    private String resolveIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.isEmpty()) {
                return xfHeader.split(",")[0];
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
