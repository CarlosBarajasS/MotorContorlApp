// app/src/main/java/com/arranquesuave/motorcontrolapp/viewmodel/NetworkConfigViewModel.kt
package com.arranquesuave.motorcontrolapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arranquesuave.motorcontrolapp.utils.NetworkConfigManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * VIEWMODEL PARA CONFIGURACIÓN DE RED LOCAL
 * 
 * Gestiona la configuración personalizable de cada usuario
 * para su red WiFi local (Raspberry Pi doméstica)
 */
class NetworkConfigViewModel(application: Application) : AndroidViewModel(application) {
    
    private val networkConfigManager = NetworkConfigManager(application)
    
    /**
     * Configuración actual como StateFlow
     */
    val currentConfig = networkConfigManager.localNetworkConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NetworkConfigManager.LocalNetworkConfig()
        )
    
    /**
     * Guardar nueva configuración
     */
    fun saveConfiguration(config: NetworkConfigManager.LocalNetworkConfig) {
        viewModelScope.launch {
            networkConfigManager.saveLocalNetworkConfig(config)
        }
    }
    
    /**
     * Restablecer a configuración por defecto
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            networkConfigManager.resetToDefaults()
        }
    }
    
    /**
     * Obtener configuraciones predefinidas comunes
     */
    fun getCommonConfigurations(): List<NetworkConfigManager.LocalNetworkConfig> {
        return networkConfigManager.getCommonConfigurations()
    }
    
    /**
     * Validar si una configuración es válida
     */
    fun isConfigValid(config: NetworkConfigManager.LocalNetworkConfig): Boolean {
        return config.isValidConfig()
    }
    
    /**
     * Obtener configuración actual de manera síncrona
     */
    suspend fun getCurrentConfig(): NetworkConfigManager.LocalNetworkConfig {
        return networkConfigManager.localNetworkConfig
            .stateIn(viewModelScope)
            .value
    }
}
