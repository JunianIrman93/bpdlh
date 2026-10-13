package com.laporlh.app.di

import android.content.Context
import androidx.room.Room
import com.laporlh.app.core.data.local.AppDatabase
import com.laporlh.app.core.data.local.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "laporlh.db").build()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()
}
