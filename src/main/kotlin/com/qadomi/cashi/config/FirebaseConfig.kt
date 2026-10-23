package com.qadomi.cashi.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.io.FileInputStream

@Configuration
class FirebaseConfig(
    @Value("\${firebase.credentials-path:}") private val credentialsPath: String,
    @Value("\${firebase.project-id:}") private val projectId: String
) {

    private val log = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @Bean
    fun firebaseApp(): FirebaseApp {
        FirebaseApp.getApps().firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }?.let { return it }

        val optionsBuilder = FirebaseOptions.builder().setCredentials(loadCredentials())
        if (projectId.isNotBlank()) {
            optionsBuilder.setProjectId(projectId)
        }
        return FirebaseApp.initializeApp(optionsBuilder.build())
    }

    @Bean
    fun firestore(firebaseApp: FirebaseApp): Firestore = FirestoreClient.getFirestore(firebaseApp)

    // Prefer the service account file; otherwise fall back to Application Default Credentials
    // (GOOGLE_APPLICATION_CREDENTIALS, gcloud auth, or the runtime's attached service account).
    private fun loadCredentials(): GoogleCredentials {
        val file = File(credentialsPath)
        return if (credentialsPath.isNotBlank() && file.isFile) {
            log.info("Loading Firebase credentials from {}", file.absolutePath)
            FileInputStream(file).use { GoogleCredentials.fromStream(it) }
        } else {
            log.info("Firebase credentials file not found, using Application Default Credentials")
            GoogleCredentials.getApplicationDefault()
        }
    }
}
