package com.readtrack;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class ReadTrackApp_MembersInjector implements MembersInjector<ReadTrackApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public ReadTrackApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<ReadTrackApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new ReadTrackApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(ReadTrackApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.readtrack.ReadTrackApp.workerFactory")
  public static void injectWorkerFactory(ReadTrackApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
