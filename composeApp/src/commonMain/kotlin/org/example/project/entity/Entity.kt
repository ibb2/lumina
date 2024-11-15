package org.example.project.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Account(
    @SerialName("email_address")
    val emailAddress: String,
)

@Serializable
data class Email(
    @SerialName("from")
    val from_user: String,
    @SerialName("to")
    val to_user: String,
    @SerialName("cc")
    val cc: String,
    @SerialName("bcc")
    val bcc: String,
    @SerialName("subject")
    val subject: String,
    @SerialName("body")
    val body: String,
    @SerialName("account")
    val accounts: Account
)
