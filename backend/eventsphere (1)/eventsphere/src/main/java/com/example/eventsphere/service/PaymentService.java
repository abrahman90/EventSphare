package com.example.eventsphere.service;

import com.example.eventsphere.dto.PaymentDTO;
import com.example.eventsphere.entity.Payment;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.PaymentStatus;
import com.example.eventsphere.repository.PaymentRepository;
import com.example.eventsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public List<PaymentDTO.PaymentResponse> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PaymentDTO.PaymentResponse getPaymentByEventId(Long eventId) {
        Payment p = paymentRepository.findByEvent_Id(eventId)
                .orElseThrow(() -> new RuntimeException("Payment not found for event: " + eventId));
        return toResponse(p);
    }

    @Transactional
    public PaymentDTO.PaymentResponse uploadProof(Long paymentId, MultipartFile file, String organizerEmail) throws IOException {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        Path uploadPath = Paths.get(uploadDir, "payments");
        Files.createDirectories(uploadPath);

        // Save with UUID prefix to avoid filename collisions
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.write(filePath, file.getBytes());

        payment.setProofFilePath(filePath.toString());
        payment.setProofFileName(filename);  // store the real filename
        payment.setStatus(PaymentStatus.UNDER_REVIEW);
        Payment saved = paymentRepository.save(payment);
        return toResponse(saved);
    }

    @Transactional
    public PaymentDTO.PaymentResponse verifyPayment(Long paymentId, PaymentDTO.VerifyRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(request.getStatus());
        payment.setAdminRemarks(request.getAdminRemarks());
        if (request.getStatus() == PaymentStatus.APPROVED) {
            payment.setVerifiedAt(LocalDateTime.now());
        }
        Payment saved = paymentRepository.save(payment);

        User organizer = payment.getEvent().getOrganizer();
        String statusLabel = request.getStatus().name().replace("_", " ");
        emailService.sendPaymentVerificationEmail(
                organizer.getFullName(), organizer.getEmail(),
                payment.getEvent().getEventName(), statusLabel, request.getAdminRemarks());
        smsService.sendPaymentStatusSms(organizer, payment.getEvent().getEventName(), statusLabel);
        return toResponse(saved);
    }

    private PaymentDTO.PaymentResponse toResponse(Payment p) {
        PaymentDTO.PaymentResponse r = new PaymentDTO.PaymentResponse();
        r.setId(p.getId());
        r.setInvoiceNumber(p.getInvoiceNumber());
        r.setAmount(p.getAmount());
        r.setStatus(p.getStatus());

        // Always derive filename from actual file path — fixes all existing broken records
        if (p.getProofFilePath() != null) {
            Path filePath = Paths.get(p.getProofFilePath());
            r.setProofFileName(filePath.getFileName().toString());
            r.setHasProof(true);
        } else {
            r.setProofFileName(null);
            r.setHasProof(false);
        }

        r.setAdminRemarks(p.getAdminRemarks());
        r.setVerifiedAt(p.getVerifiedAt() != null ? p.getVerifiedAt().toString() : null);
        r.setCreatedAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        if (p.getEvent() != null) {
            r.setEventId(p.getEvent().getId());
            r.setEventName(p.getEvent().getEventName());
            if (p.getEvent().getOrganizer() != null) {
                r.setOrganizerName(p.getEvent().getOrganizer().getFullName());
            }
        }
        return r;
    }
}