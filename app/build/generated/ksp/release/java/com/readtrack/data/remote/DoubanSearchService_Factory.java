package com.readtrack.data.remote;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class DoubanSearchService_Factory implements Factory<DoubanSearchService> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public DoubanSearchService_Factory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public DoubanSearchService get() {
    return newInstance(okHttpClientProvider.get());
  }

  public static DoubanSearchService_Factory create(Provider<OkHttpClient> okHttpClientProvider) {
    return new DoubanSearchService_Factory(okHttpClientProvider);
  }

  public static DoubanSearchService newInstance(OkHttpClient okHttpClient) {
    return new DoubanSearchService(okHttpClient);
  }
}
