package com.readtrack;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.readtrack.data.local.PreferencesManager;
import com.readtrack.data.local.dao.BookDao;
import com.readtrack.data.local.dao.ReadingRecordDao;
import com.readtrack.data.local.database.ReadTrackDatabase;
import com.readtrack.data.remote.DoubanSearchService;
import com.readtrack.di.DatabaseModule_ProvideBookDaoFactory;
import com.readtrack.di.DatabaseModule_ProvideBookRepositoryFactory;
import com.readtrack.di.DatabaseModule_ProvideDataBackupRepositoryFactory;
import com.readtrack.di.DatabaseModule_ProvideDatabaseFactory;
import com.readtrack.di.DatabaseModule_ProvideReadingRecordDaoFactory;
import com.readtrack.di.DatabaseModule_ProvideReadingRecordRepositoryFactory;
import com.readtrack.domain.repository.BookRepository;
import com.readtrack.domain.repository.DataBackupRepository;
import com.readtrack.domain.repository.ReadingRecordRepository;
import com.readtrack.presentation.viewmodel.AddBookViewModel;
import com.readtrack.presentation.viewmodel.AddBookViewModel_HiltModules;
import com.readtrack.presentation.viewmodel.BookDetailViewModel;
import com.readtrack.presentation.viewmodel.BookDetailViewModel_HiltModules;
import com.readtrack.presentation.viewmodel.BooksViewModel;
import com.readtrack.presentation.viewmodel.BooksViewModel_HiltModules;
import com.readtrack.presentation.viewmodel.HomeViewModel;
import com.readtrack.presentation.viewmodel.HomeViewModel_HiltModules;
import com.readtrack.presentation.viewmodel.SettingsViewModel;
import com.readtrack.presentation.viewmodel.SettingsViewModel_HiltModules;
import com.readtrack.presentation.viewmodel.StatsViewModel;
import com.readtrack.presentation.viewmodel.StatsViewModel_HiltModules;
import com.readtrack.presentation.viewmodel.TimelineViewModel;
import com.readtrack.presentation.viewmodel.TimelineViewModel_HiltModules;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerReadTrackApp_HiltComponents_SingletonC {
  private DaggerReadTrackApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public ReadTrackApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements ReadTrackApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public ReadTrackApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements ReadTrackApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public ReadTrackApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements ReadTrackApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public ReadTrackApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements ReadTrackApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public ReadTrackApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements ReadTrackApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public ReadTrackApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements ReadTrackApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public ReadTrackApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements ReadTrackApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public ReadTrackApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends ReadTrackApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends ReadTrackApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends ReadTrackApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends ReadTrackApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(7).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_AddBookViewModel, AddBookViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_BookDetailViewModel, BookDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_BooksViewModel, BooksViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_HomeViewModel, HomeViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_SettingsViewModel, SettingsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_StatsViewModel, StatsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_TimelineViewModel, TimelineViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_readtrack_presentation_viewmodel_SettingsViewModel = "com.readtrack.presentation.viewmodel.SettingsViewModel";

      static String com_readtrack_presentation_viewmodel_BooksViewModel = "com.readtrack.presentation.viewmodel.BooksViewModel";

      static String com_readtrack_presentation_viewmodel_AddBookViewModel = "com.readtrack.presentation.viewmodel.AddBookViewModel";

      static String com_readtrack_presentation_viewmodel_StatsViewModel = "com.readtrack.presentation.viewmodel.StatsViewModel";

      static String com_readtrack_presentation_viewmodel_TimelineViewModel = "com.readtrack.presentation.viewmodel.TimelineViewModel";

      static String com_readtrack_presentation_viewmodel_BookDetailViewModel = "com.readtrack.presentation.viewmodel.BookDetailViewModel";

      static String com_readtrack_presentation_viewmodel_HomeViewModel = "com.readtrack.presentation.viewmodel.HomeViewModel";

      @KeepFieldType
      SettingsViewModel com_readtrack_presentation_viewmodel_SettingsViewModel2;

      @KeepFieldType
      BooksViewModel com_readtrack_presentation_viewmodel_BooksViewModel2;

      @KeepFieldType
      AddBookViewModel com_readtrack_presentation_viewmodel_AddBookViewModel2;

      @KeepFieldType
      StatsViewModel com_readtrack_presentation_viewmodel_StatsViewModel2;

      @KeepFieldType
      TimelineViewModel com_readtrack_presentation_viewmodel_TimelineViewModel2;

      @KeepFieldType
      BookDetailViewModel com_readtrack_presentation_viewmodel_BookDetailViewModel2;

      @KeepFieldType
      HomeViewModel com_readtrack_presentation_viewmodel_HomeViewModel2;
    }
  }

  private static final class ViewModelCImpl extends ReadTrackApp_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AddBookViewModel> addBookViewModelProvider;

    private Provider<BookDetailViewModel> bookDetailViewModelProvider;

    private Provider<BooksViewModel> booksViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<StatsViewModel> statsViewModelProvider;

    private Provider<TimelineViewModel> timelineViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.addBookViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.bookDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.booksViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.statsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.timelineViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(7).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_AddBookViewModel, ((Provider) addBookViewModelProvider)).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_BookDetailViewModel, ((Provider) bookDetailViewModelProvider)).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_BooksViewModel, ((Provider) booksViewModelProvider)).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_HomeViewModel, ((Provider) homeViewModelProvider)).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_SettingsViewModel, ((Provider) settingsViewModelProvider)).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_StatsViewModel, ((Provider) statsViewModelProvider)).put(LazyClassKeyProvider.com_readtrack_presentation_viewmodel_TimelineViewModel, ((Provider) timelineViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_readtrack_presentation_viewmodel_SettingsViewModel = "com.readtrack.presentation.viewmodel.SettingsViewModel";

      static String com_readtrack_presentation_viewmodel_HomeViewModel = "com.readtrack.presentation.viewmodel.HomeViewModel";

      static String com_readtrack_presentation_viewmodel_StatsViewModel = "com.readtrack.presentation.viewmodel.StatsViewModel";

      static String com_readtrack_presentation_viewmodel_TimelineViewModel = "com.readtrack.presentation.viewmodel.TimelineViewModel";

      static String com_readtrack_presentation_viewmodel_BookDetailViewModel = "com.readtrack.presentation.viewmodel.BookDetailViewModel";

      static String com_readtrack_presentation_viewmodel_BooksViewModel = "com.readtrack.presentation.viewmodel.BooksViewModel";

      static String com_readtrack_presentation_viewmodel_AddBookViewModel = "com.readtrack.presentation.viewmodel.AddBookViewModel";

      @KeepFieldType
      SettingsViewModel com_readtrack_presentation_viewmodel_SettingsViewModel2;

      @KeepFieldType
      HomeViewModel com_readtrack_presentation_viewmodel_HomeViewModel2;

      @KeepFieldType
      StatsViewModel com_readtrack_presentation_viewmodel_StatsViewModel2;

      @KeepFieldType
      TimelineViewModel com_readtrack_presentation_viewmodel_TimelineViewModel2;

      @KeepFieldType
      BookDetailViewModel com_readtrack_presentation_viewmodel_BookDetailViewModel2;

      @KeepFieldType
      BooksViewModel com_readtrack_presentation_viewmodel_BooksViewModel2;

      @KeepFieldType
      AddBookViewModel com_readtrack_presentation_viewmodel_AddBookViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.readtrack.presentation.viewmodel.AddBookViewModel 
          return (T) new AddBookViewModel(singletonCImpl.provideBookRepositoryProvider.get(), singletonCImpl.doubanSearchServiceProvider.get(), singletonCImpl.preferencesManagerProvider.get());

          case 1: // com.readtrack.presentation.viewmodel.BookDetailViewModel 
          return (T) new BookDetailViewModel(singletonCImpl.provideBookRepositoryProvider.get(), singletonCImpl.provideReadingRecordRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 2: // com.readtrack.presentation.viewmodel.BooksViewModel 
          return (T) new BooksViewModel(singletonCImpl.provideBookRepositoryProvider.get());

          case 3: // com.readtrack.presentation.viewmodel.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.provideBookRepositoryProvider.get(), singletonCImpl.provideReadingRecordRepositoryProvider.get(), singletonCImpl.preferencesManagerProvider.get());

          case 4: // com.readtrack.presentation.viewmodel.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.provideDataBackupRepositoryProvider.get(), singletonCImpl.preferencesManagerProvider.get());

          case 5: // com.readtrack.presentation.viewmodel.StatsViewModel 
          return (T) new StatsViewModel(singletonCImpl.provideBookRepositoryProvider.get(), singletonCImpl.provideReadingRecordRepositoryProvider.get(), singletonCImpl.preferencesManagerProvider.get());

          case 6: // com.readtrack.presentation.viewmodel.TimelineViewModel 
          return (T) new TimelineViewModel(singletonCImpl.provideBookRepositoryProvider.get(), singletonCImpl.provideReadingRecordRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends ReadTrackApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends ReadTrackApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends ReadTrackApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<ReadTrackDatabase> provideDatabaseProvider;

    private Provider<BookDao> provideBookDaoProvider;

    private Provider<ReadingRecordDao> provideReadingRecordDaoProvider;

    private Provider<BookRepository> provideBookRepositoryProvider;

    private Provider<DoubanSearchService> doubanSearchServiceProvider;

    private Provider<PreferencesManager> preferencesManagerProvider;

    private Provider<ReadingRecordRepository> provideReadingRecordRepositoryProvider;

    private Provider<DataBackupRepository> provideDataBackupRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<ReadTrackDatabase>(singletonCImpl, 2));
      this.provideBookDaoProvider = DoubleCheck.provider(new SwitchingProvider<BookDao>(singletonCImpl, 1));
      this.provideReadingRecordDaoProvider = DoubleCheck.provider(new SwitchingProvider<ReadingRecordDao>(singletonCImpl, 3));
      this.provideBookRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<BookRepository>(singletonCImpl, 0));
      this.doubanSearchServiceProvider = DoubleCheck.provider(new SwitchingProvider<DoubanSearchService>(singletonCImpl, 4));
      this.preferencesManagerProvider = DoubleCheck.provider(new SwitchingProvider<PreferencesManager>(singletonCImpl, 5));
      this.provideReadingRecordRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ReadingRecordRepository>(singletonCImpl, 6));
      this.provideDataBackupRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<DataBackupRepository>(singletonCImpl, 7));
    }

    @Override
    public void injectReadTrackApp(ReadTrackApp readTrackApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.readtrack.domain.repository.BookRepository 
          return (T) DatabaseModule_ProvideBookRepositoryFactory.provideBookRepository(singletonCImpl.provideBookDaoProvider.get(), singletonCImpl.provideReadingRecordDaoProvider.get(), singletonCImpl.provideDatabaseProvider.get());

          case 1: // com.readtrack.data.local.dao.BookDao 
          return (T) DatabaseModule_ProvideBookDaoFactory.provideBookDao(singletonCImpl.provideDatabaseProvider.get());

          case 2: // com.readtrack.data.local.database.ReadTrackDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.readtrack.data.local.dao.ReadingRecordDao 
          return (T) DatabaseModule_ProvideReadingRecordDaoFactory.provideReadingRecordDao(singletonCImpl.provideDatabaseProvider.get());

          case 4: // com.readtrack.data.remote.DoubanSearchService 
          return (T) new DoubanSearchService();

          case 5: // com.readtrack.data.local.PreferencesManager 
          return (T) new PreferencesManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 6: // com.readtrack.domain.repository.ReadingRecordRepository 
          return (T) DatabaseModule_ProvideReadingRecordRepositoryFactory.provideReadingRecordRepository(singletonCImpl.provideReadingRecordDaoProvider.get());

          case 7: // com.readtrack.domain.repository.DataBackupRepository 
          return (T) DatabaseModule_ProvideDataBackupRepositoryFactory.provideDataBackupRepository(singletonCImpl.provideBookDaoProvider.get(), singletonCImpl.provideReadingRecordDaoProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
