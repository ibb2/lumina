package org.example.project.shared.data

data class AccountsDAO(
    val id: Long,
    val federatedId: String,
    val providerId: String,
    val email: String,
    val emailVerified: Boolean,
    val firstName: String =  "",
    val fullName: String = "",
    val lastName: String = "",
    val photoUrl: String = "",
    val localId: String,
    val displayName: String,
    val expiresIn: String,
    val rawUserInfo: String,
    val kind: String
)
