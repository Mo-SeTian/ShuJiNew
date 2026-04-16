package com.readtrack.presentation.viewmodel;

import com.readtrack.data.local.PreferencesManager;
import com.readtrack.domain.repository.DataBackupRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<DataBackupRepository> dataBackupRepositoryProvider;

  private final Provider<PreferencesManager> preferencesManagerProvider;

  public SettingsViewModel_Factory(Provider<DataBackupRepository> dataBackupRepositoryProvider,
      Provider<PreferencesManager> preferencesManagerProvider) {
    this.dataBackupRepositoryProvider = dataBackupRepositoryProvider;
    this.preferencesManagerProvider = preferencesManagerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(dataBackupRepositoryProvider.get(), preferencesManagerProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<DataBackupRepository> dataBackupRepositoryProvider,
      Provider<PreferencesManager> preferencesManagerProvider) {
    return new SettingsViewModel_Factory(dataBackupRepositoryProvider, preferencesManagerProvider);
  }

  public static SettingsViewModel newInstance(DataBackupRepository dataBackupRepository,
      PreferencesManager preferencesManager) {
    return new SettingsViewModel(dataBackupRepository, preferencesManager);
  }
}
