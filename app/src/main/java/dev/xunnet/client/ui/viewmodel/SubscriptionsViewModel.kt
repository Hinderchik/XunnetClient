package dev.xunnet.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.xunnet.client.core.domain.model.Subscription
import dev.xunnet.client.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _refreshing = MutableStateFlow<Set<String>>(emptySet())
    private val _snackbar = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SubscriptionsUiState>

    init {
        uiState = combine(
            subscriptionRepository.observeAll(),
            _refreshing,
            _snackbar
        ) { subs, refreshing, snack ->
            SubscriptionsUiState(
                subscriptions = subs,
                refreshing = refreshing,
                snackbar = snack
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionsUiState())
    }

    fun add(name: String, url: String) {
        if (name.isBlank() || url.isBlank()) {
            _snackbar.value = "Заполните все поля"
            return
        }
        viewModelScope.launch {
            subscriptionRepository.add(
                Subscription(name = name, url = url)
            ).onFailure { _snackbar.value = "Ошибка: ${it.message}" }
                .onSuccess { _snackbar.value = "Подписка добавлена" }
        }
    }

    fun refresh(id: String) {
        if (_refreshing.value.contains(id)) return
        _refreshing.value = _refreshing.value + id
        viewModelScope.launch {
            val r = subscriptionRepository.refresh(id)
            _snackbar.value = if (r.isSuccess) {
                val count = r.getOrNull()?.size ?: 0
                "Загружено: $count узлов"
            } else "Ошибка: ${r.exceptionOrNull()?.message}"
            _refreshing.value = _refreshing.value - id
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _snackbar.value = "Обновление всех подписок…"
            subscriptionRepository.refreshAll()
                .onSuccess { _snackbar.value = "Готово" }
                .onFailure { _snackbar.value = "Ошибка: ${it.message}" }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { subscriptionRepository.delete(id) }
    }

    fun consumeSnackbar() { _snackbar.value = null }

    data class SubscriptionsUiState(
        val subscriptions: List<Subscription> = emptyList(),
        val refreshing: Set<String> = emptySet(),
        val snackbar: String? = null
    )
}
