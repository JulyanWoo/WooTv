package com.wootv.app.di

import com.wootv.app.data.repository.ChannelRepositoryImpl
import com.wootv.app.data.repository.EpgRepositoryImpl
import com.wootv.app.data.repository.PlaylistRepositoryImpl
import com.wootv.app.data.repository.RecentRepositoryImpl
import com.wootv.app.domain.repository.ChannelRepository
import com.wootv.app.domain.repository.EpgRepository
import com.wootv.app.domain.repository.PlaylistRepository
import com.wootv.app.domain.repository.RecentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds @Singleton
    abstract fun bindEpgRepository(impl: EpgRepositoryImpl): EpgRepository

    @Binds @Singleton
    abstract fun bindRecentRepository(impl: RecentRepositoryImpl): RecentRepository
}
