package cc.tomko.outify

import android.content.Context
import cc.tomko.outify.data.Metadata
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
    fun provideMetadata(@ApplicationContext context: Context): Metadata {
        return (context.applicationContext as OutifyApplication).metadata
    }
}