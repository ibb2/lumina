package org.example.project.shared.utils

fun createCompositeKey(subject: String?, receivedDate: String?, sender: String?): String {
    val key = "${subject.orEmpty()}_${receivedDate.orEmpty()}_${sender.orEmpty()}"
    return key
}