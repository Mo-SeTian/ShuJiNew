package com.readtrack.presentation.viewmodel;

import com.readtrack.data.local.PreferencesManager;
import com.readtrack.data.remote.BingImageSearchService;
import com.readtrack.data.remote.DoubanSearchService;
import com.readtrack.domain.repository.BookRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AddBookViewModel_Factory implements Factory<AddBookViewModel> {
  private final Provider<BookRepository> bookRepositoryProvider;

  private final Provider<DoubanSearchService> doubanSearchServiceProvider;

  private final Provider<BingImageSearchService> bingImageSearchServiceProvider;

  private final Provider<PreferencesManager> preferencesManagerProvider;

  public AddBookViewModel_Factory(Provider<BookRepository> bookRepositoryProvider,
      Provider<DoubanSearchService> doubanSearchServiceProvider,
      Provider<BingImageSearchService> bingImageSearchServiceProvider,
      Provider<PreferencesManager> preferencesManagerProvider) {
    this.bookRepositoryProvider = bookRepositoryProvider;
    this.doubanSearchServiceProvider = doubanSearchServiceProvider;
    this.bingImageSearchServiceProvider = bingImageSearchServiceProvider;
    this.preferencesManagerProvider = preferencesManagerProvider;
  }

  @Override
  public AddBookViewModel get() {
    return newInstance(bookRepositoryProvider.get(), doubanSearchServiceProvider.get(), bingImageSearchServiceProvider.get(), preferencesManagerProvider.get());
  }

  public static AddBookViewModel_Factory create(Provider<BookRepository> bookRepositoryProvider,
      Provider<DoubanSearchService> doubanSearchServiceProvider,
      Provider<BingImageSearchService> bingImageSearchServiceProvider,
      Provider<PreferencesManager> preferencesManagerProvider) {
    return new AddBookViewModel_Factory(bookRepositoryProvider, doubanSearchServiceProvider, bingImageSearchServiceProvider, preferencesManagerProvider);
  }

  public static AddBookViewModel newInstance(BookRepository bookRepository,
      DoubanSearchService doubanSearchService, BingImageSearchService bingImageSearchService,
      PreferencesManager preferencesManager) {
    return new AddBookViewModel(bookRepository, doubanSearchService, bingImageSearchService, preferencesManager);
  }
}
