package com.readtrack.presentation.viewmodel;

import android.content.Context;
import com.readtrack.data.local.PreferencesManager;
import com.readtrack.data.remote.WebDavService;
import com.readtrack.domain.repository.DataBackupRepository;
import com.readtrack.util.CoverStorageUtil;
import com.readtrack.worker.WebDavBackupScheduler;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<Context> applicationContextProvider;

  private final Provider<DataBackupRepository> dataBackupRepositoryProvider;

  private final Provider<PreferencesManager> preferencesManagerProvider;

  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<WebDavService> webDavServiceProvider;

  private final Provider<WebDavBackupScheduler> webDavBackupSchedulerProvider;

  private final Provider<CoverStorageUtil> coverStorageUtilProvider;

  public SettingsViewModel_Factory(Provider<Context> applicationContextProvider,
      Provider<DataBackupRepository> dataBackupRepositoryProvider,
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<OkHttpClient> okHttpClientProvider, Provider<WebDavService> webDavServiceProvider,
      Provider<WebDavBackupScheduler> webDavBackupSchedulerProvider,
      Provider<CoverStorageUtil> coverStorageUtilProvider) {
    this.applicationContextProvider = applicationContextProvider;
    this.dataBackupRepositoryProvider = dataBackupRepositoryProvider;
    this.preferencesManagerProvider = preferencesManagerProvider;
    this.okHttpClientProvider = okHttpClientProvider;
    this.webDavServiceProvider = webDavServiceProvider;
    this.webDavBackupSchedulerProvider = webDavBackupSchedulerProvider;
    this.coverStorageUtilProvider = coverStorageUtilProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(applicationContextProvider.get(), dataBackupRepositoryProvider.get(), preferencesManagerProvider.get(), okHttpClientProvider.get(), webDavServiceProvider.get(), webDavBackupSchedulerProvider.get(), coverStorageUtilProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<Context> applicationContextProvider,
      Provider<DataBackupRepository> dataBackupRepositoryProvider,
      Provider<PreferencesManager> preferencesManagerProvider,
      Provider<OkHttpClient> okHttpClientProvider, Provider<WebDavService> webDavServiceProvider,
      Provider<WebDavBackupScheduler> webDavBackupSchedulerProvider,
      Provider<CoverStorageUtil> coverStorageUtilProvider) {
    return new SettingsViewModel_Factory(applicationContextProvider, dataBackupRepositoryProvider, preferencesManagerProvider, okHttpClientProvider, webDavServiceProvider, webDavBackupSchedulerProvider, coverStorageUtilProvider);
  }

  public static SettingsViewModel newInstance(Context applicationContext,
      DataBackupRepository dataBackupRepository, PreferencesManager preferencesManager,
      OkHttpClient okHttpClient, WebDavService webDavService,
      WebDavBackupScheduler webDavBackupScheduler, CoverStorageUtil coverStorageUtil) {
    return new SettingsViewModel(applicationContext, dataBackupRepository, preferencesManager, okHttpClient, webDavService, webDavBackupScheduler, coverStorageUtil);
  }
}
