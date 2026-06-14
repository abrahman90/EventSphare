package com.example.eventsphere.dto;

import com.example.eventsphere.enums.AccountStatus;
import com.example.eventsphere.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDTO {

    @Data
    public static class RegisterRequest {
        @NotBlank private String fullName;
        @Email @NotBlank private String email;
        @NotBlank private String phone;
        @NotBlank @Size(min = 8) private String password;
        private String organization;
    }

    @Data
    public static class LoginRequest {
        @Email @NotBlank private String email;
        @NotBlank private String password;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private String email;
        private String fullName;
        private Role role;
        private AccountStatus accountStatus;
        private Long userId;

        public LoginResponse(String token, String email, String fullName, Role role, AccountStatus status, Long userId) {
            this.token = token;
            this.email = email;
            this.fullName = fullName;
            this.role = role;
            this.accountStatus = status;
            this.userId = userId;
        }
    }

    @Data
    public static class UserResponse {
        private Long id;
        private String fullName;
        private String email;
        private String phone;
        private Role role;
        private AccountStatus accountStatus;
        private String organization;
        private String createdAt;
    }
}
