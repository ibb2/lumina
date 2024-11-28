package org.example.project.sqldelight

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.example.project.database.LuminaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.example.project.shared.data.AccountsDAO


class AccountsDataSource(db: LuminaDatabase) {

    private val queries = db.accountsTableQueries

    fun insert(
        id: Long?,
        federatedId: String,
        providerId: String,
        email: String,
        emailVerified: Boolean,
        firstName: String = "",
        fullName: String = "",
        lastName: String = "",
        photoUrl: String = "",
        localId: String = "",
        displayName: String = "",
        expiresIn: String,
        rawUserInfo: String,
        kind: String
    ) = queries.insertAccount(
        id = null,
        federated_id = federatedId,
        provider_id = providerId,
        email = email,
        email_verified = emailVerified,
        first_name = firstName,
        full_name = fullName,
        last_name = lastName,
        photo_url = photoUrl,
        local_id = localId,
        display_name = displayName,
        expires_in = expiresIn,
        raw_user_info = rawUserInfo,
        kind = kind
    )

    fun selectAll() = queries.selectAllAccounts(mapper = { id, federated_id, provider_id, email, email_verified, first_name, full_name, last_name, photo_url, local_id, display_name, expires_in, raw_user_info, kind ->
        AccountsDAO(
           id = id,
           federatedId = federated_id,
           providerId = provider_id,
           email = email,
           emailVerified = email_verified,
           firstName = first_name ?: "",
           fullName = full_name ?: "",
           lastName = last_name ?: "",
           photoUrl = photo_url ?: "",
           localId = local_id,
           displayName = display_name ?: "",
           expiresIn = expires_in,
           rawUserInfo = raw_user_info,
           kind = kind
        )
    }).asFlow().mapToList(context = Dispatchers.IO)
    fun select(emailAddress: String) = queries.selectAccount(emailAddress)

    fun remove(emailAddress: String) = queries.removeAccount(emailAddress)

    fun removeAll() = queries.removeAllAccounts()
}