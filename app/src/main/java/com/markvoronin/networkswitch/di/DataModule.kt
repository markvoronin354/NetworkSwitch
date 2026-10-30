package com.markvoronin.networkswitch.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.markvoronin.networkswitch.data.repository.NetworkControlRepositoryImpl
import com.markvoronin.networkswitch.data.repository.NetworkStatsRepositoryImpl
import com.markvoronin.networkswitch.data.repository.PreferencesRepositoryImpl
import com.markvoronin.networkswitch.domain.repository.NetworkControlRepository
import com.markvoronin.networkswitch.domain.repository.NetworkStatsRepository
import com.markvoronin.networkswitch.domain.repository.PreferencesRepository
import com.markvoronin.networkswitch.domain.repository.SurfaceRefresher
import com.markvoronin.networkswitch.service.AndroidSurfaceRefresher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "network_switch_preferences")

/**
 * Dependency injection module for data layer
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    
    @Binds
    @Singleton
    abstract fun bindNetworkControlRepository(
        networkControlRepositoryImpl: NetworkControlRepositoryImpl
    ): NetworkControlRepository
    
    @Binds
    @Singleton
    abstract fun bindNetworkStatsRepository(
        networkStatsRepositoryImpl: NetworkStatsRepositoryImpl
    ): NetworkStatsRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        preferencesRepositoryImpl: PreferencesRepositoryImpl
    ): PreferencesRepository
    
    @Binds
    @Singleton
    abstract fun bindSurfaceRefresher(
        androidSurfaceRefresher: AndroidSurfaceRefresher
    ): SurfaceRefresher

    companion object {
        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
            return context.dataStore
        }
    }
}
