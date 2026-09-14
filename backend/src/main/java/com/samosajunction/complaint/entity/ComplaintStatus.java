package com.samosajunction.complaint.entity;

public enum ComplaintStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    REJECTED;

    public boolean canTransitionTo(ComplaintStatus next) {
        if (next == null || next == this) {
            return false;
        }
        return switch (this) {
            case OPEN -> next == IN_PROGRESS || next == RESOLVED || next == REJECTED;
            case IN_PROGRESS -> next == RESOLVED || next == REJECTED;
            case RESOLVED, REJECTED -> false;
        };
    }
}
