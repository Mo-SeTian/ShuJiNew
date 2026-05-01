package com.readtrack.presentation.viewmodel;

import com.readtrack.data.local.PreferencesManager;
import com.readtrack.domain.repository.BookRepository;
import com.readtrack.domain.repository.ReadingRecordRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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
public final class StatsViewModel_Factory implements Factory<StatsViewModel> {
  private final Provider<BookRepository> bookRepositoryProvider;

  private final Provider<ReadingRecordRepository> recordRepositoryProvider;

  private final Provider<PreferencesManager> preferencesManagerProvider;

  public StatsViewModel_Factory(Provider<BookRepository> bookRepositoryProvider,
      Provider<ReadingRecordRepository> recordRepositoryProvider,
      Provider<PreferencesManager> preferencesManagerProvider) {
    this.bookRepositoryProvider = bookRepositoryProvider;
    this.recordRepositoryProvider = recordRepositoryProvider;
    this.preferencesManagerProvider = preferencesManagerProvider;
  }

  @Override
  public StatsViewModel get() {
    return newInstance(bookRepositoryProvider.get(), recordRepositoryProvider.get(), preferencesManagerProvider.get());
  }

  public static StatsViewModel_Factory create(Provider<BookRepository> bookRepositoryProvider,
      Provider<ReadingRecordRepository> recordRepositoryProvider,
      Provider<PreferencesManager> preferencesManagerProvider) {
    return new StatsViewModel_Factory(bookRepositoryProvider, recordRepositoryProvider, preferencesManagerProvider);
  }

  public static StatsViewModel newInstance(BookRepository bookRepository,
      ReadingRecordRepository recordRepository, PreferencesManager preferencesManager) {
    return new StatsViewModel(bookRepository, recordRepository, preferencesManager);
  }
}
