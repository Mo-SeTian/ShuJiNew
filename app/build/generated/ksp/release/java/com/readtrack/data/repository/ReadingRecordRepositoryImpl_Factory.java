package com.readtrack.data.repository;

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
public final class ReadingRecordRepositoryImpl_Factory implements Factory<ReadingRecordRepositoryImpl> {
  private final Provider<ReadingRecordDao> readingRecordDaoProvider;

  public ReadingRecordRepositoryImpl_Factory(Provider<ReadingRecordDao> readingRecordDaoProvider) {
    this.readingRecordDaoProvider = readingRecordDaoProvider;
  }

  @Override
  public ReadingRecordRepositoryImpl get() {
    return newInstance(readingRecordDaoProvider.get());
  }

  public static ReadingRecordRepositoryImpl_Factory create(
      Provider<ReadingRecordDao> readingRecordDaoProvider) {
    return new ReadingRecordRepositoryImpl_Factory(readingRecordDaoProvider);
  }

  public static ReadingRecordRepositoryImpl newInstance(ReadingRecordDao readingRecordDao) {
    return new ReadingRecordRepositoryImpl(readingRecordDao);
  }
}
