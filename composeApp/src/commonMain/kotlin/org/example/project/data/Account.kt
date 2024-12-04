package org.example.project.data

class Account (
    val federateId: String,
    val providerId: String,
    val email: String,
    val emailVerified: Boolean,
    val firstName: String =  "",
    val fullName: String = "",
    val lastName: String = "",
    val photoUrl: String = "",
    val localId: String,
    val displayName: String,
    val expiresIn: Int,
    val rawUserInfo: String,
    val kind: String
)
