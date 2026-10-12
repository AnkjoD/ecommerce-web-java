package com.ankkun.ecommerce.module.payment.entity;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;
@Entity @Table(name="user_payment_methods")@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SavedPaymentMethod {
    @Id @org.hibernate.annotations.UuidGenerator private String id;
    private String userId;private String provider;private String bankCode;
    @Builder.Default private String cardType="DEMO";
    @Builder.Default private Instant createdAt=Instant.now();
}
