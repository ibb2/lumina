package org.example.project.windowsNative

import com.microsoft.credentialstorage.SecretStore
import com.microsoft.credentialstorage.StorageProvider
import com.microsoft.credentialstorage.StorageProvider.SecureOption
import com.microsoft.credentialstorage.model.StoredCredential
import org.example.project.windowsNative.StoredCredentialApp.Companion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class CredentialManager(val user: String, val type: String) {
    private val credentialStorage: SecretStore<StoredCredential>? =
        StorageProvider.getCredentialStorage(true, SecureOption.REQUIRED)
    private val log: Logger = LoggerFactory.getLogger(StoredCredentialApp::class.java)

    private val CREDENTIALS_KEY: String = "Lumina Mail $user:$type"

//    private fun run() {
//        // Get a secure store instance.
//        credentialStorage = StorageProvider.getCredentialStorage(true, SecureOption.REQUIRED)
//
//        if (credentialStorage == null) {
//            log.error("No secure credential storage available.")
//            return
//        }
//
//        userLogin()
//
//        unregisterUser()
//    }

    fun exists(): Boolean {
        // Save the credential to the store.
        val storedCredential = credentialStorage!![CREDENTIALS_KEY]

        return storedCredential !== null
    }

    fun registerUser(email: String, password: String) {
        val credential = StoredCredential(email, password.toCharArray())

        try {
            // Save the credential to the store.
            credentialStorage!!.add(CREDENTIALS_KEY, credential)
            log.info("User registered.")
        } finally {
            // clear password value.
            credential.clear()
        }
    }

    fun returnCredentials(): StoredCredential? {
        return credentialStorage!![CREDENTIALS_KEY]
    }

    fun userLogin() {
        log.info("Authenticating a user")

        val enteredCredential = enterCredentials()
        var storedCredential: StoredCredential? = null

        try {
            // Save the credential to the store.
            storedCredential = credentialStorage!![CREDENTIALS_KEY]

            if (storedCredential == enteredCredential) {
                log.info("User logged in successfully.")
            } else {
                log.info("Authentication failed.")
            }
        } finally {
            // clear password value
            enteredCredential.clear()

            storedCredential?.clear()
        }
    }

    fun unregisterUser() {
        // Remove credentials from the store.
        credentialStorage!!.delete(CREDENTIALS_KEY)
        log.info("User deleted.")
    }

    private fun enterCredentials(): StoredCredential {
        // Request user name from user.
        val userName = System.console().readLine("Enter user name: ")

        // Request password from user.
        // Using API which returns char[] to avoid creating String
        // to minimize memory footprint for secure purposes.
        val password = System.console().readPassword("Enter password: ")

        val credential = StoredCredential(userName, password)

        // Password value is not needed anymore, clear it now without waiting GC to remove it.
        Arrays.fill(password, 0x00.toChar())

        return credential
    }

}