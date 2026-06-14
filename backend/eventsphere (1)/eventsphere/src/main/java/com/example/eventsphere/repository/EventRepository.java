package com.example.eventsphere.repository;
import com.example.eventsphere.entity.Event;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByOrganizer(User organizer);
    List<Event> findByStatus(EventStatus status);
    List<Event> findByOrganizerAndStatus(User organizer, EventStatus status);
    long countByStatus(EventStatus status);
    List<Event> findAllByOrderByCreatedAtDesc();
    List<Event> findByOrganizerOrderByCreatedAtDesc(User organizer);
    @Query("SELECT SUM(e.totalAmount) FROM Event e WHERE e.status = com.example.eventsphere.enums.EventStatus.APPROVED")
    Double sumApprovedRevenue();
}
