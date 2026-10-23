package com.qadomi.cashi.repositories

import com.qadomi.cashi.entity.RefreshToken

interface RefreshTokenRepository {
    fun save(refreshToken: RefreshToken)
    fun findByTokenHash(tokenHash: String): RefreshToken?
    fun deleteByTokenHash(tokenHash: String)
}
