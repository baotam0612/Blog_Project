package com.backlog.backend.service.Impl;

import com.backlog.backend.event.PostLikedEvent;
import com.backlog.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String sender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Override
    public void sendPostLikedEmail(PostLikedEvent event) {

        // Nếu mail đang tắt
        if (!mailEnabled) {
            log.info("Mail is disabled. PostLikedEvent {} was consumed", event.getEventId());
            return;
        }

        // Nếu owner không có email
        if (event.getOwnerEmail() == null || event.getOwnerEmail().isBlank()) {
            log.warn("Skip PostLikedEvent {} because owner email is empty", event.getEventId());
            return;
        }

        // Tạo email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(event.getOwnerEmail());
        message.setSubject("Bài viết của bạn vừa nhận được lượt thích");
        message.setText(event.getLikedByDisplayName()
                + " đã thích bài viết: " + event.getPostTitle());

        mailSender.send(message);
    }
}
