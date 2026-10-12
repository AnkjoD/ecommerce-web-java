package com.ankkun.ecommerce.module.audit.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
@Entity @Table(name="audit_logs") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {
    @Id @UuidGenerator private String id;
    private String userId;
    private String action;
    private String entityType;
    private String entityId;
    private String path;
    @Builder.Default private Instant createdAt=Instant.now();
}
