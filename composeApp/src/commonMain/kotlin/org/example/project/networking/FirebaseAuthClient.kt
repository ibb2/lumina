package org.example.project.networking

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.network.*
import kotlinx.serialization.SerializationException
import org.example.project.utils.NetworkError
import org.example.project.utils.Result

class FirebaseAuthClient(
    private val httpClient: HttpClient
) {

    suspend fun googleLogin(): Result<OAuthResponse, NetworkError> {
        val response = try {
            httpClient.post("https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp") {
                this.parameter("key", "AIzaSyAjvAm4lv6lPlV_giLIPBbl9qLwouwksuE")
                this.contentType(ContentType.Application.Json)
                this.setBody<OAuthPayload>(
                    OAuthPayload(
                        requestUri = "https://accounts.google.com/o/oauth2/auth",
                        postBody = "grant_type=authorization_code&code=code",
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

        return when (response.status.value) {
            in 200..299 -> {
                val oauthReponse = response.body<OAuthResponse>()
                Result.Success(oauthReponse)
            }

            401 -> Result.Error(NetworkError.UNAUTHORIZED)
            408 -> Result.Error(NetworkError.REQUEST_TIMEOUT)
            409 -> Result.Error(NetworkError.CONFLICT)
            413 -> Result.Error(NetworkError.PAYLOAD_TOO_LARGE)
            in 500..599 -> Result.Error(NetworkError.SERVER_ERROR)
            else -> {
                Result.Error(NetworkError.UNKNOWN)
            }
        }
    }

}