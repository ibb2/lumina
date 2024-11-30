package org.example.project.networking

import kotlinx.serialization.SerialName
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
    val rawUserInfo: String,
    val kind: String
)

@Serializable
data class OAuthPayload(
    val requestUri: String,
    val postBody: String,
    val returnSecureToken: Boolean,
    val returnIdpCredential: Boolean
)

@Serializable
data class Token(
    val code: String,
    val clientId: String,
    val redirectUri: String,
    val grantType: String
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String? = null, // Optional field
    @SerialName("scope") val scope: String? = null, // Optional field
    @SerialName("id_token") val idToken: String? = null // Optional field
)

@Serializable
data class DjangoToken(
    val code: String
)


@Serializable
data class DjangoRefreshToken(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class DjangoRefreshTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("scope") val scope: String,
    @SerialName("id_token") val idToken: String
)