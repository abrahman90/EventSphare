package com.example.eventsphere.service;

import com.example.eventsphere.dto.AuthDTO;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.AccountStatus;
import com.example.eventsphere.enums.Role;
import com.example.eventsphere.repository.UserRepository;
import com.example.eventsphere.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;

    public User register(AuthDTO.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ORGANIZER)
                .accountStatus(AccountStatus.PENDING_APPROVAL)
                .organization(request.getOrganization())
                .build();
        User saved = userRepository.save(user);


        emailService.sendWelcomeEmail(saved.getFullName(), saved.getEmail());
        return saved;
    }

    public AuthDTO.LoginResponse login(AuthDTO.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getAccountStatus() == AccountStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Account pending approval by administrator");
        }
        if (user.getAccountStatus() == AccountStatus.INACTIVE || user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new RuntimeException("Account is inactive. Contact administrator.");
        }

        authManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtUtil.generateToken(userDetails, user.getRole().name());

        return new AuthDTO.LoginResponse(token, user.getEmail(), user.getFullName(),
                user.getRole(), user.getAccountStatus(), user.getId());
    }

    public void createDefaultAdmin() {
        if (!userRepository.existsByEmail("admin@eventsphere.com")) {
            User admin = User.builder()
                    .fullName("System Administrator")
                    .email("admin@eventsphere.com")
                    .phone("+255700000000")
                    .password(passwordEncoder.encode("Admin@2025"))
                    .role(Role.ADMIN)
                    .accountStatus(AccountStatus.ACTIVE)
                    .organization("EventSphere")
                    .build();
            userRepository.save(admin);
        }
    }
}
