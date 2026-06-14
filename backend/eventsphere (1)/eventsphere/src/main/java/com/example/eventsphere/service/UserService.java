package com.example.eventsphere.service;

import com.example.eventsphere.dto.AuthDTO;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.AccountStatus;
import com.example.eventsphere.enums.Role;
import com.example.eventsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;

    public List<AuthDTO.UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<AuthDTO.UserResponse> getOrganizers() {
        return userRepository.findByRole(Role.ORGANIZER).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<AuthDTO.UserResponse> getPendingOrganizers() {
        return userRepository.findByAccountStatus(AccountStatus.PENDING_APPROVAL).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public AuthDTO.UserResponse approveOrganizer(Long userId) {
        User user = findById(userId);
        user.setAccountStatus(AccountStatus.ACTIVE);
        User saved = userRepository.save(user);
        emailService.sendAccountApprovalEmail(saved.getFullName(), saved.getEmail());
        smsService.sendAccountApprovalSms(saved);
        return toResponse(saved);
    }

    @Transactional
    public AuthDTO.UserResponse deactivateUser(Long userId) {
        User user = findById(userId);
        user.setAccountStatus(AccountStatus.INACTIVE);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public AuthDTO.UserResponse activateUser(Long userId) {
        User user = findById(userId);
        user.setAccountStatus(AccountStatus.ACTIVE);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Transactional
    public AuthDTO.UserResponse resetPassword(Long userId, String newPassword) {
        User user = findById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public AuthDTO.UserResponse updateUser(Long userId, Map<String, String> updates) {
        User user = findById(userId);
        if (updates.containsKey("fullName")) user.setFullName(updates.get("fullName"));
        if (updates.containsKey("phone")) user.setPhone(updates.get("phone"));
        if (updates.containsKey("organization")) user.setOrganization(updates.get("organization"));
        if (updates.containsKey("role")) user.setRole(Role.valueOf(updates.get("role")));
        return toResponse(userRepository.save(user));
    }

    public AuthDTO.UserResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toResponse(user);
    }

    public Map<String, Long> getDashboardStats() {
        return Map.of(
                "totalUsers", userRepository.count(),
                "totalOrganizers", userRepository.countByRole(Role.ORGANIZER),
                "pendingApprovals", (long) userRepository.findByAccountStatus(AccountStatus.PENDING_APPROVAL).size(),
                "activeUsers", (long) userRepository.findByAccountStatus(AccountStatus.ACTIVE).size()
        );
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public AuthDTO.UserResponse toResponse(User u) {
        AuthDTO.UserResponse r = new AuthDTO.UserResponse();
        r.setId(u.getId());
        r.setFullName(u.getFullName());
        r.setEmail(u.getEmail());
        r.setPhone(u.getPhone());
        r.setRole(u.getRole());
        r.setAccountStatus(u.getAccountStatus());
        r.setOrganization(u.getOrganization());
        r.setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        return r;
    }
}
