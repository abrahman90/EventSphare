package com.example.eventsphere.controller;

import com.example.eventsphere.dto.*;
import com.example.eventsphere.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@Slf4j
@RequestMapping("/api/organizer")
@PreAuthorize("hasRole('ORGANIZER')")
@RequiredArgsConstructor
public class OrganizerController {

    private final EventService eventService;
    private final PaymentService paymentService;
    private final InvitationService invitationService;
    private final UserService userService;

    // ── Dashboard ──────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<EventDTO.EventResponse> events = eventService.getEventsByOrganizer(userDetails.getUsername());
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("totalEvents", events.size());
        data.put("pendingEvents", events.stream()
                .filter(e -> e.getStatus() == com.example.eventsphere.enums.EventStatus.PENDING).count());
        data.put("approvedEvents", events.stream()
                .filter(e -> e.getStatus() == com.example.eventsphere.enums.EventStatus.APPROVED).count());
        data.put("recentEvents", events.stream().limit(5).toList());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // ── Profile ────────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AuthDTO.UserResponse>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(userDetails.getUsername())));
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<EventDTO.EventResponse>> createEvent(
            @Valid @RequestBody EventDTO.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        EventDTO.EventResponse event = eventService.createEvent(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Event created and submitted for review", event));
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<EventDTO.EventResponse>>> getMyEvents(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                eventService.getEventsByOrganizer(userDetails.getUsername())));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<ApiResponse<EventDTO.EventResponse>> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEventById(id)));
    }

    // ── Payments ──────────────────────────────────────────────────────────────

    @GetMapping("/payments/event/{eventId}")
    public ResponseEntity<ApiResponse<PaymentDTO.PaymentResponse>> getPayment(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentByEventId(eventId)));
    }

    @PostMapping("/payments/{paymentId}/upload-proof")
    public ResponseEntity<ApiResponse<PaymentDTO.PaymentResponse>> uploadProof(
            @PathVariable Long paymentId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok("Proof uploaded successfully",
                paymentService.uploadProof(paymentId, file, userDetails.getUsername())));
    }

    // ── Invitations ───────────────────────────────────────────────────────────

    @GetMapping("/invitations/event/{eventId}")
    public ResponseEntity<ApiResponse<List<InvitationDTO.CardResponse>>> getMyCards(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.getCardsByEvent(eventId)));
    }

    @PostMapping("/invitations/send-single")
    public ResponseEntity<ApiResponse<InvitationDTO.CardResponse>> sendSingle(
            @RequestBody InvitationDTO.SingleSendRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Invitation sent",
                invitationService.sendSingle(request)));
    }

    @PostMapping("/invitations/send-bulk")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendBulk(
            @RequestBody InvitationDTO.BulkSendRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Bulk send complete",
                invitationService.sendBulk(request)));
    }

    @PostMapping("/invitations/send-from-excel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendFromExcel(
            @RequestParam Long eventId,
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "true") boolean sendEmail,
            @RequestParam(defaultValue = "false") boolean sendSms) throws IOException {

        List<InvitationDTO.ParticipantInput> participants =
                invitationService.parseExcelParticipants(file);

        InvitationDTO.BulkSendRequest bulkReq = new InvitationDTO.BulkSendRequest();
        bulkReq.setEventId(eventId);
        bulkReq.setSendEmail(sendEmail);
        bulkReq.setSendSms(sendSms);

        List<InvitationDTO.CardResponse> cards = invitationService.getCardsByEvent(eventId);
        List<Long> matchedIds = cards.stream()
                .filter(c -> participants.stream().anyMatch(p ->
                        (p.getEmail() != null && p.getEmail().equalsIgnoreCase(c.getParticipantEmail())) ||
                                (p.getPhone() != null && p.getPhone().equals(c.getParticipantPhone()))))
                .map(InvitationDTO.CardResponse::getId)
                .toList();
        bulkReq.setCardIds(matchedIds.isEmpty() ? null : matchedIds);

        Map<String, Object> result = invitationService.sendBulk(bulkReq);
        result.put("excelRowsParsed", participants.size());
        return ResponseEntity.ok(ApiResponse.ok("Excel bulk send complete", result));
    }

    @GetMapping("/invitations/stats/{eventId}")
    public ResponseEntity<ApiResponse<InvitationDTO.StatsResponse>> getStats(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.getStatsByEvent(eventId)));
    }

//    @PostMapping("/invitations/scan")
//    public ResponseEntity<ApiResponse<InvitationDTO.ScanResponse>> scan(
//            @RequestBody InvitationDTO.ScanRequest request) {
//        return ResponseEntity.ok(ApiResponse.ok(
//                invitationService.scanQr(request.getInvitationId())));
//    }
@PostMapping("/invitations/scan")
public ResponseEntity<ApiResponse<InvitationDTO.ScanResponse>> scan(
        @RequestBody InvitationDTO.ScanRequest request) {
    try {
        return ResponseEntity.ok(ApiResponse.ok(
                invitationService.scanQr(request.getInvitationId())));
    } catch (Exception e) {
        log.error("Scan error: ", e);  // ← this will print full stack trace
        InvitationDTO.ScanResponse err = new InvitationDTO.ScanResponse();
        err.setStatus("INVALID");
        err.setMessage("Scan failed: " + e.getMessage());
        return ResponseEntity.ok(ApiResponse.ok(err));
    }
}

    @GetMapping("/invitations/download/{eventId}")
    public ResponseEntity<ByteArrayResource> downloadCards(
            @PathVariable Long eventId) throws IOException {

        List<InvitationDTO.CardResponse> cards = invitationService.getCardsByEvent(eventId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (InvitationDTO.CardResponse card : cards) {
                if (card.getCardPdfPath() != null) {
                    java.nio.file.Path pdfPath = Paths.get(card.getCardPdfPath());
                    if (Files.exists(pdfPath)) {
                        zos.putNextEntry(new ZipEntry(card.getInvitationId() + ".pdf"));
                        zos.write(Files.readAllBytes(pdfPath));
                        zos.closeEntry();
                    }
                }
            }
        }

        byte[] zipBytes = baos.toByteArray();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"invitations-event-" + eventId + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zipBytes.length)
                .body(new ByteArrayResource(zipBytes));
    }

    @PatchMapping("/invitations/cards/{cardId}/participant")
    public ResponseEntity<ApiResponse<InvitationDTO.CardResponse>> updateParticipant(
            @PathVariable Long cardId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("Updated",
                invitationService.updateCardParticipant(cardId, body)));
    }

    // ── Excel Template Download ───────────────────────────────────────────────

    @GetMapping("/invitations/excel-template")
    public ResponseEntity<ByteArrayResource> downloadExcelTemplate() throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Participants");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Headers row
            Row header = sheet.createRow(0);
            String[] columns = {"Participant Name *", "Email Address", "Phone Number"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 7000);
            }

            // Sample data rows
            String[][] samples = {
                    {"Alice Mwangi", "alice@example.com", "+255700111001"},
                    {"John Doe",     "john@example.com",  "+255700111002"},
                    {"Jane Smith",   "jane@example.com",  "+255700111003"},
            };

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < samples[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(samples[r][c]);
                    cell.setCellStyle(dataStyle);
                }
            }

            // Write to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            byte[] bytes = baos.toByteArray();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"participants-template.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(bytes.length)
                    .body(new ByteArrayResource(bytes));
        }
    }
}