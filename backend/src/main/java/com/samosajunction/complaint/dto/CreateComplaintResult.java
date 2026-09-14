package com.samosajunction.complaint.dto;

public record CreateComplaintResult(ComplaintResponse complaint, boolean replayed) {
}
