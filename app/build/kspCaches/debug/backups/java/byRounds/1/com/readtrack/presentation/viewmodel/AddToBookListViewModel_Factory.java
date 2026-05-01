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
public final class AddToBookListViewModel_Factory implements Factory<AddToBookListViewModel> {
  private final Provider<BookListRepository> bookListRepositoryProvider;

  public AddToBookListViewModel_Factory(Provider<BookListRepository> bookListRepositoryProvider) {
    this.bookListRepositoryProvider = bookListRepositoryProvider;
  }

  @Override
  public AddToBookListViewModel get() {
    return newInstance(bookListRepositoryProvider.get());
  }

  public static AddToBookListViewModel_Factory create(
      Provider<BookListRepository> bookListRepositoryProvider) {
    return new AddToBookListViewModel_Factory(bookListRepositoryProvider);
  }

  public static AddToBookListViewModel newInstance(BookListRepository bookListRepository) {
    return new AddToBookListViewModel(bookListRepository);
  }
}
