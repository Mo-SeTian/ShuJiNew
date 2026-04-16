package com.readtrack.di;

import com.readtrack.data.local.dao.ReadingRecordDao;
import com.readtrack.data.local.database.ReadTrackDatabase;
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
public final class DatabaseModule_ProvideReadingRecordDaoFactory implements Factory<ReadingRecordDao> {
  private final Provider<ReadTrackDatabase> databaseProvider;

  public DatabaseModule_ProvideReadingRecordDaoFactory(
      Provider<ReadTrackDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ReadingRecordDao get() {
    return provideReadingRecordDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideReadingRecordDaoFactory create(
      Provider<ReadTrackDatabase> databaseProvider) {
    return new DatabaseModule_ProvideReadingRecordDaoFactory(databaseProvider);
  }

  public static ReadingRecordDao provideReadingRecordDao(ReadTrackDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideReadingRecordDao(database));
  }
}
