package com.fintrack.di

import android.content.Context
import com.fintrack.data.local.dao.BankAccountDao
import com.fintrack.data.local.dao.CreditCardDao
import com.fintrack.data.local.dao.KittyDao
import com.fintrack.data.local.dao.LedgerDao
import com.fintrack.data.local.db.AppDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    fun provideKittyDao(db: AppDatabase): KittyDao = db.kittyDao()

    @Provides
    fun provideCreditCardDao(db: AppDatabase): CreditCardDao = db.creditCardDao()

    @Provides
    fun provideBankAccountDao(db: AppDatabase): BankAccountDao = db.bankAccountDao()

    @Provides
    fun provideLedgerDao(db: AppDatabase): LedgerDao = db.ledgerDao()
}

