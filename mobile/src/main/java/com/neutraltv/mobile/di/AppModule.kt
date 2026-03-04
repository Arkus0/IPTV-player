package com.neutraltv.mobile.di

import android.content.Context
import com.neutraltv.core.discovery.NsdDiscoveryManager
import com.neutraltv.core.transfer.TransferSoundPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNsdDiscoveryManager(@ApplicationContext context: Context): NsdDiscoveryManager =
        NsdDiscoveryManager(context)

    @Provides
    @Singleton
    fun provideTransferSoundPlayer(@ApplicationContext context: Context): TransferSoundPlayer =
        TransferSoundPlayer(context)
}
