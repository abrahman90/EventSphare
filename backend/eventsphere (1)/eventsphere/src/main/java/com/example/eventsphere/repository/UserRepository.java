package com.example.eventsphere.repository;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.AccountStatus;
import com.example.eventsphere.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByAccountStatus(AccountStatus status);
    long countByRole(Role role);
}
