package com.mirage.bank.core.update

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface GitHubApi {
    /**
     * Deliberately the *list* endpoint, not GET /repos/{o}/{r}/releases/latest --
     * "latest" is GitHub's most-recent-non-prerelease-release by date, which
     * could resolve to a non-Android release if this repo ever also tags
     * backend releases. We fetch the list and filter for the "android-v"
     * prefix ourselves (see UpdateChecker).
     */
    @GET("repos/{owner}/{repo}/releases")
    suspend fun listReleases(@Path("owner") owner: String, @Path("repo") repo: String): List<GitHubRelease>
}

object GitHubApiClient {
    private val json = Json { ignoreUnknownKeys = true }

    fun create(): GitHubApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(GitHubApi::class.java)
    }
}
