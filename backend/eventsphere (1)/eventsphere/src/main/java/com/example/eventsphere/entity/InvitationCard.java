package com.example.eventsphere.entity;

import com.example.eventsphere.enums.InvitationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "invitation_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invitationId;

    @Column
    private String participantName;

    @Column
    private String participantEmail;

    @Column
    private String participantPhone;

    @Column
    private String qrCodePath;

    @Column
    private String cardPdfPath;

    @Column
    private String cardImagePath;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InvitationStatus emailStatus = InvitationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InvitationStatus smsStatus = InvitationStatus.PENDING;

    @Column
    private LocalDateTime emailSentAt;

    @Column
    private LocalDateTime smsSentAt;

    @Column
    @Builder.Default
    private Boolean qrScanned = false;

    @Column
    private LocalDateTime scannedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Event event;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
