package dev.xunnet.client.core.vpn

import timber.log.Timber

class SingBoxCore {

    init {
        System.loadLibrary("xunnet-core")
    }

    fun start(config: String): Result<Unit> {
        return try {
            val success = nativeStart(config)
            if (success) Result.success(Unit) else Result.failure(Exception("Failed to start sing-box"))
        } catch (e: Exception) {
            Timber.e(e, "sing-box start error")
            Result.failure(e)
        }
    }

    fun stop(): Result<Unit> {
        return try {
            val success = nativeStop()
            if (success) Result.success(Unit) else Result.failure(Exception("Failed to stop sing-box"))
        } catch (e: Exception) {
            Timber.e(e, "sing-box stop error")
            Result.failure(e)
        }
    }

    fun getVersion(): String = nativeGetVersion()

    private external fun nativeStart(config: String): Boolean
    private external fun nativeStop(): Boolean
    private external fun nativeGetVersion(): String
}
