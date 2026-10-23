package com.qadomi.cashi.utils

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import java.security.SecureRandom

object ShortIdGenerator {
    private val random = SecureRandom()
    private val alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()

    fun generate(size: Int = 12): String = NanoIdUtils.randomNanoId(random, alphabet, size)
}
