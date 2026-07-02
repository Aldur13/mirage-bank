package com.mirage.bank.core.di

import android.content.Context
import com.mirage.bank.core.network.ApiClient
import com.mirage.bank.core.network.ApiService
import com.mirage.bank.core.session.SecureTokenStore
import com.mirage.bank.core.session.SessionManager
import com.mirage.bank.core.update.GitHubApiClient
import com.mirage.bank.core.update.UpdateChecker
import com.mirage.bank.core.update.UpdateDownloader
import com.mirage.bank.core.update.UpdateRepository

/**
 * Manual DI graph -- a hand-rolled Hilt would be pure ceremony at this app's
 * scope. One instance lives on the Application and is threaded through
 * Compose via LocalAppContainer.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val sessionManager: SessionManager = SessionManager(SecureTokenStore(appContext))
    val apiService: ApiService = ApiClient.create(sessionManager)

    val updateRepository: UpdateRepository = UpdateRepository(
        context = appContext,
        checker = UpdateChecker(GitHubApiClient.create()),
        downloader = UpdateDownloader(appContext),
    )
}
