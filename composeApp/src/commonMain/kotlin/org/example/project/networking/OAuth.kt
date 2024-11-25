package org.example.project.networking

import kotlinx.serialization.Serializable

@Serializable
data class OAuthResponse(
    val federatedId: String,
    val providerId: String,
    val localId: String,
    val emailVerified: Boolean,
    val email: String,
    val oauthIdToken: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val displayName: String,
    val idToken: String,
    val photoUrl: String,
    val refreshToken: String,
    val expiresIn: String,
    val rawUserInfo: String
)

@Serializable
data class OAuthPayload(
    val requestUri: String,
    val postBody: String,
    val returnSecureToken: Boolean,
    val returnIdpCredential: Boolean
)