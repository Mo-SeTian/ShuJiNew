package com.readtrack.data.remote;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
  @Override
  public DoubanSearchService get() {
    return newInstance();
  }

  public static DoubanSearchService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static DoubanSearchService newInstance() {
    return new DoubanSearchService();
  }

  private static final class InstanceHolder {
    private static final DoubanSearchService_Factory INSTANCE = new DoubanSearchService_Factory();
  }
}
