package dev.xunnet.client.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.xunnet.client.core.data.local.AppDatabase
import dev.xunnet.client.core.data.local.FederatedPanelDao
import dev.xunnet.client.core.data.local.ProfileDao
import dev.xunnet.client.core.data.local.SubscriptionDao
import dev.xunnet.client.core.data.remote.FederationApi
import dev.xunnet.client.core.data.remote.SubscriptionApi
import dev.xunnet.client.core.data.repository.ProfileRepositoryImpl
import dev.xunnet.client.core.data.repository.SubscriptionRepositoryImpl
import dev.xunnet.client.core.data.repository.FederationRepositoryImpl
import dev.xunnet.client.core.domain.parser.LinkParser
import dev.xunnet.client.core.domain.repository.FederationRepository
import dev.xunnet.client.core.domain.repository.ProfileRepository
import dev.xunnet.client.core.domain.repository.SubscriptionRepository
import dev.xunnet.client.core.vpn.SingBoxCore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "xunnet.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideSubscriptionDao(db: AppDatabase): SubscriptionDao = db.subscriptionDao()

    @Provides
    fun provideFederatedPanelDao(db: AppDatabase): FederatedPanelDao = db.federatedPanelDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideSubscriptionApi(client: OkHttpClient): SubscriptionApi {
        return Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SubscriptionApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFederationApi(client: OkHttpClient): FederationApi {
        return Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FederationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideLinkParser(): LinkParser = XunnetLinkParser()

    @Provides
    @Singleton
    fun provideSingBoxCore(): SingBoxCore = SingBoxCore()

    @Provides
    @Singleton
    fun provideProfileRepository(
        dao: ProfileDao,
        parser: LinkParser
    ): ProfileRepository = ProfileRepositoryImpl(dao, parser)

    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        dao: SubscriptionDao,
        profileDao: ProfileDao,
        api: SubscriptionApi,
        parser: LinkParser
    ): SubscriptionRepository = SubscriptionRepositoryImpl(dao, profileDao, api, parser)

    @Provides
    @Singleton
    fun provideFederationRepository(
        dao: FederatedPanelDao,
        profileDao: ProfileDao,
        api: FederationApi
    ): FederationRepository = FederationRepositoryImpl(dao, profileDao, api)
}
