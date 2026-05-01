package com.readtrack.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.readtrack.data.local.PreferencesManager;
import com.readtrack.data.remote.WebDavService;
import com.readtrack.domain.repository.DataBackupRepository;
import dagger.internal.DaggerGenerated;
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
public final class WebDavAutoBackupWorker_Factory {
  private final Provider<DataBackupRepository> dataBackupRepositoryProvider;

  private final Provider<PreferencesManager> preferencesManagerProvider;

  private final Provider<WebDavService> webDavServiceProvider;

  public WebDavAutoBackupWorker_Factory(Provider<DataBackupRepository> dataBackupRepositoryProvider,
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<WebDavService> webDavServiceProvider) {
    this.dataBackupRepositoryProvider = dataBackupRepositoryProvider;
    this.preferencesManagerProvider = preferencesManagerProvider;
    this.webDavServiceProvider = webDavServiceProvider;
  }

  public WebDavAutoBackupWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, dataBackupRepositoryProvider.get(), preferencesManagerProvider.get(), webDavServiceProvider.get());
  }

  public static WebDavAutoBackupWorker_Factory create(
      Provider<DataBackupRepository> dataBackupRepositoryProvider,
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<WebDavService> webDavServiceProvider) {
    return new WebDavAutoBackupWorker_Factory(dataBackupRepositoryProvider, preferencesManagerProvider, webDavServiceProvider);
  }

  public static WebDavAutoBackupWorker newInstance(Context appContext,
      WorkerParameters workerParams, DataBackupRepository dataBackupRepository,
      PreferencesManager preferencesManager, WebDavService webDavService) {
    return new WebDavAutoBackupWorker(appContext, workerParams, dataBackupRepository, preferencesManager, webDavService);
  }
}
