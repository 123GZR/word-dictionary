package com.galaxy.worddictionary.di

import com.galaxy.worddictionary.data.DictionaryRepository
import com.galaxy.worddictionary.data.LocalDictionaryRepository
import com.galaxy.worddictionary.data.LocalWrongBookRepository
import com.galaxy.worddictionary.data.WrongBookRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt 依赖注入装配模块。
 *
 * [DataProvidesModule]：提供需要手工构造的绑定（IO 调度器等）。
 * [DataBindsModule]：接口 → 实现的绑定（实现类带 [javax.inject.Inject] 构造注解）。
 */

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {

    /** IO 调度器绑定：数据层/ViewModel 磁盘与计算任务统一注入 */
    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineContext = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {

    @Binds
    @Singleton
    abstract fun bindDictionary(impl: LocalDictionaryRepository): DictionaryRepository

    @Binds
    @Singleton
    abstract fun bindWrongBook(impl: LocalWrongBookRepository): WrongBookRepository
}
