package com.example.eventsphere.dto;

import com.example.eventsphere.enums.InvitationStatus;
import lombok.Data;

import java.util.List;

public class InvitationDTO {

    @Data
    public static class GenerateRequest {
        private Long eventId;
        private List<ParticipantInput> participants;
    }

    @Data
    public static class ParticipantInput {
        private String participantName;
        private String email;
        private String phone;
    }

    @Data
    public static class BulkSendRequest {
        private Long eventId;
        private List<Long> cardIds;
        private boolean sendEmail;
        private boolean sendSms;
    }

    @Data
    public static class SingleSendRequest {
        private Long cardId;
        private boolean sendEmail;
        private boolean sendSms;
    }

    @Data
    public static class CardResponse {
        private Long id;
        private String invitationId;
        private String participantName;
        private String participantEmail;
        private String participantPhone;
        private InvitationStatus emailStatus;
        private InvitationStatus smsStatus;
        private String emailSentAt;
        private String smsSentAt;
        private Boolean qrScanned;
        private String scannedAt;
        private String cardPdfPath;
        private String cardImagePath;
        private String qrCodePath;
        private Long eventId;
        private String eventName;
        private String createdAt;
    }

    @Data
    public static class ScanRequest {
        private String invitationId;
    }

    @Data
    public static class ScanResponse {
        private String status; // VALID, ALREADY_SCANNED, INVALID
        private String message;
        private String participantName;
        private String eventName;
        private String invitationId;
        private String scannedAt;
    }

    @Data
    public static class StatsResponse {
        private long total;
        private long emailSent;
        private long smsSent;
        private long delivered;
        private long opened;
        private long pending;
        private long scanned;
    }
}
