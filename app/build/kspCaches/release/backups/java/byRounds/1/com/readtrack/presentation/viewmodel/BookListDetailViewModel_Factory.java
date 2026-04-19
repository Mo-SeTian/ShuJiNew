package com.readtrack.presentation.viewmodel;

import com.readtrack.domain.repository.BookListRepository;
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
public final class BookListDetailViewModel_Factory implements Factory<BookListDetailViewModel> {
  private final Provider<BookListRepository> bookListRepositoryProvider;

  public BookListDetailViewModel_Factory(Provider<BookListRepository> bookListRepositoryProvider) {
    this.bookListRepositoryProvider = bookListRepositoryProvider;
  }

  @Override
  public BookListDetailViewModel get() {
    return newInstance(bookListRepositoryProvider.get());
  }

  public static BookListDetailViewModel_Factory create(
      Provider<BookListRepository> bookListRepositoryProvider) {
    return new BookListDetailViewModel_Factory(bookListRepositoryProvider);
  }

  public static BookListDetailViewModel newInstance(BookListRepository bookListRepository) {
    return new BookListDetailViewModel(bookListRepository);
  }
}
