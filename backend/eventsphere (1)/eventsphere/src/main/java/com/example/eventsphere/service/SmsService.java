package com.example.eventsphere.service;

import com.example.eventsphere.entity.NotificationLog;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.NotificationType;
import com.example.eventsphere.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final NotificationLogRepository notificationLogRepository;
    private final RestTemplate restTemplate;

    @Value("${africastalking.api-key:sandbox_key}")
    private String apiKey;

    @Value("${africastalking.username:sandbox}")
    private String username;

    @Value("${africastalking.sender-id:EventSphere}")
    private String senderId;

    // Used to convert local-format numbers (0XXXXXXXXX) to E.164. Adjust if you target other countries.
    private static final String DEFAULT_COUNTRY_CODE = "255"; // Tanzania

    @Async
    public void sendEventApprovalSms(User organizer, String eventName, String status) {
        String message = "EventSphere: Your event \"" + eventName + "\" has been " + status +
                ". Log in to EventSphere for details.";
        if (organizer.getPhone() != null && !organizer.getPhone().isEmpty()) {
            sendSms(organizer.getPhone(), message, eventName, organizer);
        }
    }

    @Async
    public void sendPaymentStatusSms(User organizer, String eventName, String paymentStatus) {
        String message = "EventSphere: Payment for \"" + eventName + "\" is " + paymentStatus +
                ". Check your EventSphere dashboard.";
        if (organizer.getPhone() != null && !organizer.getPhone().isEmpty()) {
            sendSms(organizer.getPhone(), message, eventName, organizer);
        }
    }

    @Async
    public void sendAccountApprovalSms(User user) {
        String message = "EventSphere: Hello " + user.getFullName() +
                "! Your account has been approved. Log in at http://localhost:5173";
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            sendSms(user.getPhone(), message, "Account Approval", user);
        }
    }

    @Async
    public void sendInvitationSms(String phone, String participantName, String eventName,
                                  String venue, String date, String invitationId) {
        String message = "EventSphere: Hi " + participantName + "! You're invited to " + eventName +
                " at " + venue + " on " + date + ". Your ID: " + invitationId +
                ". Check your email for the invitation card.";
        sendSms(phone, message, eventName, null);
    }

    /**
     * Send an SMS to a single recipient via Africa's Talking Bulk SMS endpoint.
     */
    public void sendSms(String phone, String message, String eventName, User user) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        sendToRecipients(List.of(normalizePhoneNumber(phone)), message, eventName, user);
    }
    @PostConstruct
    public void logSmsConfig() {
        String masked = (apiKey != null && apiKey.length() > 8)
                ? apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4)
                : "MISSING";
        log.info("Africa's Talking config -> username='{}', senderId='{}', apiKey='{}' (len={})",
                username, senderId, masked, apiKey == null ? 0 : apiKey.length());
    }

    /**
     * Send the same message to multiple recipients in a single Africa's Talking Bulk SMS request.
     */
    @Async
    public void sendBulkSms(List<String> phones, String message, String eventName) {
        if (phones == null || phones.isEmpty()) {
            return;
        }
        List<String> normalized = new ArrayList<>();
        for (String phone : phones) {
            if (phone != null && !phone.isBlank()) {
                normalized.add(normalizePhoneNumber(phone));
            }
        }
        if (!normalized.isEmpty()) {
            sendToRecipients(normalized, message, eventName, null);
        }
    }

    @SuppressWarnings("unchecked")
    private void sendToRecipients(List<String> phoneNumbers, String message, String eventName, User user) {
        String url = username.equalsIgnoreCase("sandbox")
                ? "https://api.sandbox.africastalking.com/version1/messaging/bulk"
                : "https://api.africastalking.com/version1/messaging/bulk";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apiKey", apiKey);
        headers.set("Accept", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("message", message);
        body.put("phoneNumbers", phoneNumbers);
        if (senderId != null && !senderId.isBlank()) {
            body.put("senderId", senderId);
        }

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> recipients = null;

            if (responseBody != null && responseBody.get("SMSMessageData") instanceof Map) {
                Map<String, Object> smsData = (Map<String, Object>) responseBody.get("SMSMessageData");
                if (smsData.get("Recipients") instanceof List) {
                    recipients = (List<Map<String, Object>>) smsData.get("Recipients");
                }
            }

            if (recipients == null || recipients.isEmpty()) {
                String error = "Unexpected response from Africa's Talking: " + responseBody;
                log.warn("SMS to {} - {}", phoneNumbers, error);
                for (String phone : phoneNumbers) {
                    logSms(phone, message, eventName, user, false, error);
                }
                return;
            }

            for (Map<String, Object> recipient : recipients) {
                String number = String.valueOf(recipient.get("number"));
                String status = String.valueOf(recipient.get("status"));
                boolean success = "Success".equalsIgnoreCase(status);
                logSms(number, message, eventName, user, success, success ? null : status);
                log.info("SMS to {}: status={}, messageId={}", number, status, recipient.get("messageId"));
            }

        } catch (HttpStatusCodeException e) {
            // AT returns useful error JSON (e.g. invalid api key, invalid sender id) in the body
            String errorBody = e.getResponseBodyAsString();
            log.error("Failed to send SMS to {}: HTTP {} - {}", phoneNumbers, e.getStatusCode(), errorBody);
            for (String phone : phoneNumbers) {
                logSms(phone, message, eventName, user, false, errorBody);
            }
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumbers, e.getMessage());
            for (String phone : phoneNumbers) {
                logSms(phone, message, eventName, user, false, e.getMessage());
            }
        }
    }

    /**
     * Normalizes a phone number to E.164, required by Africa's Talking.
     * - "+..." numbers are left as-is.
     * - "0XXXXXXXXX" local format gets the default country code.
     * - Numbers already starting with the country code (no "+") get a "+" prefixed.
     */
    private String normalizePhoneNumber(String phone) {
        String cleaned = phone.replaceAll("[\\s\\-()]", "");

        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        if (cleaned.startsWith("0")) {
            return "+" + DEFAULT_COUNTRY_CODE + cleaned.substring(1);
        }
        if (cleaned.startsWith(DEFAULT_COUNTRY_CODE)) {
            return "+" + cleaned;
        }
        return "+" + DEFAULT_COUNTRY_CODE + cleaned;
    }

    private void logSms(String phone, String message, String eventName, User user, boolean delivered, String error) {
        NotificationLog nl = NotificationLog.builder()
                .type(NotificationType.SMS)
                .recipient(phone)
                .subject("SMS")
                .content(message)
                .eventName(eventName)
                .delivered(delivered)
                .errorMessage(error)
                .user(user)
                .build();
        notificationLogRepository.save(nl);
    }
}