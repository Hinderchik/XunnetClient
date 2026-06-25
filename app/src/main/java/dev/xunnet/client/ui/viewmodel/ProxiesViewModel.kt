package dev.xunnet.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.xunnet.client.core.domain.model.Profile
import dev.xunnet.client.core.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProxiesViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProxiesUiState())
    val uiState: StateFlow<ProxiesUiState> = _uiState.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            profileRepository.observeAll().collect { profiles ->
                _uiState.value = _uiState.value.copy(allProfiles = profiles, filteredProfiles = profiles)
            }
        }
    }

    fun search(query: String) {
        val all = _uiState.value.allProfiles
        val filtered = if (query.isBlank()) all else all.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.protocol.contains(query, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        _uiState.value = _uiState.value.copy(filteredProfiles = filtered, searchQuery = query)
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            profileRepository.delete(id)
        }
    }

    data class ProxiesUiState(
        val allProfiles: List<Profile> = emptyList(),
        val filteredProfiles: List<Profile> = emptyList(),
        val searchQuery: String = "",
        val isLoading: Boolean = false
    )
}
