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
    Boolean active,
    java.util.List<String> vehicleRegNos,
    java.util.List<VehicleDto> vehicles
) {
    public static UserDto fromEntity(User user) {
        return fromEntity(user, java.util.Collections.emptyList());
    }

    public static UserDto fromEntity(User user, java.util.List<VehicleDto> vehicles) {
        java.util.List<VehicleDto> vList = vehicles != null ? vehicles : java.util.Collections.emptyList();
        java.util.List<String> regNos = vList.stream()
                .map(VehicleDto::regNo)
                .filter(r -> r != null && !r.isBlank())
                .toList();

        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole(),
            user.getLanguage(),
            user.getAlertPreferences(),
            user.getActive(),
            regNos,
            vList
        );
    }
}
