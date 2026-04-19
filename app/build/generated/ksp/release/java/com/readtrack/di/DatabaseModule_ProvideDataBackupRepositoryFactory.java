package com.readtrack.di;

import com.readtrack.data.local.dao.BookDao;
import com.readtrack.data.local.dao.BookListDao;
import com.readtrack.data.local.dao.ReadingRecordDao;
import com.readtrack.domain.repository.DataBackupRepository;
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
public final class DatabaseModule_ProvideDataBackupRepositoryFactory implements Factory<DataBackupRepository> {
  private final Provider<BookDao> bookDaoProvider;

  private final Provider<ReadingRecordDao> readingRecordDaoProvider;

  private final Provider<BookListDao> bookListDaoProvider;

  public DatabaseModule_ProvideDataBackupRepositoryFactory(Provider<BookDao> bookDaoProvider,
      Provider<ReadingRecordDao> readingRecordDaoProvider,
      Provider<BookListDao> bookListDaoProvider) {
    this.bookDaoProvider = bookDaoProvider;
    this.readingRecordDaoProvider = readingRecordDaoProvider;
    this.bookListDaoProvider = bookListDaoProvider;
  }

  @Override
  public DataBackupRepository get() {
    return provideDataBackupRepository(bookDaoProvider.get(), readingRecordDaoProvider.get(), bookListDaoProvider.get());
  }

  public static DatabaseModule_ProvideDataBackupRepositoryFactory create(
      Provider<BookDao> bookDaoProvider, Provider<ReadingRecordDao> readingRecordDaoProvider,
      Provider<BookListDao> bookListDaoProvider) {
    return new DatabaseModule_ProvideDataBackupRepositoryFactory(bookDaoProvider, readingRecordDaoProvider, bookListDaoProvider);
  }

  public static DataBackupRepository provideDataBackupRepository(BookDao bookDao,
      ReadingRecordDao readingRecordDao, BookListDao bookListDao) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDataBackupRepository(bookDao, readingRecordDao, bookListDao));
  }
}
