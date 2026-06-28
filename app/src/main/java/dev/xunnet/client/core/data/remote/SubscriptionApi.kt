package dev.xunnet.client.core.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface SubscriptionApi {
    /**
     * Fetch subscription content as plain text. Many subscription providers
     * return either plain newlines, base64-encoded list, or JSON.
     */
    @GET
    suspend fun fetchSubscription(
        @Url url: String,
        @Header("User-Agent") userAgent: String = "Xunnet/1.0"
    ): Response<ResponseBody>
}
