package com.example.amulet.shared.domain.devices.repository

import com.example.amulet.shared.core.AppResult
import com.example.amulet.shared.domain.devices.model.DeviceAnimationPlan
import com.example.amulet.shared.domain.devices.model.DeviceId
import com.example.amulet.shared.domain.devices.model.NotificationType
import com.example.amulet.shared.domain.practices.model.Practice
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для управления устройством (воспроизведение, загрузка данных).
 * Отвечает за отправку команд управления и загрузку данных на устройство.
 */
interface DeviceControlRepository {
    
    /**
     * Применить яркость устройства.
     */
    suspend fun setBrightness(deviceId: DeviceId, brightness: Double): AppResult<Unit>
    
    /**
     * Применить силу вибрации.
     */
    suspend fun setHaptics(deviceId: DeviceId, haptics: Double): AppResult<Unit>
    
    /**
     * Воспроизвести паттерн на устройстве.
     */
    suspend fun playPattern(patternId: String): AppResult<Unit>
    
    /**
     * Проверить наличие паттерна на устройстве.
     */
    suspend fun hasPattern(patternId: String): AppResult<Boolean>
    
    /**
     * Загрузить таймлайновый план на устройство.
     */
    fun uploadTimelinePlan(plan: DeviceAnimationPlan, hardwareVersion: Int): Flow<Int>
    
    /**
     * Загрузить скрипт практики на устройство.
     */
    suspend fun uploadPracticeScript(practice: Practice): AppResult<Unit>
    
    /**
     * Проверить наличие скрипта практики на устройстве.
     */
    suspend fun hasPracticeScript(practiceId: String): AppResult<Boolean>
    
    /**
     * Воспроизвести скрипт практики.
     */
    suspend fun playPracticeScript(practiceId: String): AppResult<Unit>
    
    /**
     * Очистить текущее устройство.
     */
    suspend fun clearDevice(): AppResult<Unit>
    
    /**
     * Наблюдать за уведомлениями от устройства.
     */
    fun observeNotifications(type: NotificationType? = null): Flow<String>
}
