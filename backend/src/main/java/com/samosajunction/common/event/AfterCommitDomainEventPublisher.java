package com.samosajunction.common.event;

import com.samosajunction.common.support.AfterCommit;

public class AfterCommitDomainEventPublisher implements DomainEventPublisher {

    private final DomainEventSender sender;

    public AfterCommitDomainEventPublisher(DomainEventSender sender) {
        this.sender = sender;
    }

    @Override
    public void publishAfterCommit(DomainEvent event) {
        AfterCommit.run(() -> sender.send(event));
    }
}
