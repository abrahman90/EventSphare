package com.example.eventsphere.controller;

import com.example.eventsphere.dto.*;
import com.example.eventsphere.entity.InvitationCard;
import com.example.eventsphere.entity.NotificationLog;
import com.example.eventsphere.repository.EventRepository;
import com.example.eventsphere.repository.NotificationLogRepository;
import com.example.eventsphere.service.*;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.io.ByteArrayOutputStream;

import lombok.Setter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Getter
@Setter
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final EventService eventService;
    private final PaymentService paymentService;
    private final InvitationService invitationService;
    private final EventRepository eventRepository;
    private final NotificationLogRepository notificationLogRepository;

    // ── Dashboard ──────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", userService.getDashboardStats().get("totalUsers"));
        data.put("totalOrganizers", userService.getDashboardStats().get("totalOrganizers"));
        data.put("pendingApprovals", userService.getDashboardStats().get("pendingApprovals"));
        data.put("totalEvents", eventRepository.count());
        data.put("pendingEvents", eventRepository.countByStatus(
                com.example.eventsphere.enums.EventStatus.PENDING));
        data.put("approvedEvents", eventRepository.countByStatus(
                com.example.eventsphere.enums.EventStatus.APPROVED));
        data.put("totalRevenue", eventRepository.sumApprovedRevenue());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // ── User Management ────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AuthDTO.UserResponse>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers()));
    }

    @GetMapping("/users/organizers")
    public ResponseEntity<ApiResponse<List<AuthDTO.UserResponse>>> getOrganizers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getOrganizers()));
    }

    @GetMapping("/users/pending")
    public ResponseEntity<ApiResponse<List<AuthDTO.UserResponse>>> getPendingOrganizers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getPendingOrganizers()));
    }

    @PostMapping("/users/{userId}/approve")
    public ResponseEntity<ApiResponse<AuthDTO.UserResponse>> approveOrganizer(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Account approved",
                userService.approveOrganizer(userId)));
    }

    @PostMapping("/users/{userId}/deactivate")
    public ResponseEntity<ApiResponse<AuthDTO.UserResponse>> deactivateUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("User deactivated",
                userService.deactivateUser(userId)));
    }

    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<ApiResponse<AuthDTO.UserResponse>> activateUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("User activated",
                userService.activateUser(userId)));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
    }

    @PatchMapping("/users/{userId}/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        userService.resetPassword(userId, body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.ok("Password reset and email sent", null));
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AuthDTO.UserResponse>> updateUser(
            @PathVariable Long userId,
            @RequestBody Map<String, String> updates) {
        return ResponseEntity.ok(ApiResponse.ok("User updated",
                userService.updateUser(userId, updates)));
    }

    // ── Event Management ──────────────────────────────────────────────────────

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<EventDTO.EventResponse>>> getEvents() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getAllEvents()));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<ApiResponse<EventDTO.EventResponse>> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEventById(id)));
    }

    @PatchMapping("/events/{id}/status")
    public ResponseEntity<ApiResponse<EventDTO.EventResponse>> updateEventStatus(
            @PathVariable Long id,
            @Valid @RequestBody EventDTO.StatusUpdateRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated",
                eventService.updateStatus(id, request, userDetails.getUsername())));
    }

    // ── Payment Management ────────────────────────────────────────────────────

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<PaymentDTO.PaymentResponse>>> getPayments() {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getAllPayments()));
    }

    @PostMapping("/payments/{paymentId}/verify")
    public ResponseEntity<ApiResponse<PaymentDTO.PaymentResponse>> verifyPayment(
            @PathVariable Long paymentId,
            @RequestBody PaymentDTO.VerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Payment verified",
                paymentService.verifyPayment(paymentId, request)));
    }

    // ── Invitation / Card Management ──────────────────────────────────────────

    /**
     * Admin generates invitation cards based on event capacity.
     * Returns a ZIP file for immediate browser download.
     * Uses GET so no CSRF/body issues.
     */
    @GetMapping("/invitations/generate/{eventId}")
    public ResponseEntity<ByteArrayResource> generateCards(
            @PathVariable Long eventId) throws IOException {

        // Generate all cards based on event capacity
        List<InvitationCard> cards = invitationService.generateCardsForEvent(eventId);

        // Build ZIP in memory
        byte[] zipBytes = buildZipFromCards(cards);

        String zipName = "invitations-event-" + eventId + ".zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + zipName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zipBytes.length)
                .body(new ByteArrayResource(zipBytes));
    }

    /**
     * Download ZIP of already-generated cards for an event.
     */
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
        String zipName = "invitations-event-" + eventId + ".zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + zipName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zipBytes.length)
                .body(new ByteArrayResource(zipBytes));
    }

    @GetMapping("/invitations/event/{eventId}")
    public ResponseEntity<ApiResponse<List<InvitationDTO.CardResponse>>> getCardsByEvent(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.getCardsByEvent(eventId)));
    }

    @GetMapping("/invitations/stats/{eventId}")
    public ResponseEntity<ApiResponse<InvitationDTO.StatsResponse>> getStats(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.getStatsByEvent(eventId)));
    }

    // ── Notification Log ──────────────────────────────────────────────────────

// ── Notification Log ──────────────────────────────────────────────────────

    @GetMapping("/notifications/log")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getNotificationLog() {
        try {
            List<NotificationLog> logs = notificationLogRepository
                    .findTop100ByOrderByCreatedAtDesc();
            return ResponseEntity.ok(ApiResponse.ok(logs));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
    }

    @GetMapping("/notifications/failed")
    public ResponseEntity<ApiResponse<List<NotificationLog>>> getFailedNotifications() {
        try {
            List<NotificationLog> logs = notificationLogRepository.findByDelivered(false);
            return ResponseEntity.ok(ApiResponse.ok(logs));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private byte[] buildZipFromCards(List<InvitationCard> cards) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (InvitationCard card : cards) {
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
        return baos.toByteArray();
    }
}