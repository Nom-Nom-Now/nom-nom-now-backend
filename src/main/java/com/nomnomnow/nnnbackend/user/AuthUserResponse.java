package com.nomnomnow.nnnbackend.user;

import java.time.OffsetDateTime;

public record AuthUserResponse(
        Long id,
        String email,
        String name,
        OffsetDateTime createdAt
) {
}
