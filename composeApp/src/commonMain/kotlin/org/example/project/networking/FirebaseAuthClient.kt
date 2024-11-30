package org.example.project.networking

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.network.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.example.project.utils.NetworkError
import org.example.project.utils.Result



class FirebaseAuthClient(
    private val httpClient: HttpClient
) {

    suspend fun googleLogin(googleIdToken: String): Result<OAuthResponse, NetworkError> {
        val response = try {
            httpClient.post("https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp") {
                this.parameter("key", "AIzaSyAjvAm4lv6lPlV_giLIPBbl9qLwouwksuE")
                this.contentType(ContentType.Application.Json)
                this.setBody(
                    OAuthPayload(
                        requestUri = "http://localhost:8080",
                        postBody = "id_token=$googleIdToken&providerId=google.com",
                        returnSecureToken = true,
                        returnIdpCredential = true
                    )
                )
            }
        } catch (e: UnresolvedAddressException) {
            return Result.Error(NetworkError.NO_INTERNET)
        } catch (e: SerializationException) {
            return Result.Error(NetworkError.SERIALIZATION)
        }

        println("Response status: ${response.status.value}")
        println("Response body: ${response.bodyAsText()}")

        return when (response.status.value) {
            in 200..299 -> {
                val oauthResponse = response.body<OAuthResponse>()
                Result.Success(oauthResponse)
            }

            401 -> Result.Error(NetworkError.UNAUTHORIZED)
            408 -> Result.Error(NetworkError.REQUEST_TIMEOUT)
            409 -> Result.Error(NetworkError.CONFLICT)
            413 -> Result.Error(NetworkError.PAYLOAD_TOO_LARGE)
            in 500..599 -> Result.Error(NetworkError.SERVER_ERROR)
            else -> {
                println(response.status.value)
                Result.Error(NetworkError.UNKNOWN)
            }
        }
    }




    suspend fun googleTokenIdEndpoint(code: String): Result<TokenResponse, NetworkError> {

        val response = try {
            httpClient.post("http://127.0.0.1:8000/api/tokens") {
                this.contentType(ContentType.Application.Json)
                this.setBody(
                    DjangoToken(code)
                )

            }
        } catch (e: UnresolvedAddressException) {
            return Result.Error(NetworkError.NO_INTERNET)
        } catch (e: SerializationException) {
            return Result.Error(NetworkError.SERIALIZATION)
        }
        return when (response.status.value) {
            in 200..299 -> {
                val oauthResponse = response.body<TokenResponse>()
                Result.Success(oauthResponse)
            }

            401 -> Result.Error(NetworkError.UNAUTHORIZED)
            408 -> Result.Error(NetworkError.REQUEST_TIMEOUT)
            409 -> Result.Error(NetworkError.CONFLICT)
            413 -> Result.Error(NetworkError.PAYLOAD_TOO_LARGE)
            in 500..599 -> Result.Error(NetworkError.SERVER_ERROR)
            else -> {
                println(response.status.description)
                println(response.status.value)
                Result.Error(NetworkError.UNKNOWN)
            }
        }
    }

    suspend fun refreshAccessToken(refreshToken: String): Result<DjangoRefreshTokenResponse, NetworkError> {

        val response = try {
            httpClient.post("http://127.0.0.1:8000/api/tokens/refresh") {
                this.contentType(ContentType.Application.Json)
                this.setBody(
                    DjangoRefreshToken(refreshToken)
                )

            }
        } catch (e: UnresolvedAddressException) {
            return Result.Error(NetworkError.NO_INTERNET)
        } catch (e: SerializationException) {
            return Result.Error(NetworkError.SERIALIZATION)
        }
        return when (response.status.value) {
            in 200..299 -> {
                val oauthResponse = response.body<DjangoRefreshTokenResponse>()
                Result.Success(oauthResponse)
            }

            401 -> Result.Error(NetworkError.UNAUTHORIZED)
            408 -> Result.Error(NetworkError.REQUEST_TIMEOUT)
            409 -> Result.Error(NetworkError.CONFLICT)
            413 -> Result.Error(NetworkError.PAYLOAD_TOO_LARGE)
            in 500..599 -> Result.Error(NetworkError.SERVER_ERROR)
            else -> {
                println(response.status.description)
                println(response.status.value)
                Result.Error(NetworkError.UNKNOWN)
            }
        }
    }

}