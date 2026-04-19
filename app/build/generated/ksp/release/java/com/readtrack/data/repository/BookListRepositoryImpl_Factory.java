package com.readtrack.data.repository;

import com.readtrack.data.local.dao.BookDao;
import com.readtrack.data.local.dao.BookListDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class BookListRepositoryImpl_Factory implements Factory<BookListRepositoryImpl> {
  private final Provider<BookListDao> bookListDaoProvider;

  private final Provider<BookDao> bookDaoProvider;

  public BookListRepositoryImpl_Factory(Provider<BookListDao> bookListDaoProvider,
      Provider<BookDao> bookDaoProvider) {
    this.bookListDaoProvider = bookListDaoProvider;
    this.bookDaoProvider = bookDaoProvider;
  }

  @Override
  public BookListRepositoryImpl get() {
    return newInstance(bookListDaoProvider.get(), bookDaoProvider.get());
  }

  public static BookListRepositoryImpl_Factory create(Provider<BookListDao> bookListDaoProvider,
      Provider<BookDao> bookDaoProvider) {
    return new BookListRepositoryImpl_Factory(bookListDaoProvider, bookDaoProvider);
  }

  public static BookListRepositoryImpl newInstance(BookListDao bookListDao, BookDao bookDao) {
    return new BookListRepositoryImpl(bookListDao, bookDao);
  }
}
