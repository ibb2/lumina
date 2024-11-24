package org.example.project.data

class NewEmail (
    val from: String,
    val subject: String,
    val to: String,
    val cc: Array<String>? = null,
    val bcc : Array<String>? = null,
    val body: String
)