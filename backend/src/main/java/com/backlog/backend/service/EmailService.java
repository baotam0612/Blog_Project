package com.backlog.backend.service;

import com.backlog.backend.event.PostLikedEvent;

public interface EmailService {

    void sendPostLikedEmail(PostLikedEvent event);
}
