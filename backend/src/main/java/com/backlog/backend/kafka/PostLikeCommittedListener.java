package com.backlog.backend.kafka;

import com.backlog.backend.event.PostLikedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PostLikeCommittedListener {

    private final PostLikeProducer postLikeProducer;

    // Spring tự gọi publishAfterCommit() sau khi MySQL commit thành công.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(PostLikedEvent event) {
        postLikeProducer.send(event);
    }
}
