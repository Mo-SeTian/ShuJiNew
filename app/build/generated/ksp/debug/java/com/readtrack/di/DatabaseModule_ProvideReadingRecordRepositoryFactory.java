package com.readtrack.di;

import com.readtrack.data.local.dao.ReadingRecordDao;
import com.readtrack.domain.repository.ReadingRecordRepository;
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
public final class DatabaseModule_ProvideReadingRecordRepositoryFactory implements Factory<ReadingRecordRepository> {
  private final Provider<ReadingRecordDao> readingRecordDaoProvider;

  public DatabaseModule_ProvideReadingRecordRepositoryFactory(
      Provider<ReadingRecordDao> readingRecordDaoProvider) {
    this.readingRecordDaoProvider = readingRecordDaoProvider;
  }

  @Override
  public ReadingRecordRepository get() {
    return provideReadingRecordRepository(readingRecordDaoProvider.get());
  }

  public static DatabaseModule_ProvideReadingRecordRepositoryFactory create(
      Provider<ReadingRecordDao> readingRecordDaoProvider) {
    return new DatabaseModule_ProvideReadingRecordRepositoryFactory(readingRecordDaoProvider);
  }

  public static ReadingRecordRepository provideReadingRecordRepository(
      ReadingRecordDao readingRecordDao) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideReadingRecordRepository(readingRecordDao));
  }
}
