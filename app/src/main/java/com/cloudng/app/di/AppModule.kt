package com.cloudng.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.cloudng.app.core.CoreBridge
import com.cloudng.app.core.XrayCoreBridge
import com.cloudng.app.data.db.*
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cloudng_settings")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CloudNGDatabase =
        Room.databaseBuilder(context, CloudNGDatabase::class.java, "cloudng.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideProfileDao(db: CloudNGDatabase): ProfileDao = db.profileDao()
    @Provides fun provideSubscriptionDao(db: CloudNGDatabase): SubscriptionDao = db.subscriptionDao()
    @Provides fun provideRoutingRuleDao(db: CloudNGDatabase): RoutingRuleDao = db.routingRuleDao()
    @Provides fun provideLogEventDao(db: CloudNGDatabase): LogEventDao = db.logEventDao()
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {
    @Binds
    @Singleton
    abstract fun bindCoreBridge(impl: XrayCoreBridge): CoreBridge
}
