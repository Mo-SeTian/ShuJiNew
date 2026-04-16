package com.readtrack.di;

import com.readtrack.data.local.dao.BookDao;
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

  public DatabaseModule_ProvideDataBackupRepositoryFactory(Provider<BookDao> bookDaoProvider,
      Provider<ReadingRecordDao> readingRecordDaoProvider) {
    this.bookDaoProvider = bookDaoProvider;
    this.readingRecordDaoProvider = readingRecordDaoProvider;
  }

  @Override
  public DataBackupRepository get() {
    return provideDataBackupRepository(bookDaoProvider.get(), readingRecordDaoProvider.get());
  }

  public static DatabaseModule_ProvideDataBackupRepositoryFactory create(
      Provider<BookDao> bookDaoProvider, Provider<ReadingRecordDao> readingRecordDaoProvider) {
    return new DatabaseModule_ProvideDataBackupRepositoryFactory(bookDaoProvider, readingRecordDaoProvider);
  }

  public static DataBackupRepository provideDataBackupRepository(BookDao bookDao,
      ReadingRecordDao readingRecordDao) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDataBackupRepository(bookDao, readingRecordDao));
  }
}
