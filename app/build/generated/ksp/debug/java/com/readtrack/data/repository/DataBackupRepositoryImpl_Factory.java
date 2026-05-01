package com.readtrack.data.repository;

import android.content.Context;
import com.readtrack.data.local.PreferencesManager;
import com.readtrack.data.local.dao.BookDao;
import com.readtrack.data.local.dao.BookListDao;
import com.readtrack.data.local.dao.ReadingRecordDao;
import com.readtrack.util.CoverStorageUtil;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class DataBackupRepositoryImpl_Factory implements Factory<DataBackupRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<BookDao> bookDaoProvider;

  private final Provider<ReadingRecordDao> recordDaoProvider;

  private final Provider<BookListDao> bookListDaoProvider;

  private final Provider<PreferencesManager> preferencesManagerProvider;

  private final Provider<CoverStorageUtil> coverStorageUtilProvider;

  public DataBackupRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<BookDao> bookDaoProvider, Provider<ReadingRecordDao> recordDaoProvider,
      Provider<BookListDao> bookListDaoProvider,
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<CoverStorageUtil> coverStorageUtilProvider) {
    this.contextProvider = contextProvider;
    this.bookDaoProvider = bookDaoProvider;
    this.recordDaoProvider = recordDaoProvider;
    this.bookListDaoProvider = bookListDaoProvider;
    this.preferencesManagerProvider = preferencesManagerProvider;
    this.coverStorageUtilProvider = coverStorageUtilProvider;
  }

  @Override
  public DataBackupRepositoryImpl get() {
    return newInstance(contextProvider.get(), bookDaoProvider.get(), recordDaoProvider.get(), bookListDaoProvider.get(), preferencesManagerProvider.get(), coverStorageUtilProvider.get());
  }

  public static DataBackupRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<BookDao> bookDaoProvider, Provider<ReadingRecordDao> recordDaoProvider,
      Provider<BookListDao> bookListDaoProvider,
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<CoverStorageUtil> coverStorageUtilProvider) {
    return new DataBackupRepositoryImpl_Factory(contextProvider, bookDaoProvider, recordDaoProvider, bookListDaoProvider, preferencesManagerProvider, coverStorageUtilProvider);
  }

  public static DataBackupRepositoryImpl newInstance(Context context, BookDao bookDao,
      ReadingRecordDao recordDao, BookListDao bookListDao, PreferencesManager preferencesManager,
      CoverStorageUtil coverStorageUtil) {
    return new DataBackupRepositoryImpl(context, bookDao, recordDao, bookListDao, preferencesManager, coverStorageUtil);
  }
}
