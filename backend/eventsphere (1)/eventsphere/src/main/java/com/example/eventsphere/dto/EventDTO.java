package com.example.eventsphere.dto;

import com.example.eventsphere.enums.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

public class EventDTO {

    @Data
    public static class CreateRequest {
        @NotBlank private String eventName;
        @NotNull private EventCategory category;
        @NotBlank private String description;
        @NotBlank private String venue;
        @NotNull private LocalDate eventDate;
        @NotNull private LocalTime startTime;
        @NotNull private LocalTime endTime;
        @NotNull @Min(1) private Integer capacity;
        private String contactInfo;
        @NotNull private EventLayout selectedLayout;
    }

    @Data
    public static class EventResponse {
        private Long id;
        private String eventName;
        private EventCategory category;
        private String description;
        private String venue;
        private LocalDate eventDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer capacity;
        private String contactInfo;
        private EventStatus status;
        private EventLayout selectedLayout;
        private Double totalAmount;
        private String adminRemarks;
        private String organizerName;
        private String organizerEmail;
        private Long organizerId;
        private String createdAt;
        private PaymentInfo payment;
        private Long generatedCards;
    }

    @Data
    public static class PaymentInfo {
        private Long id;
        private String invoiceNumber;
        private Double amount;
        private PaymentStatus status;
        private boolean hasProof;
    }

    @Data
    public static class StatusUpdateRequest {
        @NotNull private EventStatus status;
        private String adminRemarks;
    }
}
