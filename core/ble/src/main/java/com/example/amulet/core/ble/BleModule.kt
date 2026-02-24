package com.example.amulet.core.ble

import com.example.amulet.core.ble.internal.AmuletDeviceImpl
import com.example.amulet.core.ble.internal.DeviceCommandSenderImpl
import com.example.amulet.core.ble.internal.DeviceConnectionManagerImpl
import com.example.amulet.core.ble.internal.DeviceStateManagerImpl
import com.example.amulet.core.ble.protocol.AmuletProtocolParser
import com.example.amulet.core.ble.service.AnimationUploadService
import com.example.amulet.core.ble.service.AnimationUploadServiceImpl
import com.example.amulet.core.ble.service.OtaUpdateService
import com.example.amulet.core.ble.service.OtaUpdateServiceImpl
import com.example.amulet.core.ble.transport.BleGattClient
import com.example.amulet.core.ble.transport.BleGattClientImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BleModule {

    @Binds
    @Singleton
    abstract fun bindBleGattClient(
        impl: BleGattClientImpl
    ): BleGattClient

    @Binds
    @Singleton
    abstract fun bindAmuletProtocolParser(
        impl: AmuletProtocolParser
    ): AmuletProtocolParser

    @Binds
    @Singleton
    abstract fun bindDeviceConnectionManager(
        impl: DeviceConnectionManagerImpl
    ): DeviceConnectionManager

    @Binds
    @Singleton
    abstract fun bindDeviceStateManager(
        impl: DeviceStateManagerImpl
    ): DeviceStateManager

    @Binds
    @Singleton
    abstract fun bindDeviceCommandSender(
        impl: DeviceCommandSenderImpl
    ): DeviceCommandSender

    @Binds
    @Singleton
    abstract fun bindAmuletDevice(
        impl: AmuletDeviceImpl
    ): AmuletDevice

    companion object {
        @Provides
        @Singleton
        fun provideOtaUpdateService(
            commandSender: DeviceCommandSender,
            flowControlManager: com.example.amulet.core.ble.internal.FlowControlManager
        ): OtaUpdateService = OtaUpdateServiceImpl(commandSender, flowControlManager)

        @Provides
        @Singleton
        fun provideAnimationUploadService(
            commandSender: DeviceCommandSender,
            flowControlManager: com.example.amulet.core.ble.internal.FlowControlManager
        ): AnimationUploadService = AnimationUploadServiceImpl(commandSender, flowControlManager)
    }
}
