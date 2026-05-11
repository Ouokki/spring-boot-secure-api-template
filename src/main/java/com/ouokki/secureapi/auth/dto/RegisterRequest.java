package com.ouokki.secureapi.auth.dto;

import com.ouokki.secureapi.auth.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email @Size(max = 254) String email, @NotBlank @ValidPassword String password) {}
