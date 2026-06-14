package com.example.eventsphere.entity;

import com.example.eventsphere.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column
    private String recipient;

    @Column
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column
    private String eventName;

    @Column
    @Builder.Default
    private Boolean delivered = false;

    @Column
    private String errorMessage;

    @JsonIgnore                          // ← prevents lazy-load serialization crash
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @CreationTimestamp
    private LocalDateTime createdAt;
}