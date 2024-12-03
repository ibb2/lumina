package org.example.project

import com.example.AccountsTableQueries
import com.example.EmailsTableQueries
import kotlinx.coroutines.flow.StateFlow
import org.example.project.data.NewEmail
import org.example.project.networking.FirebaseAuthClient
import org.example.project.networking.OAuthResponse
import org.example.project.shared.data.AccountsDAO
import org.example.project.shared.data.AttachmentsDAO
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.AccountsDataSource
import org.example.project.sqldelight.AttachmentsDataSource
import org.example.project.sqldelight.EmailsDataSource
import org.example.project.utils.NetworkError

