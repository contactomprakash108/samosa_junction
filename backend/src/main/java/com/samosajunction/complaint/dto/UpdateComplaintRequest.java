package com.samosajunction.complaint.dto;

import com.samosajunction.complaint.entity.ComplaintPriority;
import com.samosajunction.complaint.entity.ComplaintStatus;

public record UpdateComplaintRequest(ComplaintStatus status, ComplaintPriority priority) {
}
