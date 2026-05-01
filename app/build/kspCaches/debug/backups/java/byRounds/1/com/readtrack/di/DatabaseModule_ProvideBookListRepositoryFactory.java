package com.readtrack.di;

import com.readtrack.data.local.dao.BookDao;
import com.readtrack.data.local.dao.BookListDao;
import com.readtrack.domain.repository.BookListRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideBookListRepositoryFactory implements Factory<BookListRepository> {
  private final Provider<BookListDao> bookListDaoProvider;

  private final Provider<BookDao> bookDaoProvider;

  public DatabaseModule_ProvideBookListRepositoryFactory(Provider<BookListDao> bookListDaoProvider,
      Provider<BookDao> bookDaoProvider) {
    this.bookListDaoProvider = bookListDaoProvider;
    this.bookDaoProvider = bookDaoProvider;
  }

  @Override
  public BookListRepository get() {
    return provideBookListRepository(bookListDaoProvider.get(), bookDaoProvider.get());
  }

  public static DatabaseModule_ProvideBookListRepositoryFactory create(
      Provider<BookListDao> bookListDaoProvider, Provider<BookDao> bookDaoProvider) {
    return new DatabaseModule_ProvideBookListRepositoryFactory(bookListDaoProvider, bookDaoProvider);
  }

  public static BookListRepository provideBookListRepository(BookListDao bookListDao,
      BookDao bookDao) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBookListRepository(bookListDao, bookDao));
  }
}
