package com.readtrack.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class WebDavAutoBackupWorker_AssistedFactory_Impl implements WebDavAutoBackupWorker_AssistedFactory {
  private final WebDavAutoBackupWorker_Factory delegateFactory;

  WebDavAutoBackupWorker_AssistedFactory_Impl(WebDavAutoBackupWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public WebDavAutoBackupWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<WebDavAutoBackupWorker_AssistedFactory> create(
      WebDavAutoBackupWorker_Factory delegateFactory) {
    return InstanceFactory.create(new WebDavAutoBackupWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<WebDavAutoBackupWorker_AssistedFactory> createFactoryProvider(
      WebDavAutoBackupWorker_Factory delegateFactory) {
    return InstanceFactory.create(new WebDavAutoBackupWorker_AssistedFactory_Impl(delegateFactory));
  }
}
