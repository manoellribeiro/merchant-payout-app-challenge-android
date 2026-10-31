package manoellribeiro.dev.data

import android.content.Context
import android.net.ConnectivityManager
import manoellribeiro.dev.data.local.createEncryptedDevicePreferences
import manoellribeiro.dev.data.remote.MerchantApi
import manoellribeiro.dev.data.remote.apiJson
import manoellribeiro.dev.data.repository.DeviceIdentityRepositoryImpl
import manoellribeiro.dev.data.repository.MerchantRepositoryImpl
import manoellribeiro.dev.data.service.ConnectivityServiceImpl
import manoellribeiro.dev.domain.repository.DeviceIdentityRepository
import manoellribeiro.dev.domain.repository.MerchantRepository
import manoellribeiro.dev.domain.service.ConnectivityService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** The only entry point to this module. Implementations stay internal, so :app sees just the interfaces. */
object DataLayerFactory {

    fun createMerchantRepository(baseUrl: String): MerchantRepository {
        val client = OkHttpClient.Builder()
            // A payout POST has no idempotency key yet, so OkHttp must not silently resend it.
            .retryOnConnectionFailure(false)
            .build()
        val api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(apiJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create<MerchantApi>()
        return MerchantRepositoryImpl(api)
    }

    fun createConnectivityService(context: Context): ConnectivityService =
        ConnectivityServiceImpl(context.getSystemService(ConnectivityManager::class.java))

    fun createDeviceIdentityRepository(context: Context): DeviceIdentityRepository {
        val applicationContext = context.applicationContext
        return DeviceIdentityRepositoryImpl(
            preferencesProvider = { createEncryptedDevicePreferences(applicationContext) },
        )
    }
}
