package com.example.eventsphere.repository;
import com.example.eventsphere.entity.Payment;
import com.example.eventsphere.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByEvent_Id(Long eventId);
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findAllByOrderByCreatedAtDesc();
    Optional<Payment> findByInvoiceNumber(String invoiceNumber);
}
