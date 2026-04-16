package com.readtrack.data.repository;

import com.readtrack.data.local.dao.BookDao;
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

  public DataBackupRepositoryImpl_Factory(Provider<BookDao> bookDaoProvider,
      Provider<ReadingRecordDao> recordDaoProvider) {
    this.bookDaoProvider = bookDaoProvider;
    this.recordDaoProvider = recordDaoProvider;
  }

  @Override
  public DataBackupRepositoryImpl get() {
    return newInstance(bookDaoProvider.get(), recordDaoProvider.get());
  }

  public static DataBackupRepositoryImpl_Factory create(Provider<BookDao> bookDaoProvider,
      Provider<ReadingRecordDao> recordDaoProvider) {
    return new DataBackupRepositoryImpl_Factory(bookDaoProvider, recordDaoProvider);
  }

  public static DataBackupRepositoryImpl newInstance(BookDao bookDao, ReadingRecordDao recordDao) {
    return new DataBackupRepositoryImpl(bookDao, recordDao);
  }
}
