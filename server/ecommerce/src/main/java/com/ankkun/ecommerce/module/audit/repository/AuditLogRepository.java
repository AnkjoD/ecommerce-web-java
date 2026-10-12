package com.ankkun.ecommerce.module.audit.repository;
import com.ankkun.ecommerce.module.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditLogRepository extends JpaRepository<AuditLog,String> {}
