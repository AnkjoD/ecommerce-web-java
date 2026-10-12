package com.ankkun.ecommerce.module.audit;
import com.ankkun.ecommerce.common.dto.*;
import com.ankkun.ecommerce.module.audit.repository.AuditLogRepository;
import com.ankkun.ecommerce.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import java.util.*;
@RestController @RequiredArgsConstructor
public class AuditController {
    private final AuditLogRepository logs; private final UserRepository users;
    @GetMapping("/api/admin/audit-logs") public Object list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="50") int limit) {
        page=Math.max(1,page);limit=Math.max(1,Math.min(100,limit));
        var result=logs.findAll(PageRequest.of(page-1,limit,Sort.by(Sort.Direction.DESC,"createdAt")));
        var rows=result.getContent().stream().map(log -> {
            Map<String,Object> r=new LinkedHashMap<>();r.put("id",log.getId());r.put("action",log.getAction());r.put("entity_type",log.getEntityType());r.put("entity_id",log.getEntityId());r.put("created_at",log.getCreatedAt());
            r.put("details",Map.of("path",log.getPath()));
            if(log.getUserId()!=null) users.findById(log.getUserId()).ifPresent(u -> r.put("user",Map.of("full_name",u.getFullName(),"email",u.getEmail())));
            return r;
        }).toList();return ApiResponse.ok(PaginatedResponse.of(rows,page,limit,result.getTotalElements()));
    }
}
