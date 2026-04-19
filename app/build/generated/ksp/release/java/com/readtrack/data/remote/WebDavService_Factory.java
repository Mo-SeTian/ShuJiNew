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
public final class WebDavService_Factory implements Factory<WebDavService> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public WebDavService_Factory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public WebDavService get() {
    return newInstance(okHttpClientProvider.get());
  }

  public static WebDavService_Factory create(Provider<OkHttpClient> okHttpClientProvider) {
    return new WebDavService_Factory(okHttpClientProvider);
  }

  public static WebDavService newInstance(OkHttpClient okHttpClient) {
    return new WebDavService(okHttpClient);
  }
}
