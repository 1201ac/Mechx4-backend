package com.nerlogix.dto;

import com.nerlogix.entity.User;
import com.nerlogix.model.enums.Role;

public record UserDto(
    Long id,
    String email,
    String name,
    Role role,
    String language,
    String alertPreferences,
    Boolean active
) {
    public static UserDto fromEntity(User user) {
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole(),
            user.getLanguage(),
            user.getAlertPreferences(),
            user.getActive()
        );
    }
}
