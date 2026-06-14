package com.example.eventsphere.dto;

import com.example.eventsphere.enums.InvitationStatus;
import com.example.eventsphere.enums.PaymentStatus;
import lombok.Data;

import java.util.List;

public class PaymentDTO {

    @Data
    public static class PaymentResponse {
        private Long id;
        private String invoiceNumber;
        private Double amount;
        private PaymentStatus status;
        private String proofFileName;
        private boolean hasProof;
        private String adminRemarks;
        private String verifiedAt;
        private String createdAt;
        private Long eventId;
        private String eventName;
        private String organizerName;
    }

    @Data
    public static class VerifyRequest {
        private PaymentStatus status;
        private String adminRemarks;
    }
}
