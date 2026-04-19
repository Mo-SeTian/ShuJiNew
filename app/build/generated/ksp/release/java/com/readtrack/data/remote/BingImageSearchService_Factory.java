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
public final class BingImageSearchService_Factory implements Factory<BingImageSearchService> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public BingImageSearchService_Factory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public BingImageSearchService get() {
    return newInstance(okHttpClientProvider.get());
  }

  public static BingImageSearchService_Factory create(Provider<OkHttpClient> okHttpClientProvider) {
    return new BingImageSearchService_Factory(okHttpClientProvider);
  }

  public static BingImageSearchService newInstance(OkHttpClient okHttpClient) {
    return new BingImageSearchService(okHttpClient);
  }
}
