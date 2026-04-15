package com.readtrack.presentation.viewmodel;

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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<BookRepository> bookRepositoryProvider;

  private final Provider<ReadingRecordRepository> recordRepositoryProvider;

  public HomeViewModel_Factory(Provider<BookRepository> bookRepositoryProvider,
      Provider<ReadingRecordRepository> recordRepositoryProvider) {
    this.bookRepositoryProvider = bookRepositoryProvider;
    this.recordRepositoryProvider = recordRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(bookRepositoryProvider.get(), recordRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<BookRepository> bookRepositoryProvider,
      Provider<ReadingRecordRepository> recordRepositoryProvider) {
    return new HomeViewModel_Factory(bookRepositoryProvider, recordRepositoryProvider);
  }

  public static HomeViewModel newInstance(BookRepository bookRepository,
      ReadingRecordRepository recordRepository) {
    return new HomeViewModel(bookRepository, recordRepository);
  }
}
