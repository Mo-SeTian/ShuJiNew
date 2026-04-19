package com.readtrack.di;

import com.readtrack.data.local.dao.BookListDao;
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
public final class DatabaseModule_ProvideBookListDaoFactory implements Factory<BookListDao> {
  private final Provider<ReadTrackDatabase> databaseProvider;

  public DatabaseModule_ProvideBookListDaoFactory(Provider<ReadTrackDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public BookListDao get() {
    return provideBookListDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideBookListDaoFactory create(
      Provider<ReadTrackDatabase> databaseProvider) {
    return new DatabaseModule_ProvideBookListDaoFactory(databaseProvider);
  }

  public static BookListDao provideBookListDao(ReadTrackDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBookListDao(database));
  }
}
