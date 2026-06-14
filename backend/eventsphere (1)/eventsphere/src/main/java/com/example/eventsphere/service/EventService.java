package com.example.eventsphere.service;

import com.example.eventsphere.dto.EventDTO;
import com.example.eventsphere.entity.Event;
import com.example.eventsphere.entity.Payment;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.EventStatus;
import com.example.eventsphere.enums.PaymentStatus;
import com.example.eventsphere.repository.EventRepository;
import com.example.eventsphere.repository.InvitationCardRepository;
import com.example.eventsphere.repository.PaymentRepository;
import com.example.eventsphere.repository.UserRepository;
import com.example.eventsphere.util.PricingUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final InvitationCardRepository invitationCardRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    @Transactional
    public EventDTO.EventResponse createEvent(EventDTO.CreateRequest request, String organizerEmail) {
        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new RuntimeException("Organizer not found"));

        double total = PricingUtil.calculateTotal(request.getCategory(), request.getCapacity());

        Event event = Event.builder()
                .eventName(request.getEventName())
                .category(request.getCategory())
                .description(request.getDescription())
                .venue(request.getVenue())
                .eventDate(request.getEventDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .capacity(request.getCapacity())
                .contactInfo(request.getContactInfo())
                .selectedLayout(request.getSelectedLayout())
                .totalAmount(total)
                .status(EventStatus.PENDING)
                .organizer(organizer)
                .build();

        Event saved = eventRepository.save(event);

        // Create invoice
        String invoiceNumber = "INV-" + System.currentTimeMillis();
        Payment payment = Payment.builder()
                .invoiceNumber(invoiceNumber)
                .amount(total)
                .status(PaymentStatus.PENDING)
                .event(saved)
                .build();
        paymentRepository.save(payment);

        return toResponse(saved);
    }

    public List<EventDTO.EventResponse> getAllEvents() {
        return eventRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<EventDTO.EventResponse> getEventsByOrganizer(String email) {
        User organizer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return eventRepository.findByOrganizerOrderByCreatedAtDesc(organizer)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public EventDTO.EventResponse getEventById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public EventDTO.EventResponse updateStatus(Long eventId, EventDTO.StatusUpdateRequest request, String adminEmail) {
        Event event = findById(eventId);
        EventStatus oldStatus = event.getStatus();
        event.setStatus(request.getStatus());
        event.setAdminRemarks(request.getAdminRemarks());
        Event updated = eventRepository.save(event);

        // Notify organizer
        User organizer = event.getOrganizer();
        String statusLabel = request.getStatus().name().replace("_", " ");
        emailService.sendEventApprovalEmail(
                organizer.getFullName(), organizer.getEmail(),
                event.getEventName(), statusLabel, request.getAdminRemarks());
        smsService.sendEventApprovalSms(organizer, event.getEventName(), statusLabel);
        return toResponse(updated);
    }

    private Event findById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));
    }

    public EventDTO.EventResponse toResponse(Event event) {
        EventDTO.EventResponse r = new EventDTO.EventResponse();
        r.setId(event.getId());
        r.setEventName(event.getEventName());
        r.setCategory(event.getCategory());
        r.setDescription(event.getDescription());
        r.setVenue(event.getVenue());
        r.setEventDate(event.getEventDate());
        r.setStartTime(event.getStartTime());
        r.setEndTime(event.getEndTime());
        r.setCapacity(event.getCapacity());
        r.setContactInfo(event.getContactInfo());
        r.setStatus(event.getStatus());
        r.setSelectedLayout(event.getSelectedLayout());
        r.setTotalAmount(event.getTotalAmount());
        r.setAdminRemarks(event.getAdminRemarks());
        r.setCreatedAt(event.getCreatedAt() != null ? event.getCreatedAt().toString() : null);

        if (event.getOrganizer() != null) {
            r.setOrganizerName(event.getOrganizer().getFullName());
            r.setOrganizerEmail(event.getOrganizer().getEmail());
            r.setOrganizerId(event.getOrganizer().getId());
        }

        try {
            Payment pay = paymentRepository.findByEvent_Id(event.getId()).orElse(null);
            if (pay != null) {
                EventDTO.PaymentInfo pi = new EventDTO.PaymentInfo();
                pi.setId(pay.getId());
                pi.setInvoiceNumber(pay.getInvoiceNumber());
                pi.setAmount(pay.getAmount());
                pi.setStatus(pay.getStatus());
                pi.setHasProof(pay.getProofFilePath() != null);
                r.setPayment(pi);
            }
        } catch (Exception ignored) {}

        try {
            r.setGeneratedCards(invitationCardRepository.countByEvent_Id(event.getId()));
        } catch (Exception ignored) { r.setGeneratedCards(0L); }

        return r;
    }
}
