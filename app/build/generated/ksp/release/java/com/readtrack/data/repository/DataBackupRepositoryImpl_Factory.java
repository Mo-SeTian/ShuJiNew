package com.readtrack.data.repository;

import com.readtrack.data.local.dao.BookDao;
import com.readtrack.data.local.dao.BookListDao;
import com.readtrack.data.local.dao.ReadingRecordDao;
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
public final class DataBackupRepositoryImpl_Factory implements Factory<DataBackupRepositoryImpl> {
  private final Provider<BookDao> bookDaoProvider;

  private final Provider<ReadingRecordDao> recordDaoProvider;

  private final Provider<BookListDao> bookListDaoProvider;

  public DataBackupRepositoryImpl_Factory(Provider<BookDao> bookDaoProvider,
      Provider<ReadingRecordDao> recordDaoProvider, Provider<BookListDao> bookListDaoProvider) {
    this.bookDaoProvider = bookDaoProvider;
    this.recordDaoProvider = recordDaoProvider;
    this.bookListDaoProvider = bookListDaoProvider;
  }

  @Override
  public DataBackupRepositoryImpl get() {
    return newInstance(bookDaoProvider.get(), recordDaoProvider.get(), bookListDaoProvider.get());
  }

  public static DataBackupRepositoryImpl_Factory create(Provider<BookDao> bookDaoProvider,
      Provider<ReadingRecordDao> recordDaoProvider, Provider<BookListDao> bookListDaoProvider) {
    return new DataBackupRepositoryImpl_Factory(bookDaoProvider, recordDaoProvider, bookListDaoProvider);
  }

  public static DataBackupRepositoryImpl newInstance(BookDao bookDao, ReadingRecordDao recordDao,
      BookListDao bookListDao) {
    return new DataBackupRepositoryImpl(bookDao, recordDao, bookListDao);
  }
}
