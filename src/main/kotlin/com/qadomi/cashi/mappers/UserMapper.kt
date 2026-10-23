package com.qadomi.cashi.mappers

import com.qadomi.cashi.dto.UserProfileDTO
import com.qadomi.cashi.entity.User

fun User.toResponse() = UserProfileDTO(
    id = this.id,
    email = this.email,
    fullName = this.fullName
)
