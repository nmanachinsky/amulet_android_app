package com.example.amulet.shared.di

import com.example.amulet.shared.domain.auth.usecase.EnableGuestModeUseCase
import com.example.amulet.shared.domain.auth.usecase.GetCurrentUserIdUseCase
import com.example.amulet.shared.domain.auth.usecase.ObserveCurrentUserIdUseCase
import com.example.amulet.shared.domain.auth.usecase.SignInUseCase
import com.example.amulet.shared.domain.auth.usecase.SignInWithGoogleUseCase
import com.example.amulet.shared.domain.auth.usecase.SignOutUseCase
import com.example.amulet.shared.domain.auth.usecase.SignUpUseCase
import com.example.amulet.shared.domain.courses.usecase.*
import com.example.amulet.shared.domain.devices.usecase.*
import com.example.amulet.shared.domain.hugs.DefaultSendHugUseCase
import com.example.amulet.shared.domain.hugs.DeviceHugSendListener
import com.example.amulet.shared.domain.hugs.ObserveHugsForPairUseCase
import com.example.amulet.shared.domain.hugs.ObserveHugsForUserUseCase
import com.example.amulet.shared.domain.hugs.ObservePairEmotionsUseCase
import com.example.amulet.shared.domain.hugs.ObservePairQuickRepliesUseCase
import com.example.amulet.shared.domain.hugs.ObservePairUseCase
import com.example.amulet.shared.domain.hugs.ObservePairsUseCase
import com.example.amulet.shared.domain.hugs.SendHugUseCase
import com.example.amulet.shared.domain.hugs.UpdateHugStatusUseCase
import com.example.amulet.shared.domain.hugs.UpdatePairEmotionsUseCase
import com.example.amulet.shared.domain.hugs.UpdatePairMemberSettingsUseCase
import com.example.amulet.shared.domain.hugs.UpdatePairQuickRepliesUseCase
import com.example.amulet.shared.domain.hugs.ExecuteRemoteHugCommandUseCase
import com.example.amulet.shared.domain.hugs.SetHugsDndEnabledUseCase
import com.example.amulet.shared.domain.hugs.BlockPairUseCase
import com.example.amulet.shared.domain.hugs.DeletePairUseCase
import com.example.amulet.shared.domain.hugs.UnblockPairUseCase
import com.example.amulet.shared.domain.hugs.SendQuickReplyByGestureUseCase
import com.example.amulet.shared.domain.hugs.GetSecretCodesUseCase
import com.example.amulet.shared.domain.hugs.GetHugByIdUseCase
import com.example.amulet.shared.domain.hugs.SyncHugsUseCase
import com.example.amulet.shared.domain.hugs.SyncHugsAndEnsurePatternsUseCase
import com.example.amulet.shared.domain.hugs.SyncPairsUseCase
import com.example.amulet.shared.domain.hugs.SyncPairsAndFetchMemberProfilesUseCase
import com.example.amulet.shared.domain.hugs.InvitePairUseCase
import com.example.amulet.shared.domain.hugs.AcceptPairUseCase
import com.example.amulet.shared.domain.notifications.SyncPushTokenUseCase
import com.example.amulet.shared.domain.privacy.usecase.GetUserConsentsUseCase
import com.example.amulet.shared.domain.privacy.usecase.UpdateUserConsentsUseCase
import com.example.amulet.shared.domain.privacy.usecase.RequestDataExportUseCase
import com.example.amulet.shared.domain.privacy.usecase.RequestAccountDeletionUseCase
import com.example.amulet.shared.domain.initialization.usecase.SeedLocalDataUseCase
import com.example.amulet.shared.domain.patterns.PatternPlaybackService
import com.example.amulet.shared.domain.patterns.compiler.DeviceTimelineCompiler
import com.example.amulet.shared.domain.patterns.compiler.DeviceTimelineCompilerImpl
import com.example.amulet.shared.domain.patterns.usecase.*
import com.example.amulet.shared.domain.practices.PracticeSessionManager
import com.example.amulet.shared.domain.practices.PracticeSessionManagerImpl
import com.example.amulet.shared.domain.practices.usecase.*
import com.example.amulet.shared.domain.user.usecase.FetchUserProfileUseCase
import com.example.amulet.shared.domain.user.usecase.ObserveCurrentUserUseCase
import com.example.amulet.shared.domain.user.usecase.ObserveUserByIdUseCase
import com.example.amulet.shared.domain.user.usecase.UpdateUserProfileUseCase
import com.example.amulet.shared.domain.dashboard.usecase.GetDashboardDailyStatsUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin модули для :shared (domain layer).
 * Предоставляет UseCase'ы, которые зависят только от интерфейсов репозиториев.
 */
