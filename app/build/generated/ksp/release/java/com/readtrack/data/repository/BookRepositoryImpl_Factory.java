package com.readtrack.data.repository;

import com.readtrack.data.local.dao.BookDao;
import com.readtrack.data.local.dao.ReadingRecordDao;
import com.readtrack.data.local.database.ReadTrackDatabase;
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
public final class BookRepositoryImpl_Factory implements Factory<BookRepositoryImpl> {
  private final Provider<BookDao> bookDaoProvider;

  private final Provider<ReadingRecordDao> readingRecordDaoProvider;

  private final Provider<ReadTrackDatabase> databaseProvider;

  public BookRepositoryImpl_Factory(Provider<BookDao> bookDaoProvider,
      Provider<ReadingRecordDao> readingRecordDaoProvider,
      Provider<ReadTrackDatabase> databaseProvider) {
    this.bookDaoProvider = bookDaoProvider;
    this.readingRecordDaoProvider = readingRecordDaoProvider;
    this.databaseProvider = databaseProvider;
  }

  @Override
  public BookRepositoryImpl get() {
    return newInstance(bookDaoProvider.get(), readingRecordDaoProvider.get(), databaseProvider.get());
  }

  public static BookRepositoryImpl_Factory create(Provider<BookDao> bookDaoProvider,
      Provider<ReadingRecordDao> readingRecordDaoProvider,
      Provider<ReadTrackDatabase> databaseProvider) {
    return new BookRepositoryImpl_Factory(bookDaoProvider, readingRecordDaoProvider, databaseProvider);
  }

  public static BookRepositoryImpl newInstance(BookDao bookDao, ReadingRecordDao readingRecordDao,
      ReadTrackDatabase database) {
    return new BookRepositoryImpl(bookDao, readingRecordDao, database);
  }
}
