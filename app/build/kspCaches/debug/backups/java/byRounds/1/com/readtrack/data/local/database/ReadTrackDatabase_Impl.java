package com.readtrack.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.readtrack.data.local.dao.BookDao;
import com.readtrack.data.local.dao.BookDao_Impl;
import com.readtrack.data.local.dao.ReadingRecordDao;
import com.readtrack.data.local.dao.ReadingRecordDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ReadTrackDatabase_Impl extends ReadTrackDatabase {
  private volatile BookDao _bookDao;

  private volatile ReadingRecordDao _readingRecordDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(6) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `books` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `author` TEXT, `publisher` TEXT, `description` TEXT, `progressType` TEXT NOT NULL, `totalPages` REAL NOT NULL, `currentPage` REAL NOT NULL, `totalChapters` INTEGER, `currentChapter` INTEGER NOT NULL, `coverPath` TEXT, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `lastReadAt` INTEGER)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_status_updatedAt` ON `books` (`status`, `updatedAt`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_updatedAt` ON `books` (`updatedAt`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_lastReadAt` ON `books` (`lastReadAt`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_title` ON `books` (`title`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_author` ON `books` (`author`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `reading_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `bookId` INTEGER NOT NULL, `pagesRead` REAL NOT NULL, `fromPage` REAL NOT NULL, `toPage` REAL NOT NULL, `date` INTEGER NOT NULL, `note` TEXT, `recordType` TEXT NOT NULL, FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_records_bookId` ON `reading_records` (`bookId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_records_date` ON `reading_records` (`date`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_records_bookId_date` ON `reading_records` (`bookId`, `date`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7f2c850f79efaa5f6ab7654cc49dc187')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `books`");
        db.execSQL("DROP TABLE IF EXISTS `reading_records`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsBooks = new HashMap<String, TableInfo.Column>(15);
        _columnsBooks.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("author", new TableInfo.Column("author", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("publisher", new TableInfo.Column("publisher", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("progressType", new TableInfo.Column("progressType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("totalPages", new TableInfo.Column("totalPages", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("currentPage", new TableInfo.Column("currentPage", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("totalChapters", new TableInfo.Column("totalChapters", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("currentChapter", new TableInfo.Column("currentChapter", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("coverPath", new TableInfo.Column("coverPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBooks.put("lastReadAt", new TableInfo.Column("lastReadAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBooks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBooks = new HashSet<TableInfo.Index>(5);
        _indicesBooks.add(new TableInfo.Index("index_books_status_updatedAt", false, Arrays.asList("status", "updatedAt"), Arrays.asList("ASC", "ASC")));
        _indicesBooks.add(new TableInfo.Index("index_books_updatedAt", false, Arrays.asList("updatedAt"), Arrays.asList("ASC")));
        _indicesBooks.add(new TableInfo.Index("index_books_lastReadAt", false, Arrays.asList("lastReadAt"), Arrays.asList("ASC")));
        _indicesBooks.add(new TableInfo.Index("index_books_title", false, Arrays.asList("title"), Arrays.asList("ASC")));
        _indicesBooks.add(new TableInfo.Index("index_books_author", false, Arrays.asList("author"), Arrays.asList("ASC")));
        final TableInfo _infoBooks = new TableInfo("books", _columnsBooks, _foreignKeysBooks, _indicesBooks);
        final TableInfo _existingBooks = TableInfo.read(db, "books");
        if (!_infoBooks.equals(_existingBooks)) {
          return new RoomOpenHelper.ValidationResult(false, "books(com.readtrack.data.local.entity.BookEntity).\n"
                  + " Expected:\n" + _infoBooks + "\n"
                  + " Found:\n" + _existingBooks);
        }
        final HashMap<String, TableInfo.Column> _columnsReadingRecords = new HashMap<String, TableInfo.Column>(8);
        _columnsReadingRecords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadingRecords.put("bookId", new TableInfo.Column("bookId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadingRecords.put("pagesRead", new TableInfo.Column("pagesRead", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadingRecords.put("fromPage", new TableInfo.Column("fromPage", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadingRecords.put("toPage", new TableInfo.Column("toPage", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadingRecords.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadingRecords.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadingRecords.put("recordType", new TableInfo.Column("recordType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysReadingRecords = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysReadingRecords.add(new TableInfo.ForeignKey("books", "CASCADE", "NO ACTION", Arrays.asList("bookId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesReadingRecords = new HashSet<TableInfo.Index>(3);
        _indicesReadingRecords.add(new TableInfo.Index("index_reading_records_bookId", false, Arrays.asList("bookId"), Arrays.asList("ASC")));
        _indicesReadingRecords.add(new TableInfo.Index("index_reading_records_date", false, Arrays.asList("date"), Arrays.asList("ASC")));
        _indicesReadingRecords.add(new TableInfo.Index("index_reading_records_bookId_date", false, Arrays.asList("bookId", "date"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoReadingRecords = new TableInfo("reading_records", _columnsReadingRecords, _foreignKeysReadingRecords, _indicesReadingRecords);
        final TableInfo _existingReadingRecords = TableInfo.read(db, "reading_records");
        if (!_infoReadingRecords.equals(_existingReadingRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "reading_records(com.readtrack.data.local.entity.ReadingRecordEntity).\n"
                  + " Expected:\n" + _infoReadingRecords + "\n"
                  + " Found:\n" + _existingReadingRecords);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "7f2c850f79efaa5f6ab7654cc49dc187", "66e53973cf7a5cfa0d14cf1e7258b649");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "books","reading_records");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `books`");
      _db.execSQL("DELETE FROM `reading_records`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(BookDao.class, BookDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ReadingRecordDao.class, ReadingRecordDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public BookDao bookDao() {
    if (_bookDao != null) {
      return _bookDao;
    } else {
      synchronized(this) {
        if(_bookDao == null) {
          _bookDao = new BookDao_Impl(this);
        }
        return _bookDao;
      }
    }
  }

  @Override
  public ReadingRecordDao readingRecordDao() {
    if (_readingRecordDao != null) {
      return _readingRecordDao;
    } else {
      synchronized(this) {
        if(_readingRecordDao == null) {
          _readingRecordDao = new ReadingRecordDao_Impl(this);
        }
        return _readingRecordDao;
      }
    }
  }
}
