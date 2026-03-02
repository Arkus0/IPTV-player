package com.neutraltv.player.di

import com.neutraltv.player.data.parser.M3uParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideM3uParser(): M3uParser {
        return M3uParser()
    }
}
