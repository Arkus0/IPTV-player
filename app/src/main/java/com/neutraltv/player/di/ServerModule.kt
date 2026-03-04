package com.neutraltv.player.di

import android.content.Context
import com.neutraltv.core.discovery.NsdDiscoveryManager
import com.neutraltv.player.server.PlaybackBridge
import com.neutraltv.player.server.PlaybackStateProvider
import com.neutraltv.player.server.RemoteCommandHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServerModule {

    @Provides
    @Singleton
    fun providePlaybackBridge(): PlaybackBridge = PlaybackBridge()

    @Provides
    @Singleton
    fun providePlaybackStateProvider(bridge: PlaybackBridge): PlaybackStateProvider = bridge

    @Provides
    @Singleton
    fun provideRemoteCommandHandler(bridge: PlaybackBridge): RemoteCommandHandler = bridge

    @Provides
    @Singleton
    fun provideNsdDiscoveryManager(@ApplicationContext context: Context): NsdDiscoveryManager =
        NsdDiscoveryManager(context)
}
