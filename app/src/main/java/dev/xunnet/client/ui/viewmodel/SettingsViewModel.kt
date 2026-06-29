package dev.xunnet.client.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xunnet.client.core.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(load())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private fun load(): SettingsUiState {
        val aliases = discoverIconAliases()
        return SettingsUiState(
            autoConnect = repo.autoConnect,
            killSwitch = repo.killSwitch,
            blockQuic = repo.blockQuic,
            disconnectOnSleep = repo.disconnectOnSleep,
            splitPreset = repo.splitTunnelingPreset,
            themeMode = repo.themeMode,
            language = repo.language,
            selectedIconAlias = repo.selectedIconAlias,
            availableIconAliases = aliases
        )
    }

    fun setAutoConnect(value: Boolean) { repo.autoConnect = value; reload() }
    fun setKillSwitch(value: Boolean) { repo.killSwitch = value; reload() }
    fun setBlockQuic(value: Boolean) { repo.blockQuic = value; reload() }
    fun setDisconnectOnSleep(value: Boolean) { repo.disconnectOnSleep = value; reload() }
    fun setSplitPreset(value: String) { repo.splitTunnelingPreset = value; reload() }
    fun setThemeMode(value: SettingsRepository.ThemeMode) { repo.themeMode = value; reload() }
    fun setLanguage(value: SettingsRepository.Language) { repo.language = value; reload() }

    fun applyIconAlias(aliasIndex: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                applyIconAliasInternal(aliasIndex)
            }
            repo.selectedIconAlias = aliasIndex
            reload()
        }
    }

    private fun applyIconAliasInternal(targetIndex: Int) {
        val pm = context.packageManager
        val pkg = context.packageName
        // Disable all aliases, enable target (or disable all if -1 to fall back to main)
        val aliases = discoverIconAliases()
        aliases.forEach { info ->
            val compName = "${pkg}.${info.componentName}"
            val state = if (info.index == targetIndex) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(
                android.content.ComponentName(pkg, compName),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    /**
     * Discovers all activity-aliases declared in the manifest that match
     * the pattern `IconAlias<index>` and returns their metadata.
     */
    private fun discoverIconAliases(): List<IconAliasInfo> {
        val pm = context.packageManager
        val pkg = context.packageName
        val result = mutableListOf<IconAliasInfo>()
        for (i in 0..9) {
            val compName = "${pkg}.IconAlias$i"
            try {
                val info = pm.getActivityInfo(
                    android.content.ComponentName(pkg, compName),
                    PackageManager.GET_META_DATA
                )
                val label = info.loadLabel(pm).toString()
                result.add(IconAliasInfo(i, label, compName))
            } catch (e: PackageManager.NameNotFoundException) {
                // not found
            }
        }
        return result
    }

    private fun reload() {
        _state.value = load()
    }

    data class IconAliasInfo(val index: Int, val label: String, val componentName: String)
}

data class SettingsUiState(
    val autoConnect: Boolean = false,
    val killSwitch: Boolean = true,
    val blockQuic: Boolean = false,
    val disconnectOnSleep: Boolean = false,
    val splitPreset: String = "default",
    val themeMode: SettingsRepository.ThemeMode = SettingsRepository.ThemeMode.SYSTEM,
    val language: SettingsRepository.Language = SettingsRepository.Language.SYSTEM,
    val selectedIconAlias: Int = -1,
    val availableIconAliases: List<SettingsViewModel.IconAliasInfo> = emptyList()
)
