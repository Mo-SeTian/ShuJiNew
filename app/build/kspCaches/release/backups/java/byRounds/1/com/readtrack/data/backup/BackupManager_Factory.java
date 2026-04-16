package com.readtrack.data.backup;

import android.content.Context;
import com.readtrack.domain.repository.BookRepository;
import com.readtrack.domain.repository.ReadingRecordRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class BackupManager_Factory implements Factory<BackupManager> {
  private final Provider<Context> contextProvider;

  private final Provider<BookRepository> bookRepositoryProvider;

  private final Provider<ReadingRecordRepository> recordRepositoryProvider;

  public BackupManager_Factory(Provider<Context> contextProvider,
      Provider<BookRepository> bookRepositoryProvider,
      Provider<ReadingRecordRepository> recordRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.bookRepositoryProvider = bookRepositoryProvider;
    this.recordRepositoryProvider = recordRepositoryProvider;
  }

  @Override
  public BackupManager get() {
    return newInstance(contextProvider.get(), bookRepositoryProvider.get(), recordRepositoryProvider.get());
  }

  public static BackupManager_Factory create(Provider<Context> contextProvider,
      Provider<BookRepository> bookRepositoryProvider,
      Provider<ReadingRecordRepository> recordRepositoryProvider) {
    return new BackupManager_Factory(contextProvider, bookRepositoryProvider, recordRepositoryProvider);
  }

  public static BackupManager newInstance(Context context, BookRepository bookRepository,
      ReadingRecordRepository recordRepository) {
    return new BackupManager(context, bookRepository, recordRepository);
  }
}
