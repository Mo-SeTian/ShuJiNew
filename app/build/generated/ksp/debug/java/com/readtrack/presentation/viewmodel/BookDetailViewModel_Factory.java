package com.readtrack.presentation.viewmodel;

import androidx.lifecycle.SavedStateHandle;
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
public final class BookDetailViewModel_Factory implements Factory<BookDetailViewModel> {
  private final Provider<BookRepository> bookRepositoryProvider;

  private final Provider<ReadingRecordRepository> recordRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public BookDetailViewModel_Factory(Provider<BookRepository> bookRepositoryProvider,
      Provider<ReadingRecordRepository> recordRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.bookRepositoryProvider = bookRepositoryProvider;
    this.recordRepositoryProvider = recordRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public BookDetailViewModel get() {
    return newInstance(bookRepositoryProvider.get(), recordRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static BookDetailViewModel_Factory create(Provider<BookRepository> bookRepositoryProvider,
      Provider<ReadingRecordRepository> recordRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new BookDetailViewModel_Factory(bookRepositoryProvider, recordRepositoryProvider, savedStateHandleProvider);
  }

  public static BookDetailViewModel newInstance(BookRepository bookRepository,
      ReadingRecordRepository recordRepository, SavedStateHandle savedStateHandle) {
    return new BookDetailViewModel(bookRepository, recordRepository, savedStateHandle);
  }
}
