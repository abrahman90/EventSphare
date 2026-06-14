package com.example.eventsphere.repository;
import com.example.eventsphere.entity.InvitationCard;
import com.example.eventsphere.entity.Event;
import com.example.eventsphere.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface InvitationCardRepository extends JpaRepository<InvitationCard, Long> {

    List<InvitationCard> findByEvent(Event event);
    List<InvitationCard> findByEvent_Id(Long eventId);
    Optional<InvitationCard> findByInvitationId(String invitationId);
    long countByEvent_Id(Long eventId);
    long countByEvent_IdAndQrScanned(Long eventId, Boolean scanned);
    List<InvitationCard> findByEvent_IdAndEmailStatus(Long eventId, InvitationStatus status);
    List<InvitationCard> findByEvent_IdOrderByCreatedAtDesc(Long eventId);
}
