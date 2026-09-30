package com.example.pulsechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank(message = "room name must not be blank")
        @Size(max = 100, message = "room name must be at most 100 characters")
        String name
) {}
