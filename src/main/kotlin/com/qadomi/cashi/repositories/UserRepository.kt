package com.qadomi.cashi.repositories

import com.qadomi.cashi.entity.User

interface UserRepository {
    fun findById(id: String): User?
    fun findByEmail(email: String): User?

    /** Creates the user; returns false if the email is already registered. */
    fun create(user: User): Boolean
}
