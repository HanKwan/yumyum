package com.example.yumyum.dto;

public record UserResponse(
        Long id,
        String username,
        String bio,
        String profileImage
) {
}
