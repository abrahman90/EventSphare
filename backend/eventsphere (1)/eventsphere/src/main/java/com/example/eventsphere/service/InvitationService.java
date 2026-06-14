package com.example.eventsphere.service;

import com.example.eventsphere.dto.InvitationDTO;
import com.example.eventsphere.entity.Event;
import com.example.eventsphere.entity.InvitationCard;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.InvitationStatus;
import com.example.eventsphere.enums.PaymentStatus;
import com.example.eventsphere.repository.EventRepository;
import com.example.eventsphere.repository.InvitationCardRepository;
import com.example.eventsphere.repository.PaymentRepository;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Getter
@Setter

public class InvitationService {

    private final InvitationCardRepository cardRepository;
    private final EventRepository eventRepository;
    private final PaymentRepository paymentRepository;
    private final QrCodeService qrCodeService;
    private final PdfCardService pdfCardService;
    private final EmailService emailService;
    private final SmsService smsService;

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN — Generate cards based on event capacity
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public List<InvitationCard> generateCardsForEvent(Long eventId) throws IOException {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        var payment = paymentRepository.findByEvent_Id(eventId)
                .orElseThrow(() -> new RuntimeException("No payment found for this event"));
        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new RuntimeException("Payment must be approved before generating cards");
        }

        // Delete existing cards if regenerating
        List<InvitationCard> existing = cardRepository.findByEvent_Id(eventId);
        if (!existing.isEmpty()) {
            cardRepository.deleteAll(existing);
            log.info("Deleted {} existing cards for event {}", existing.size(), eventId);
        }

        int capacity = event.getCapacity();
        List<InvitationCard> generated = new ArrayList<>();

        for (int i = 1; i <= capacity; i++) {
            String uniqueId = generateSequentialInvitationId(event, i);

            String qrData = String.join("|",
                    uniqueId,
                    "Guest " + i,
                    event.getEventName(),
                    event.getEventDate().toString(),
                    event.getVenue()
            );

            String qrPath = null;
            try {
                qrPath = qrCodeService.generateQrCode(qrData, uniqueId);
            } catch (Exception e) {
                log.error("QR generation failed for {}: {}", uniqueId, e.getMessage());
            }

            InvitationCard card = InvitationCard.builder()
                    .invitationId(uniqueId)
                    .participantName("Guest " + i)
                    .participantEmail(null)
                    .participantPhone(null)
                    .qrCodePath(qrPath)
                    .emailStatus(InvitationStatus.PENDING)
                    .smsStatus(InvitationStatus.PENDING)
                    .qrScanned(false)
                    .event(event)
                    .build();

            InvitationCard saved = cardRepository.save(card);

            try {
                String pdfPath = pdfCardService.generateInvitationCard(saved, event);
                saved.setCardPdfPath(pdfPath);
                cardRepository.save(saved);
            } catch (Exception e) {
                log.error("PDF generation failed for {}: {}", uniqueId, e.getMessage());
            }

            generated.add(saved);
        }

        log.info("Generated {} cards for event: {}", generated.size(), event.getEventName());

        User organizer = event.getOrganizer();
        try {
            emailService.sendEventApprovalEmail(
                    organizer.getFullName(), organizer.getEmail(),
                    event.getEventName(),
                    generated.size() + " Invitation Cards Generated",
                    "Your " + generated.size() + " invitation cards are ready. Log in to assign participants and send them.");
        } catch (Exception e) {
            log.error("Email notification failed: {}", e.getMessage());
        }
        try {
            smsService.sendEventApprovalSms(organizer, event.getEventName(),
                    generated.size() + " invitation cards generated and ready.");
        } catch (Exception e) {
            log.error("SMS notification failed: {}", e.getMessage());
        }

        return generated;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN — Generate cards with explicit participant list (legacy)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public List<InvitationDTO.CardResponse> generateCards(
            Long eventId,
            List<InvitationDTO.ParticipantInput> participants) throws IOException {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        var payment = paymentRepository.findByEvent_Id(eventId)
                .orElseThrow(() -> new RuntimeException("No payment found for this event"));
        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new RuntimeException("Payment must be approved before generating cards");
        }

        List<InvitationCard> generated = new ArrayList<>();

