package com.readtrack.presentation.viewmodel;

import com.readtrack.data.local.PreferencesManager;
import com.readtrack.data.remote.WebDavService;
import com.readtrack.domain.repository.DataBackupRepository;
import com.readtrack.worker.WebDavBackupScheduler;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<DataBackupRepository> dataBackupRepositoryProvider;

  private final Provider<PreferencesManager> preferencesManagerProvider;

  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<WebDavService> webDavServiceProvider;

  private final Provider<WebDavBackupScheduler> webDavBackupSchedulerProvider;

  public SettingsViewModel_Factory(Provider<DataBackupRepository> dataBackupRepositoryProvider,
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<OkHttpClient> okHttpClientProvider, Provider<WebDavService> webDavServiceProvider,
      Provider<WebDavBackupScheduler> webDavBackupSchedulerProvider) {
    this.dataBackupRepositoryProvider = dataBackupRepositoryProvider;
    this.preferencesManagerProvider = preferencesManagerProvider;
    this.okHttpClientProvider = okHttpClientProvider;
    this.webDavServiceProvider = webDavServiceProvider;
    this.webDavBackupSchedulerProvider = webDavBackupSchedulerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(dataBackupRepositoryProvider.get(), preferencesManagerProvider.get(), okHttpClientProvider.get(), webDavServiceProvider.get(), webDavBackupSchedulerProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<DataBackupRepository> dataBackupRepositoryProvider,
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<OkHttpClient> okHttpClientProvider, Provider<WebDavService> webDavServiceProvider,
      Provider<WebDavBackupScheduler> webDavBackupSchedulerProvider) {
    return new SettingsViewModel_Factory(dataBackupRepositoryProvider, preferencesManagerProvider, okHttpClientProvider, webDavServiceProvider, webDavBackupSchedulerProvider);
  }

  public static SettingsViewModel newInstance(DataBackupRepository dataBackupRepository,
      PreferencesManager preferencesManager, OkHttpClient okHttpClient, WebDavService webDavService,
      WebDavBackupScheduler webDavBackupScheduler) {
    return new SettingsViewModel(dataBackupRepository, preferencesManager, okHttpClient, webDavService, webDavBackupScheduler);
  }
}
