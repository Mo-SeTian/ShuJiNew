package com.readtrack.di;

import android.content.Context;
import com.readtrack.data.local.PreferencesManager;
import com.readtrack.data.local.dao.BookDao;
import com.readtrack.data.local.dao.BookListDao;
import com.readtrack.data.local.dao.ReadingRecordDao;
import com.readtrack.domain.repository.DataBackupRepository;
import com.readtrack.util.CoverStorageUtil;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
  private final Provider<Context> contextProvider;

  private final Provider<BookDao> bookDaoProvider;

  private final Provider<ReadingRecordDao> readingRecordDaoProvider;

  private final Provider<BookListDao> bookListDaoProvider;

  private final Provider<PreferencesManager> preferencesManagerProvider;

  private final Provider<CoverStorageUtil> coverStorageUtilProvider;

  public DatabaseModule_ProvideDataBackupRepositoryFactory(Provider<Context> contextProvider,
      Provider<BookDao> bookDaoProvider, Provider<ReadingRecordDao> readingRecordDaoProvider,
      Provider<BookListDao> bookListDaoProvider,
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<CoverStorageUtil> coverStorageUtilProvider) {
    this.contextProvider = contextProvider;
    this.bookDaoProvider = bookDaoProvider;
    this.readingRecordDaoProvider = readingRecordDaoProvider;
    this.bookListDaoProvider = bookListDaoProvider;
    this.preferencesManagerProvider = preferencesManagerProvider;
    this.coverStorageUtilProvider = coverStorageUtilProvider;
  }

  @Override
  public DataBackupRepository get() {
    return provideDataBackupRepository(contextProvider.get(), bookDaoProvider.get(), readingRecordDaoProvider.get(), bookListDaoProvider.get(), preferencesManagerProvider.get(), coverStorageUtilProvider.get());
  }

  public static DatabaseModule_ProvideDataBackupRepositoryFactory create(
      Provider<Context> contextProvider, Provider<BookDao> bookDaoProvider,
      Provider<ReadingRecordDao> readingRecordDaoProvider,
      Provider<BookListDao> bookListDaoProvider,
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<CoverStorageUtil> coverStorageUtilProvider) {
    return new DatabaseModule_ProvideDataBackupRepositoryFactory(contextProvider, bookDaoProvider, readingRecordDaoProvider, bookListDaoProvider, preferencesManagerProvider, coverStorageUtilProvider);
  }

  public static DataBackupRepository provideDataBackupRepository(Context context, BookDao bookDao,
      ReadingRecordDao readingRecordDao, BookListDao bookListDao,
      PreferencesManager preferencesManager, CoverStorageUtil coverStorageUtil) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDataBackupRepository(context, bookDao, readingRecordDao, bookListDao, preferencesManager, coverStorageUtil));
  }
}