        for (InvitationDTO.ParticipantInput p : participants) {
            String uniqueId = generateUniqueInvitationId(event);

            String qrData = String.join("|",
                    uniqueId,
                    p.getParticipantName() != null ? p.getParticipantName() : "Guest",
                    event.getEventName(),
                    event.getEventDate().toString(),
                    event.getVenue()
            );

            String qrPath = null;
            try {
                qrPath = qrCodeService.generateQrCode(qrData, uniqueId);
            } catch (Exception e) {
                log.error("QR generation failed for {}: {}", uniqueId, e.getMessage());
            }

            InvitationCard card = InvitationCard.builder()
                    .invitationId(uniqueId)
                    .participantName(p.getParticipantName())
                    .participantEmail(p.getEmail())
                    .participantPhone(p.getPhone())
                    .qrCodePath(qrPath)
                    .emailStatus(InvitationStatus.PENDING)
                    .smsStatus(InvitationStatus.PENDING)
                    .qrScanned(false)
                    .event(event)
                    .build();

            InvitationCard saved = cardRepository.save(card);

            try {
                String pdfPath = pdfCardService.generateInvitationCard(saved, event);
                saved.setCardPdfPath(pdfPath);
                cardRepository.save(saved);
            } catch (Exception e) {
                log.error("PDF generation failed for {}: {}", uniqueId, e.getMessage());
            }

            generated.add(saved);
        }

        log.info("Generated {} invitation cards for event: {}", generated.size(), event.getEventName());

        User organizer = event.getOrganizer();
        try {
            emailService.sendEventApprovalEmail(
                    organizer.getFullName(), organizer.getEmail(),
                    event.getEventName(),
                    generated.size() + " Invitation Cards Generated",
                    "Your " + generated.size() + " invitation cards are now ready.");
        } catch (Exception e) {
            log.error("Email notification failed: {}", e.getMessage());
        }
        try {
            smsService.sendEventApprovalSms(organizer, event.getEventName(),
                    "Invitations Ready (" + generated.size() + " cards generated)");
        } catch (Exception e) {
            log.error("SMS notification failed: {}", e.getMessage());
        }

