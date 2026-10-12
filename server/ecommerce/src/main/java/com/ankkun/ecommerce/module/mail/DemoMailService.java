package com.ankkun.ecommerce.module.mail;
import com.ankkun.ecommerce.module.mail.entity.DemoMessage;
import com.ankkun.ecommerce.module.mail.repository.DemoMessageRepository;
import com.ankkun.ecommerce.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
@Service @RequiredArgsConstructor
public class DemoMailService {
    private final DemoMessageRepository messages;private final UserRepository users;
    @Value("${app.demo-mode:false}")private boolean demo;
    public void send(String userId,String subject,String text) {if(!demo)return;var user=users.findById(userId).orElseThrow();messages.save(DemoMessage.builder().userId(userId).recipient(user.getEmail()).subject(subject).body(text).build());}
}
