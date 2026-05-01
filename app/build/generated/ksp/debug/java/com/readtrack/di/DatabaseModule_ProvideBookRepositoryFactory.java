package com.readtrack.di;

import com.readtrack.data.local.dao.BookDao;
import com.readtrack.data.local.dao.ReadingRecordDao;
import com.readtrack.data.local.database.ReadTrackDatabase;
import com.readtrack.domain.repository.BookRepository;
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
public final class DatabaseModule_ProvideBookRepositoryFactory implements Factory<BookRepository> {
  private final Provider<BookDao> bookDaoProvider;

  private final Provider<ReadingRecordDao> readingRecordDaoProvider;

  private final Provider<ReadTrackDatabase> databaseProvider;

  public DatabaseModule_ProvideBookRepositoryFactory(Provider<BookDao> bookDaoProvider,
      Provider<ReadingRecordDao> readingRecordDaoProvider,
      Provider<ReadTrackDatabase> databaseProvider) {
    this.bookDaoProvider = bookDaoProvider;
    this.readingRecordDaoProvider = readingRecordDaoProvider;
    this.databaseProvider = databaseProvider;
  }

  @Override
  public BookRepository get() {
    return provideBookRepository(bookDaoProvider.get(), readingRecordDaoProvider.get(), databaseProvider.get());
  }

  public static DatabaseModule_ProvideBookRepositoryFactory create(
      Provider<BookDao> bookDaoProvider, Provider<ReadingRecordDao> readingRecordDaoProvider,
      Provider<ReadTrackDatabase> databaseProvider) {
    return new DatabaseModule_ProvideBookRepositoryFactory(bookDaoProvider, readingRecordDaoProvider, databaseProvider);
  }

  public static BookRepository provideBookRepository(BookDao bookDao,
      ReadingRecordDao readingRecordDao, ReadTrackDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBookRepository(bookDao, readingRecordDao, database));
  }
}