private val sharedModule = module {
    // Auth UseCases
    factory { SignInUseCase(get(), get()) }
    factory { SignInWithGoogleUseCase(get(), get()) }
    factory { SignOutUseCase(get()) }
    factory { SignUpUseCase(get(), get()) }
    factory { EnableGuestModeUseCase(get()) }
    factory { ObserveCurrentUserIdUseCase(get()) }
    factory { GetCurrentUserIdUseCase(get()) }
    
    // Initialization UseCases
    factory { SeedLocalDataUseCase(get(), get(), get()) }
    
    // User UseCases
    factory { ObserveCurrentUserUseCase(get(), get()) }
    factory { ObserveUserByIdUseCase(get()) }
    factory { UpdateUserProfileUseCase(get()) }
    factory { FetchUserProfileUseCase(get()) }

    // Hugs UseCases
    factory<SendHugUseCase> { DefaultSendHugUseCase(get(), get()) }
    factory { ObserveHugsForPairUseCase(get()) }
    factory { ObserveHugsForUserUseCase(get()) }
    factory { UpdateHugStatusUseCase(get()) }
    factory { GetHugByIdUseCase(get()) }
    factory { SyncHugsUseCase(get()) }
    factory { SyncHugsAndEnsurePatternsUseCase(get(), get()) }
    factory { ExecuteRemoteHugCommandUseCase(get(), get(), get(), get(), get(), get()) }
    factory { SetHugsDndEnabledUseCase(get(), get()) }
    factory { BlockPairUseCase(get()) }
    factory { UnblockPairUseCase(get()) }
    factory { DeletePairUseCase(get()) }
    factory { SendQuickReplyByGestureUseCase(get(), get()) }
    factory { GetSecretCodesUseCase(get()) }
    factory { ObservePairsUseCase(get()) }
    factory { ObservePairUseCase(get()) }
    factory { ObservePairEmotionsUseCase(get()) }
    factory { UpdatePairEmotionsUseCase(get()) }
    factory { ObservePairQuickRepliesUseCase(get()) }
    factory { UpdatePairQuickRepliesUseCase(get()) }
    factory { UpdatePairMemberSettingsUseCase(get()) }
    factory { InvitePairUseCase(get()) }
    factory { AcceptPairUseCase(get()) }
    factory { SyncPairsUseCase(get()) }
    factory { SyncPairsAndFetchMemberProfilesUseCase(get(), get(), get(), get(), get(), get()) }

    // Hugs BLE bridge listener
    single { DeviceHugSendListener(get(), get(), get(), get()) }

    // Notifications UseCases
    factory { SyncPushTokenUseCase(get(), get()) }

    // Privacy UseCases
    factory { GetUserConsentsUseCase(get()) }
    factory { UpdateUserConsentsUseCase(get()) }
    factory { RequestDataExportUseCase(get()) }
    factory { RequestAccountDeletionUseCase(get()) }

    // Devices UseCases (локальная работа без серверной привязки)
    factory { ObserveDevicesUseCase(get()) }
    factory { GetDeviceUseCase(get()) }
    factory { AddDeviceUseCase(get()) }
    factory { RemoveDeviceUseCase(get()) }
    factory { ScanForDevicesUseCase(get()) }
    factory { ConnectToDeviceUseCase(get()) }
    factory { DisconnectFromDeviceUseCase(get()) }
    factory { ObserveConnectionStateUseCase(get()) }
    factory { ObserveConnectedDeviceStatusUseCase(get()) }
    factory { ObserveDeviceSessionStatusUseCase(get(), get()) }
    factory { UpdateDeviceSettingsUseCase(get()) }
    factory { ApplyDeviceBrightnessUseCase(get()) }
    factory { ApplyDeviceHapticsUseCase(get()) }
    factory { AutoConnectLastDeviceUseCase(get()) }

    // OTA UseCases
    factory { CheckFirmwareUpdateUseCase(get()) }
    factory { StartBleOtaUpdateUseCase(get()) }
    factory { StartWifiOtaUpdateUseCase(get()) }
    factory { CancelOtaUpdateUseCase(get()) }
    
    // Patterns playback (PatternTimeline -> DeviceTimelineSegment -> DeviceAnimationPlan)
    single<DeviceTimelineCompiler> { DeviceTimelineCompilerImpl() }
    single { PatternPlaybackService(get(), get(), get(), get()) }
    
    // Patterns UseCases
    factory { PatternValidator() }
    factory { CreatePatternUseCase(get(), get()) }
    factory { UpdatePatternUseCase(get(), get()) }
    factory { DeletePatternUseCase(get()) }
    factory { GetPatternsStreamUseCase(get()) }
    factory { GetPresetsUseCase(get()) }
    factory { GetPatternByIdUseCase(get()) }
    factory { EnsurePatternLoadedUseCase(get()) }
    factory { ObserveMyPatternsUseCase(get()) }
    factory { SyncPatternsUseCase(get()) }
    factory { PublishPatternUseCase(get()) }
    factory { SharePatternUseCase(get()) }
    factory { AddTagToPatternUseCase(get()) }
    factory { RemoveTagFromPatternUseCase(get()) }
    factory { GetAllTagsUseCase(get()) }
    factory { CreateTagsUseCase(get()) }
    factory { SetPatternTagsUseCase(get()) }
    factory { DeleteTagsUseCase(get()) }
    factory { PreviewPatternOnDeviceUseCase(get()) }
    factory { ClearCurrentDevicePatternUseCase(get()) }
    factory { SlicePatternIntoSegmentsUseCase() }
    factory { ApplyPatternSegmentationUseCase(get(), get()) }
    factory { GetPatternMarkersUseCase(get()) }
    factory { UpsertPatternMarkersUseCase(get()) }
    factory { GetPatternSegmentsUseCase(get()) }
    factory { PatternEditorFacade(get(), get(), get(), get()) }

    // Practices UseCases
    factory { GetPracticesStreamUseCase(get()) }
    factory { GetPracticeByIdUseCase(get()) }
    factory { GetCategoriesStreamUseCase(get()) }
    factory { GetFavoritesStreamUseCase(get()) }
    factory { SearchPracticesUseCase(get()) }
    factory { RefreshPracticesUseCase(get()) }
    factory { UpsertPracticeUseCase(get()) }
    factory { SetFavoritePracticeUseCase(get()) }
    factory { GetActiveSessionStreamUseCase(get()) }
    factory { GetSessionsHistoryStreamUseCase(get()) }
    factory { GetScheduledSessionsStreamUseCase(get(), get()) }
    factory { GetScheduledSessionsForDateRangeUseCase(get(), get()) }
    factory { RefreshPracticesCatalogUseCase(get()) }
    factory { StartPracticeUseCase(get()) }
    factory { StopSessionUseCase(get()) }
    factory { GetUserPreferencesStreamUseCase(get()) }
    factory { UpdateUserPreferencesUseCase(get()) }
    factory { UpdatePracticeDefaultsUseCase(get(), get()) }
    factory { GetRecommendationsStreamUseCase(get()) }
    factory { UpsertPracticeScheduleUseCase(get()) }
    factory { GetScheduleByPracticeIdUseCase(get()) }
    factory { DeletePracticeScheduleUseCase(get()) }
    factory { DeleteSchedulesForCourseUseCase(get()) }
    factory { SkipScheduledSessionUseCase(get()) }
    factory { LogMoodSelectionUseCase(get()) }
    factory { GetPracticeScriptUseCase(get()) }
    factory { UpdateSessionFeedbackUseCase(get(), get()) }
    factory { UpdateSessionMoodBeforeUseCase(get(), get()) }
    factory { GetDashboardDailyStatsUseCase(get(), get(), get(), get()) }
    factory { UploadPracticeScriptToDeviceUseCase(get()) }
    factory { PlayPracticeScriptOnDeviceUseCase(get()) }
    factory { HasPracticeScriptOnDeviceUseCase(get()) }

    // Practices Manager
    factory<PracticeSessionManager> {
        PracticeSessionManagerImpl(
            startPractice = get(),
            stopSessionUseCase = get(),
            getActiveSessionStreamUseCase = get(),
            getPracticeById = get(),
            patternPlaybackService = get(),
            uploadPracticeScriptToDevice = get(),
            playPracticeScriptOnDevice = get(),
            hasPracticeScriptOnDevice = get(),
            observeConnectionStateUseCase = get()
        )
    }

    // Courses UseCases
    factory { GetCoursesStreamUseCase(get()) }
    factory { GetCourseByIdUseCase(get()) }
    factory { GetCourseItemsStreamUseCase(get()) }
    factory { GetCourseModulesStreamUseCase(get()) }
    factory { GetCourseProgressStreamUseCase(get(), get()) }
    factory { GetAllCoursesProgressStreamUseCase(get(), get()) }
    factory { GetCoursesByPracticeIdUseCase(get()) }
    factory { RefreshCoursesUseCase(get()) }
    factory { RefreshCoursesCatalogUseCase(get()) }
    factory { StartCourseUseCase(get(), get()) }
    factory { ContinueCourseUseCase(get(), get()) }
    factory { CompleteCourseItemUseCase(get(), get()) }
    factory { ResetCourseProgressUseCase(get(), get()) }
    factory { SearchCoursesUseCase(get()) }
    factory { CheckItemUnlockUseCase(get()) }
    factory { GetUnlockedItemsUseCase(get(), get()) }
    factory { GetNextCourseItemUseCase(get(), get()) }
    factory { CreateScheduleForCourseUseCase() }
    factory { EnrollCourseUseCase(get(), get(), get()) }

    // Complete practice session (нужен доступ к практикам и курсам для источника FromCourse)
    factory { CompletePracticeSessionUseCase(get(), get()) }
}

fun sharedKoinModules(): List<Module> = listOf(sharedModule)
