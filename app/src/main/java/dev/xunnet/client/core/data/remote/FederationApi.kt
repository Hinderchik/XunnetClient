package dev.xunnet.client.core.data.remote

import dev.xunnet.client.core.domain.model.FederatedPanel
import dev.xunnet.client.core.domain.model.Profile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface FederationApi {
    @GET
    suspend fun getInfo(@Url url: String, @Header("X-API-Key") apiKey: String): Response<FederatedPanel>

    @GET
    suspend fun getServers(
        @Url url: String,
        @Header("X-API-Key") apiKey: String,
        @Query("tags") tags: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<List<Profile>>

    @POST
    suspend fun sync(
        @Url url: String,
        @Header("X-API-Key") apiKey: String,
        @Body body: Map<String, Any>
    ): Response<Map<String, Any>>
}
