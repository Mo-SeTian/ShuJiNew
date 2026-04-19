package com.readtrack.worker;

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
public final class WebDavBackupScheduler_Factory implements Factory<WebDavBackupScheduler> {
  private final Provider<Context> contextProvider;

  public WebDavBackupScheduler_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WebDavBackupScheduler get() {
    return newInstance(contextProvider.get());
  }

  public static WebDavBackupScheduler_Factory create(Provider<Context> contextProvider) {
    return new WebDavBackupScheduler_Factory(contextProvider);
  }

  public static WebDavBackupScheduler newInstance(Context context) {
    return new WebDavBackupScheduler(context);
  }
}
