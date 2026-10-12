package com.ankkun.ecommerce.module.mail.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="demo_outbox")@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DemoMessage {
    @Id @org.hibernate.annotations.UuidGenerator private String id;
    private String userId;private String recipient;private String subject;
    @Column(length=8000)private String body;
    @Builder.Default private Instant createdAt=Instant.now();
}
