package dev.xunnet.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.xunnet.client.core.domain.model.FederatedPanel
import dev.xunnet.client.core.domain.repository.FederationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FederationViewModel @Inject constructor(
    private val federationRepository: FederationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FederationUiState())
    val uiState: StateFlow<FederationUiState> = _uiState.asStateFlow()

    init {
        loadPanels()
    }

    private fun loadPanels() {
        viewModelScope.launch {
            federationRepository.observeAll().collect { panels ->
                _uiState.value = _uiState.value.copy(panels = panels)
            }
        }
    }

    fun addPanel(panel: FederatedPanel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val r = federationRepository.addPanel(panel)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = r.exceptionOrNull()?.message
            )
        }
    }

    suspend fun syncPanel(id: String): Result<List<dev.xunnet.client.core.domain.model.Profile>> {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val r = federationRepository.syncPanel(id)
        _uiState.value = _uiState.value.copy(isLoading = false, error = r.exceptionOrNull()?.message)
        return r
    }

    fun deletePanel(id: String) {
        viewModelScope.launch {
            federationRepository.deletePanel(id)
        }
    }

    data class FederationUiState(
        val panels: List<FederatedPanel> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )
}
