package dev.xunnet.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.repository.ProfileRepository
import dev.xunnet.client.core.vpn.SingBoxCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val singBoxCore: SingBoxCore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            profileRepository.observeAll().collect { profiles ->
                _uiState.value = _uiState.value.copy(
                    profiles = profiles.filter { it.enabled },
                    activeProfile = profiles.firstOrNull { it.enabled }
                )
            }
        }
    }

    fun connect(profile: Profile) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true)
            val config = "" // TODO: generate sing-box config from profile
            val result = singBoxCore.start(config)
            _uiState.value = _uiState.value.copy(
                isConnected = result.isSuccess,
                isConnecting = false,
                activeProfile = if (result.isSuccess) profile else _uiState.value.activeProfile
            )
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            singBoxCore.stop()
            _uiState.value = _uiState.value.copy(isConnected = false, activeProfile = null)
        }
    }

    data class DashboardUiState(
        val isConnected: Boolean = false,
        val isConnecting: Boolean = false,
        val activeProfile: Profile? = null,
        val profiles: List<Profile> = emptyList(),
        val downloadSpeed: String = "0 B/s",
        val uploadSpeed: String = "0 B/s"
    )
}
