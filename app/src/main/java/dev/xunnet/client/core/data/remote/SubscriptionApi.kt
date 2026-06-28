package dev.xunnet.client.core.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface SubscriptionApi {
    @GET
    suspend fun fetchSubscription(@Url url: String): Response<ResponseBody>
}
