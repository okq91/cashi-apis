package com.qadomi.cashi.repositories

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.qadomi.cashi.entity.User
import org.springframework.stereotype.Repository

@Repository
class FirestoreUserRepository(private val firestore: Firestore) : UserRepository {

    private val users get() = firestore.collection(Collections.USERS)
    private val emails get() = firestore.collection(Collections.USER_EMAILS)

    override fun findById(id: String): User? {
        val snapshot = users.document(id).get().get()
        return if (snapshot.exists()) snapshot.toUser() else null
    }

    override fun findByEmail(email: String): User? {
        val emailDoc = emails.document(email).get().get()
        val userId = emailDoc.getString("userId") ?: return null
        return findById(userId)
    }

    override fun create(user: User): Boolean {
        return firestore.runTransaction { tx ->
            val emailRef = emails.document(user.email)
            if (tx.get(emailRef).get().exists()) {
                false
            } else {
                tx.create(emailRef, mapOf("userId" to user.id))
                tx.create(
                    users.document(user.id), mapOf(
                        "email" to user.email,
                        "fullName" to user.fullName,
                        "passwordHash" to user.passwordHash,
                        "createdAt" to user.createdAt.toTimestamp()
                    )
                )
                true
            }
        }.get()
    }

    private fun DocumentSnapshot.toUser() = User(
        id = id,
        email = getString("email")!!,
        fullName = getString("fullName")!!,
        passwordHash = getString("passwordHash")!!,
        createdAt = getTimestamp("createdAt")!!.toInstant()
    )
}
