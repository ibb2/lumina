package org.example.project.windowsNative

import com.microsoft.credentialstorage.SecretStore
import com.microsoft.credentialstorage.StorageProvider
import com.microsoft.credentialstorage.StorageProvider.SecureOption
import com.microsoft.credentialstorage.model.StoredCredential
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class CredentialManager(val user: String,val type: String) {
    private var credentialStorage: SecretStore<StoredCredential>? = null
    private val log: Logger = LoggerFactory.getLogger(StoredCredentialApp::class.java)

    private val CREDENTIALS_KEY: String = "Lumina Mail $user:$type"

    private fun run() {
        // Get a secure store instance.
        credentialStorage = StorageProvider.getCredentialStorage(true, SecureOption.REQUIRED)

        if (credentialStorage == null) {
            log.error("No secure credential storage available.")
            return
        }

        registerUser()

        userLogin()

        unregisterUser()
    }

    private fun registerUser() {
        log.info("Registering a new user:")

        val credential = enterCredentials()

        try {
            // Save the credential to the store.
            credentialStorage!!.add(CREDENTIALS_KEY, credential)
            log.info("User registered.")
        } finally {
            // clear password value.
            credential.clear()
        }
    }

    private fun userLogin() {
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

    private fun unregisterUser() {
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