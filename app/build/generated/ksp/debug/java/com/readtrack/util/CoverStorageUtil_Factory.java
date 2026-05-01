package com.readtrack.util;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class CoverStorageUtil_Factory implements Factory<CoverStorageUtil> {
  private final Provider<Context> contextProvider;

  public CoverStorageUtil_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CoverStorageUtil get() {
    return newInstance(contextProvider.get());
  }

  public static CoverStorageUtil_Factory create(Provider<Context> contextProvider) {
    return new CoverStorageUtil_Factory(contextProvider);
  }

  public static CoverStorageUtil newInstance(Context context) {
    return new CoverStorageUtil(context);
  }
}