        return generated.stream()
                .map(c -> toResponseWithEvent(c, event))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORGANIZER — Send single card
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public InvitationDTO.CardResponse sendSingle(InvitationDTO.SingleSendRequest request) {
        InvitationCard card = cardRepository.findById(request.getCardId())
                .orElseThrow(() -> new RuntimeException("Card not found: " + request.getCardId()));

        // Eagerly load event
        Event event = eventRepository.findById(card.getEvent().getId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (request.isSendEmail() && card.getParticipantEmail() != null
                && !card.getParticipantEmail().isBlank()) {
            try {
                emailService.sendInvitationWithCard(
                        card.getParticipantName(), card.getParticipantEmail(),
                        card.getInvitationId(), event.getVenue(),
                        event.getEventDate().toString(), event.getStartTime().toString(),
                        event.getDescription(), card.getCardPdfPath(), event.getEventName());
                card.setEmailStatus(InvitationStatus.SENT);
                card.setEmailSentAt(LocalDateTime.now());
            } catch (Exception e) {
                card.setEmailStatus(InvitationStatus.FAILED);
                log.error("Email send failed for card {}: {}", card.getInvitationId(), e.getMessage());
            }
        }

        if (request.isSendSms() && card.getParticipantPhone() != null
                && !card.getParticipantPhone().isBlank()) {
            try {
                smsService.sendInvitationSms(
                        card.getParticipantPhone(), card.getParticipantName(),
                        event.getEventName(), event.getVenue(),
                        event.getEventDate().toString(), card.getInvitationId());
                card.setSmsStatus(InvitationStatus.SENT);
                card.setSmsSentAt(LocalDateTime.now());
            } catch (Exception e) {
                card.setSmsStatus(InvitationStatus.FAILED);
                log.error("SMS send failed for card {}: {}", card.getInvitationId(), e.getMessage());
            }
        }

        InvitationCard saved = cardRepository.save(card);
        return toResponseWithEvent(saved, event);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORGANIZER — Bulk send
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> sendBulk(InvitationDTO.BulkSendRequest request) {
        List<InvitationCard> cards;

        if (request.getCardIds() != null && !request.getCardIds().isEmpty()) {
            cards = cardRepository.findAllById(request.getCardIds());
        } else {
            cards = cardRepository.findByEvent_Id(request.getEventId());
        }

        int emailSent = 0, smsSent = 0, failed = 0;

        for (InvitationCard card : cards) {
            // Eagerly load event for each card
            Event event = eventRepository.findById(card.getEvent().getId()).orElse(null);
            if (event == null) continue;

            if (request.isSendEmail() && card.getParticipantEmail() != null
                    && !card.getParticipantEmail().isBlank()) {
                try {
                    emailService.sendInvitationWithCard(
                            card.getParticipantName(), card.getParticipantEmail(),
                            card.getInvitationId(), event.getVenue(),
                            event.getEventDate().toString(), event.getStartTime().toString(),
                            event.getDescription(), card.getCardPdfPath(), event.getEventName());
                    card.setEmailStatus(InvitationStatus.SENT);
                    card.setEmailSentAt(LocalDateTime.now());
                    emailSent++;
                } catch (Exception e) {
                    card.setEmailStatus(InvitationStatus.FAILED);
                    failed++;
                    log.error("Bulk email failed for {}: {}", card.getInvitationId(), e.getMessage());
                }
            }

            if (request.isSendSms() && card.getParticipantPhone() != null
                    && !card.getParticipantPhone().isBlank()) {
                try {
                    smsService.sendInvitationSms(
                            card.getParticipantPhone(), card.getParticipantName(),
                            event.getEventName(), event.getVenue(),
                            event.getEventDate().toString(), card.getInvitationId());
                    card.setSmsStatus(InvitationStatus.SENT);
                    card.setSmsSentAt(LocalDateTime.now());
                    smsSent++;
                } catch (Exception e) {
                    card.setSmsStatus(InvitationStatus.FAILED);
                    failed++;
                }
            }

            cardRepository.save(card);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", cards.size());
        result.put("emailSent", emailSent);
        result.put("smsSent", smsSent);
        result.put("failed", failed);
        result.put("message", "Bulk send completed");
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parse Excel participants
    // ─────────────────────────────────────────────────────────────────────────

    public List<InvitationDTO.ParticipantInput> parseExcelParticipants(MultipartFile file)
            throws IOException {
        List<InvitationDTO.ParticipantInput> participants = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name  = getCellString(row, 0);
                String email = getCellString(row, 1);
                String phone = getCellString(row, 2);

                if ((email != null && !email.isBlank()) || (phone != null && !phone.isBlank())) {
                    InvitationDTO.ParticipantInput p = new InvitationDTO.ParticipantInput();
                    p.setParticipantName(name != null ? name : "Guest");
                    p.setEmail(email);
                    p.setPhone(phone);
                    participants.add(p);
                }
            }
        }
        return participants;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QR scan — fixed lazy loading by eagerly loading event
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public InvitationDTO.ScanResponse scanQr(String invitationId) {
        InvitationDTO.ScanResponse response = new InvitationDTO.ScanResponse();
        response.setInvitationId(invitationId);

        if (invitationId == null || invitationId.isBlank()) {
            response.setStatus("INVALID");
            response.setMessage("No invitation ID provided.");
            return response;
        }

        // Handle QR data that may be pipe-separated: "INVID|name|event|date|venue"
        String cleanId = invitationId.contains("|")
                ? invitationId.split("\\|")[0].trim()
                : invitationId.trim();

        Optional<InvitationCard> opt = cardRepository.findByInvitationId(cleanId);

        if (opt.isEmpty()) {
            response.setStatus("INVALID");
            response.setMessage("Ticket not found. Invalid QR code.");
            return response;
        }

        InvitationCard card = opt.get();

        // ← KEY FIX: eagerly load event by ID to avoid lazy proxy issues
        Event event = eventRepository.findById(card.getEvent().getId()).orElse(null);
        String eventName = event != null ? event.getEventName() : "Unknown Event";

        if (Boolean.TRUE.equals(card.getQrScanned())) {
            response.setStatus("ALREADY_SCANNED");
            response.setMessage("This ticket has already been scanned for entry.");
            response.setParticipantName(card.getParticipantName());
            response.setEventName(eventName);
            response.setScannedAt(card.getScannedAt() != null
                    ? card.getScannedAt().toString() : null);
            return response;
        }

        // Valid — mark as scanned
        card.setQrScanned(true);
        card.setScannedAt(LocalDateTime.now());
        //card.setEmailStatus(InvitationStatus.OPENED);
        cardRepository.save(card);

        response.setStatus("VALID");
        response.setMessage("Valid ticket! Entry granted.");
        response.setParticipantName(card.getParticipantName());
        response.setEventName(eventName);
        response.setScannedAt(card.getScannedAt().toString());
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InvitationDTO.CardResponse> getCardsByEvent(Long eventId) {
        // Eagerly load event once
        Event event = eventRepository.findById(eventId).orElse(null);
        return cardRepository.findByEvent_IdOrderByCreatedAtDesc(eventId)
                .stream()
                .map(c -> toResponseWithEvent(c, event))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvitationDTO.StatsResponse getStatsByEvent(Long eventId) {
        List<InvitationCard> cards = cardRepository.findByEvent_Id(eventId);
        InvitationDTO.StatsResponse stats = new InvitationDTO.StatsResponse();
        stats.setTotal(cards.size());
        stats.setEmailSent(cards.stream().filter(c ->
                c.getEmailStatus() == InvitationStatus.SENT
                        || c.getEmailStatus() == InvitationStatus.OPENED).count());
        stats.setSmsSent(cards.stream().filter(c ->
                c.getSmsStatus() == InvitationStatus.SENT
                        || c.getSmsStatus() == InvitationStatus.OPENED).count());
        stats.setDelivered(cards.stream().filter(c ->
                c.getEmailStatus() == InvitationStatus.DELIVERED
                        || c.getSmsStatus() == InvitationStatus.DELIVERED).count());
        stats.setOpened(cards.stream().filter(c ->
                c.getEmailStatus() == InvitationStatus.OPENED).count());
        stats.setPending(cards.stream().filter(c ->
                c.getEmailStatus() == InvitationStatus.PENDING
                        && c.getSmsStatus() == InvitationStatus.PENDING).count());
        stats.setScanned(cards.stream().filter(c ->
                Boolean.TRUE.equals(c.getQrScanned())).count());
        return stats;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update card participant details
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public InvitationDTO.CardResponse updateCardParticipant(Long cardId, Map<String, String> body) {
        InvitationCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        if (body.containsKey("participantName"))
            card.setParticipantName(body.get("participantName"));
        if (body.containsKey("participantEmail"))
            card.setParticipantEmail(body.get("participantEmail"));
        if (body.containsKey("participantPhone"))
            card.setParticipantPhone(body.get("participantPhone"));

        InvitationCard saved = cardRepository.save(card);

        // Eagerly load event for response
        Event event = eventRepository.findById(saved.getEvent().getId()).orElse(null);
        return toResponseWithEvent(saved, event);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String generateUniqueInvitationId(Event event) {
        String prefix = event.getEventName().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (prefix.length() > 6) prefix = prefix.substring(0, 6);
        return prefix + "-" + event.getId() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateSequentialInvitationId(Event event, int index) {
        String prefix = event.getEventName().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (prefix.length() > 5) prefix = prefix.substring(0, 5);
        return prefix + "-" + event.getId() + "-" + String.format("%04d", index);
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> null;
        };
    }

    /**
     * Convert InvitationCard to response DTO using a pre-loaded Event.
     * This avoids lazy loading issues by never accessing card.getEvent() directly.
     */
    public InvitationDTO.CardResponse toResponseWithEvent(InvitationCard c, Event event) {
        InvitationDTO.CardResponse r = new InvitationDTO.CardResponse();
        r.setId(c.getId());
        r.setInvitationId(c.getInvitationId());
        r.setParticipantName(c.getParticipantName());
        r.setParticipantEmail(c.getParticipantEmail());
        r.setParticipantPhone(c.getParticipantPhone());
        r.setEmailStatus(c.getEmailStatus());
        r.setSmsStatus(c.getSmsStatus());
        r.setEmailSentAt(c.getEmailSentAt() != null ? c.getEmailSentAt().toString() : null);
        r.setSmsSentAt(c.getSmsSentAt() != null ? c.getSmsSentAt().toString() : null);
        r.setQrScanned(c.getQrScanned());
        r.setScannedAt(c.getScannedAt() != null ? c.getScannedAt().toString() : null);
        r.setCardPdfPath(c.getCardPdfPath());
        r.setQrCodePath(c.getQrCodePath());
        r.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        // Use pre-loaded event — never touches lazy proxy
        if (event != null) {
            r.setEventId(event.getId());
            r.setEventName(event.getEventName());
        }
        return r;
    }

    /**
     * Legacy toResponse — kept for compatibility but loads event safely.
     */
    public InvitationDTO.CardResponse toResponse(InvitationCard c) {
        Event event = null;
        try {
            if (c.getEvent() != null) {
                event = eventRepository.findById(c.getEvent().getId()).orElse(null);
            }
        } catch (Exception e) {
            log.warn("Could not load event for card {}: {}", c.getId(), e.getMessage());
        }
        return toResponseWithEvent(c, event);
    }
}