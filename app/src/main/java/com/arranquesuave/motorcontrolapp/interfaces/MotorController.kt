package com.arranquesuave.motorcontrolapp.interfaces

/**
 * INTERFACE UNIFICADA PARA CONTROL DE MOTOR
 * 
 * Define las operaciones comunes para todos los tipos de controladores:
 * - BluetoothMotorController (conexión directa ESP32)
 * - MqttMotorController (conexión remota vía WiFi/Internet)
 * 
 * Permite cambiar entre modos de conexión sin modificar la UI
 */
interface MotorController {
    
    /**
     * Establecer conexión con el sistema de control del motor
     * @return Result<Unit> - Success si se conecta, Failure con excepción si falla
     */
    suspend fun connect(): Result<Unit>
    
    /**
     * Desconectar del sistema de control del motor
     */
    suspend fun disconnect()
    
    /**
     * Enviar comando de arranque suave (6 pasos)
     * @param values Lista de 6 valores PWM (0-254) para cada paso
     * @return Result<Unit> - Success si se envía correctamente
     */
    suspend fun sendArranque6P(values: List<Int>): Result<Unit>
    
    /**
     * Enviar comando de arranque continuo
     * @return Result<Unit> - Success si se envía correctamente
     */
    suspend fun sendContinuo(): Result<Unit>
    
    /**
     * Enviar comando de paro de emergencia
     * @return Result<Unit> - Success si se envía correctamente
     */
    suspend fun sendParo(): Result<Unit>
    
    /**
     * Configurar callback para recibir velocidad del motor
     * @param callback Función que recibe la velocidad actual (0-254)
     */
    fun setOnSpeedReceived(callback: (Int) -> Unit)
    
    /**
     * Configurar callback para recibir estado del motor
     * @param callback Función que recibe el estado ("running", "stopped", "starting")
     */
    fun setOnStatusReceived(callback: (String) -> Unit)
    
    /**
     * Verificar si está conectado al sistema de control
     * @return Boolean - true si conectado, false si desconectado
     */
    fun isConnected(): Boolean
    
    /**
     * Obtener información de la conexión actual
     * @return String - Descripción de la conexión (IP, puerto, etc.)
     */
    fun getConnectionInfo(): String
    
    /**
     * Configurar callback para recibir corriente del motor (opcional)
     * @param callback Función que recibe la corriente en Amperios
     */
    fun setOnCurrentReceived(callback: (Float) -> Unit) {
        // Implementación por defecto vacía para controladores que no soportan telemetría extendida
    }
    
    /**
     * Configurar callback para recibir voltaje del motor (opcional)
     * @param callback Función que recibe el voltaje en Voltios
     */
    fun setOnVoltageReceived(callback: (Float) -> Unit) {
        // Implementación por defecto vacía para controladores que no soportan telemetría extendida
    }

    /**
     * Configurar callback para recibir el modo actual reportado por el motor (opcional)
     * @param callback Función que recibe el modo como cadena (arranque6p, continuo, paro, etc.)
     */
    fun setOnModeReceived(callback: (String) -> Unit) {
        // Implementación por defecto vacía
    }
}
