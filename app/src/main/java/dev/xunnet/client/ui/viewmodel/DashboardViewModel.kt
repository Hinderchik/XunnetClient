package dev.xunnet.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.model.Stats
import dev.xunnet.client.core.domain.repository.ProfileRepository
import dev.xunnet.client.core.vpn.SingBoxCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val singBoxCore: SingBoxCore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState>

    init {
        // Combine profiles + stats → UI state
        val profilesFlow = profileRepository.observeAll()
        val statsFlow = singBoxCore.stats

        val combined = combine(profilesFlow, statsFlow) { profiles, stats ->
            val active = profiles.firstOrNull { it.id == stats.activeProfileId }
                ?: profiles.firstOrNull { it.enabled }
            DashboardUiState(
                isConnected = stats.connected,
                isConnecting = false, // TODO: track separate state
                activeProfile = active,
                profiles = profiles,
                downloadSpeed = formatSpeed(stats.downloadSpeed),
                uploadSpeed = formatSpeed(stats.uploadSpeed),
                totalDownload = formatBytes(stats.downloadBytes),
                totalUpload = formatBytes(stats.uploadBytes)
            )
        }
        uiState = combined.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
    }

    fun connect(profile: Profile) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true)
            val config = singBoxCore.buildConfig(profile)
            val result = singBoxCore.startRaw(config)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isConnected = true,
                    isConnecting = false,
                    activeProfile = profile
                )
            }.onFailure {
                Timber.e(it, "Failed to start VPN")
                _uiState.value = _uiState.value.copy(isConnecting = false)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            singBoxCore.stop()
            _uiState.value = _uiState.value.copy(
                isConnected = false,
                isConnecting = false,
                activeProfile = null
            )
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String = when {
        bytesPerSec < 1024 -> "$bytesPerSec B/s"
        bytesPerSec < 1024 * 1024 -> "${bytesPerSec / 1024} KB/s"
        else -> String.format("%.1f MB/s", bytesPerSec / 1024.0 / 1024.0)
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024L * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
        else -> String.format("%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
    }

    data class DashboardUiState(
        val isConnected: Boolean = false,
        val isConnecting: Boolean = false,
        val activeProfile: Profile? = null,
        val profiles: List<Profile> = emptyList(),
        val downloadSpeed: String = "0 B/s",
        val uploadSpeed: String = "0 B/s",
        val totalDownload: String = "0 B",
        val totalUpload: String = "0 B"
    )
}
