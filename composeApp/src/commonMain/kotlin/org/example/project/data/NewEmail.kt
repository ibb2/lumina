package org.example.project.data

class NewEmail (
    val from: String,
    val subject: String,
    val to: String,
    val cc: Array<String>?,
    val bcc : Array<String>?,
    val body: String
)