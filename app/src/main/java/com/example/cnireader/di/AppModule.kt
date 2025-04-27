package com.example.cnireader.di

import com.example.cnireader.data.PassportRepository
import com.example.cnireader.data.PassportRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindPassportRepository(
        passportRepositoryImpl: PassportRepositoryImpl
    ): PassportRepository
}
