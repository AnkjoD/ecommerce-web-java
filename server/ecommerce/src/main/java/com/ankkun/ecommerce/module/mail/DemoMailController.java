package com.ankkun.ecommerce.module.mail;
import com.ankkun.ecommerce.module.mail.repository.DemoMessageRepository;
import com.ankkun.ecommerce.common.dto.*;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.Map;
@RestController @RequiredArgsConstructor
public class DemoMailController {
    private final DemoMessageRepository messages;
    @GetMapping("/api/admin/mail/outbox")public Object list(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int limit){page=Math.max(1,page);limit=Math.max(1,Math.min(100,limit));var result=messages.findAll(PageRequest.of(page-1,limit,Sort.by(Sort.Direction.DESC,"createdAt")));return ApiResponse.ok(PaginatedResponse.of(result.getContent().stream().map(m->Map.of("id",m.getId(),"recipient",m.getRecipient(),"subject",m.getSubject(),"body",m.getBody(),"created_at",m.getCreatedAt(),"mode","demo")).toList(),page,limit,result.getTotalElements()));}
}
