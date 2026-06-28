package dev.xunnet.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.repository.ProfileRepository
import dev.xunnet.client.core.vpn.SingBoxCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProxiesViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val singBoxCore: SingBoxCore
) : ViewModel() {

    private val _query = MutableStateFlow("")

    val uiState: StateFlow<ProxiesUiState>

    init {
        val profilesFlow = profileRepository.observeAll()
        val statsFlow = singBoxCore.stats
        val queryFlow = _query

        uiState = combine(profilesFlow, statsFlow, queryFlow) { profiles, stats, query ->
            val filtered = if (query.isBlank()) profiles else profiles.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.protocol.contains(query, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
            ProxiesUiState(
                proxies = filtered,
                query = query,
                activeProfileId = stats.activeProfileId
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProxiesUiState())
    }

    fun onQueryChange(q: String) { _query.value = q }

    fun toggleEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            val profile = profileRepository.getById(id) ?: return@launch
            profileRepository.save(profile.copy(enabled = enabled))
                .onFailure { Timber.e(it, "Failed to toggle profile $id") }
        }
    }

    fun add(profile: Profile) {
        viewModelScope.launch {
            profileRepository.save(profile)
                .onFailure { Timber.e(it, "Failed to add profile ${profile.name}") }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            profileRepository.delete(id)
        }
    }

    fun connect(profile: Profile) {
        viewModelScope.launch {
            val config = singBoxCore.buildConfig(profile)
            singBoxCore.startRaw(config)
        }
    }

    data class ProxiesUiState(
        val proxies: List<Profile> = emptyList(),
        val query: String = "",
        val activeProfileId: String? = null,
        val isLoading: Boolean = false
    )
}
