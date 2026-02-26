package com.example.amulet.data.devices.di

import com.example.amulet.data.devices.datasource.ble.DevicesBleDataSource
import com.example.amulet.data.devices.datasource.ble.DevicesBleDataSourceImpl
import com.example.amulet.data.devices.datasource.ble.OtaBleDataSource
import com.example.amulet.data.devices.datasource.ble.OtaBleDataSourceImpl
import com.example.amulet.data.devices.datasource.local.DevicesLocalDataSource
import com.example.amulet.data.devices.datasource.local.DevicesLocalDataSourceImpl
import com.example.amulet.data.devices.datasource.local.OtaLocalDataSource
import com.example.amulet.data.devices.datasource.local.OtaLocalDataSourceImpl
import com.example.amulet.data.devices.datasource.remote.OtaRemoteDataSource
import com.example.amulet.data.devices.datasource.remote.OtaRemoteDataSourceImpl
import com.example.amulet.data.devices.repository.DeviceConnectionRepositoryImpl
import com.example.amulet.data.devices.repository.DeviceControlRepositoryImpl
import com.example.amulet.data.devices.repository.DeviceRegistryRepositoryImpl
import com.example.amulet.data.devices.repository.OtaRepositoryImpl
import com.example.amulet.shared.domain.devices.repository.DeviceConnectionRepository
import com.example.amulet.shared.domain.devices.repository.DeviceControlRepository
import com.example.amulet.shared.domain.devices.repository.DeviceRegistryRepository
import com.example.amulet.shared.domain.devices.repository.OtaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI модуль для Data Layer устройств и OTA.
 * Devices работают только локально (БД + BLE), OTA сохраняет remote источник.
 */
@Module
@InstallIn(SingletonComponent::class)
interface DevicesDataModule {
    
    // Repositories
    
    @Binds
    @Singleton
    fun bindDeviceRegistryRepository(impl: DeviceRegistryRepositoryImpl): DeviceRegistryRepository
    
    @Binds
    @Singleton
    fun bindDeviceConnectionRepository(impl: DeviceConnectionRepositoryImpl): DeviceConnectionRepository
    
    @Binds
    @Singleton
    fun bindDeviceControlRepository(impl: DeviceControlRepositoryImpl): DeviceControlRepository
    
    @Binds
    @Singleton
    fun bindOtaRepository(impl: OtaRepositoryImpl): OtaRepository
    
    // Remote Data Sources (только для OTA)
    
    @Binds
    @Singleton
    fun bindOtaRemoteDataSource(impl: OtaRemoteDataSourceImpl): OtaRemoteDataSource
    
    // Local Data Sources
    
    @Binds
    @Singleton
    fun bindDevicesLocalDataSource(impl: DevicesLocalDataSourceImpl): DevicesLocalDataSource
    
    @Binds
    @Singleton
    fun bindOtaLocalDataSource(impl: OtaLocalDataSourceImpl): OtaLocalDataSource
    
    // BLE Data Sources
    
    @Binds
    @Singleton
    fun bindDevicesBleDataSource(impl: DevicesBleDataSourceImpl): DevicesBleDataSource
    
    @Binds
    @Singleton
    fun bindOtaBleDataSource(impl: OtaBleDataSourceImpl): OtaBleDataSource
}
