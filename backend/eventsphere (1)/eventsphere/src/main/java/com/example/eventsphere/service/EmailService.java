package com.example.eventsphere.service;

import com.example.eventsphere.entity.NotificationLog;
import com.example.eventsphere.enums.NotificationType;
import com.example.eventsphere.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.io.File;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository notificationLogRepository;

    @Value("${spring.mail.from}")
    private String fromEmail;

    // ─── Public API — all accept plain strings, no entity objects ─────────────

    @Async
    public void sendEventApprovalEmail(String organizerName, String organizerEmail,
                                       String eventName, String status, String remarks) {
        String subject = "EventSphere – Event " + status + ": " + eventName;
        String html = buildStatusEmailHtml(organizerName, eventName, status, remarks);
        sendHtmlEmail(organizerEmail, subject, html, eventName, true, null);
    }

    @Async
    public void sendPaymentVerificationEmail(String organizerName, String organizerEmail,
                                             String eventName, String paymentStatus, String remarks) {
        String subject = "EventSphere – Payment " + paymentStatus + " for " + eventName;
        String html = buildPaymentEmailHtml(organizerName, eventName, paymentStatus, remarks);
        sendHtmlEmail(organizerEmail, subject, html, eventName, true, null);
    }

    @Async
    public void sendWelcomeEmail(String userName, String userEmail) {
        String subject = "Welcome to EventSphere – Registration Received";
        String html = buildWelcomeEmailHtml(userName, userEmail);
        sendHtmlEmail(userEmail, subject, html, "System", true, null);
    }

    @Async
    public void sendAccountApprovalEmail(String userName, String userEmail) {
        String subject = "EventSphere – Your Account is Approved!";
        String html = buildAccountApprovalHtml(userName);
        sendHtmlEmail(userEmail, subject, html, "System", true, null);
    }

    @Async
    public void sendInvitationWithCard(String participantName, String participantEmail,
                                       String invitationId, String venue, String eventDate,
                                       String startTime, String description,
                                       String pdfPath, String eventName) {
        String subject = "You're Invited – " + eventName;
        String html = buildInvitationEmailHtml(participantName, eventName, invitationId,
                venue, eventDate, startTime, description);
        sendHtmlEmailWithAttachment(participantEmail, subject, html, pdfPath, eventName, invitationId);
    }

    // ─── Private send helpers ──────────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String html,
                               String eventName, boolean delivered, String attachmentPath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "EventSphere");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            logNotification(to, subject, html, eventName, true, null);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            logNotification(to, subject, html, eventName, false, e.getMessage());
        }
    }

    private void sendHtmlEmailWithAttachment(String to, String subject, String html,
                                             String attachmentPath, String eventName, String invId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "EventSphere");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            if (attachmentPath != null) {
                FileSystemResource file = new FileSystemResource(new File(attachmentPath));
                helper.addAttachment("InvitationCard_" + invId + ".pdf", file);
            }
            mailSender.send(message);
            logNotification(to, subject, "Invitation email with PDF card", eventName, true, null);
            log.info("Invitation email with attachment sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send invitation email to {}: {}", to, e.getMessage());
            logNotification(to, subject, "Invitation email with PDF card", eventName, false, e.getMessage());
        }
    }

    private void logNotification(String to, String subject, String content,
                                 String eventName, boolean delivered, String error) {
        try {
            NotificationLog notif = NotificationLog.builder()
                    .type(NotificationType.EMAIL)
                    .recipient(to)
                    .subject(subject)
                    .content(content != null && content.length() > 500 ? content.substring(0, 500) : content)
                    .eventName(eventName)
                    .delivered(delivered)
                    .errorMessage(error)
                    .build();
            notificationLogRepository.save(notif);
        } catch (Exception e) {
            log.warn("Failed to log notification: {}", e.getMessage());
        }
    }

    // ─── HTML Templates ────────────────────────────────────────────────────────

    private String buildStatusEmailHtml(String name, String eventName, String status, String remarks) {
        String color = status.contains("Approved") ? "#059669" : "#dc2626";
        String icon  = status.contains("Approved") ? "✅" : "❌";
        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#f8fafc;margin:0;padding:20px'>" +
                "<div style='max-width:580px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,.1)'>" +
                "<div style='background:#1a56db;padding:28px;text-align:center'>" +
                "<h1 style='color:#fff;margin:0;font-size:22px'>EventSphere</h1>" +
                "<p style='color:rgba(255,255,255,.8);margin:6px 0 0;font-size:13px'>Smart Event Management System</p></div>" +
                "<div style='padding:32px'>" +
                "<h2 style='color:#0f172a;margin:0 0 16px'>Hello, " + name + "!</h2>" +
                "<p style='color:#475569;font-size:15px'>Your event <strong>" + eventName + "</strong> has been " +
                "<span style='color:" + color + ";font-weight:600'>" + icon + " " + status + "</span>.</p>" +
                (remarks != null && !remarks.isEmpty()
                        ? "<div style='background:#f1f5f9;border-radius:8px;padding:14px;margin:16px 0'>" +
                          "<p style='margin:0;color:#475569;font-size:14px'><strong>Admin Remarks:</strong> " + remarks + "</p></div>"
                        : "") +
                "<p style='color:#475569;font-size:14px'>Please log in to EventSphere for more details.</p>" +
                "<a href='http://localhost:5173' style='display:inline-block;background:#1a56db;color:#fff;padding:12px 24px;" +
                "border-radius:8px;text-decoration:none;font-weight:600;margin-top:8px'>Open EventSphere</a>" +
                "</div></div></body></html>";
    }

    private String buildPaymentEmailHtml(String name, String eventName, String status, String remarks) {
        String color = status.contains("Approved") ? "#059669" : "#dc2626";
        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#f8fafc;padding:20px'>" +
                "<div style='max-width:580px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,.1)'>" +
                "<div style='background:#1a56db;padding:28px;text-align:center'><h1 style='color:#fff;margin:0'>EventSphere</h1></div>" +
                "<div style='padding:32px'><h2 style='color:#0f172a'>Hello, " + name + "!</h2>" +
                "<p style='color:#475569'>Your payment for <strong>" + eventName + "</strong> has been " +
                "<span style='color:" + color + ";font-weight:700'>" + status + "</span>.</p>" +
                (remarks != null
                        ? "<div style='background:#f1f5f9;border-radius:8px;padding:14px;margin:16px 0'>" +
                          "<p style='margin:0;color:#475569'><strong>Remarks:</strong> " + remarks + "</p></div>"
                        : "") +
                "<a href='http://localhost:5173' style='display:inline-block;background:#1a56db;color:#fff;padding:12px 24px;" +
                "border-radius:8px;text-decoration:none;font-weight:600;margin-top:8px'>Open EventSphere</a>" +
                "</div></div></body></html>";
    }

    private String buildWelcomeEmailHtml(String name, String email) {
        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#f8fafc;padding:20px'>" +
                "<div style='max-width:580px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,.1)'>" +
                "<div style='background:#1a56db;padding:28px;text-align:center'><h1 style='color:#fff;margin:0'>Welcome to EventSphere!</h1></div>" +
                "<div style='padding:32px'><h2 style='color:#0f172a'>Hello, " + name + "!</h2>" +
                "<p style='color:#475569'>Your registration request has been received. An administrator will review and approve your account shortly.</p>" +
                "<p style='color:#475569'>Your email: <strong>" + email + "</strong></p>" +
                "<p style='color:#94a3b8;font-size:13px'>You will receive a confirmation email once your account is approved.</p>" +
                "</div></div></body></html>";
    }

    private String buildAccountApprovalHtml(String name) {
        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#f8fafc;padding:20px'>" +
                "<div style='max-width:580px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,.1)'>" +
                "<div style='background:#059669;padding:28px;text-align:center'><h1 style='color:#fff;margin:0'>✅ Account Approved!</h1></div>" +
                "<div style='padding:32px'><h2 style='color:#0f172a'>Hello, " + name + "!</h2>" +
                "<p style='color:#475569;font-size:15px'>Great news! Your EventSphere organizer account has been approved. You can now log in and start creating events.</p>" +
                "<a href='http://localhost:5173' style='display:inline-block;background:#059669;color:#fff;padding:14px 28px;" +
                "border-radius:8px;text-decoration:none;font-weight:700;margin-top:8px'>Log In Now</a>" +
                "</div></div></body></html>";
    }

    private String buildInvitationEmailHtml(String participantName, String eventName, String invId,
                                            String venue, String date, String time, String description) {
        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#f8fafc;padding:20px'>" +
                "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,.1)'>" +
                "<div style='background:linear-gradient(135deg,#1a56db,#7c3aed);padding:32px;text-align:center'>" +
                "<h1 style='color:#fff;margin:0;font-size:26px'>You're Invited!</h1>" +
                "<p style='color:rgba(255,255,255,.8);margin:8px 0 0;font-size:15px'>" + eventName + "</p></div>" +
                "<div style='padding:32px'>" +
                "<p style='color:#475569;font-size:16px'>Dear <strong>" + participantName + "</strong>,</p>" +
                "<p style='color:#475569'>You are cordially invited to attend <strong>" + eventName + "</strong>.</p>" +
                "<div style='background:#f1f5f9;border-radius:10px;padding:18px;margin:20px 0'>" +
                "<p style='margin:0 0 8px;color:#475569'><strong>📍 Venue:</strong> " + venue + "</p>" +
                "<p style='margin:0 0 8px;color:#475569'><strong>📅 Date:</strong> " + date + "</p>" +
                "<p style='margin:0 0 8px;color:#475569'><strong>🕐 Time:</strong> " + time + "</p>" +
                "<p style='margin:0;color:#475569'><strong>🎟️ Invitation ID:</strong> " + invId + "</p>" +
                "</div>" +
                "<p style='color:#475569;font-size:14px'>" + (description != null ? description : "") + "</p>" +
                "<div style='background:#fef3c7;border-radius:8px;padding:14px;margin:16px 0;border-left:4px solid #d97706'>" +
                "<p style='margin:0;color:#92400e;font-size:14px'>📎 Your personalized invitation card with QR code is attached as a PDF. Please present it at the entrance.</p>" +
                "</div>" +
                "</div></div></body></html>";
    }
}