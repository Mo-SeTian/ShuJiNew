package com.readtrack.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.readtrack.data.local.entity.ReadingRecordEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ReadingRecordDao_Impl implements ReadingRecordDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ReadingRecordEntity> __insertionAdapterOfReadingRecordEntity;

  private final EntityDeletionOrUpdateAdapter<ReadingRecordEntity> __deletionAdapterOfReadingRecordEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteRecordsByBookId;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllRecords;

  public ReadingRecordDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfReadingRecordEntity = new EntityInsertionAdapter<ReadingRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `reading_records` (`id`,`bookId`,`pagesRead`,`fromPage`,`toPage`,`date`,`note`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ReadingRecordEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getBookId());
        statement.bindDouble(3, entity.getPagesRead());
        statement.bindDouble(4, entity.getFromPage());
        statement.bindDouble(5, entity.getToPage());
        statement.bindLong(6, entity.getDate());
        if (entity.getNote() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getNote());
        }
      }
    };
    this.__deletionAdapterOfReadingRecordEntity = new EntityDeletionOrUpdateAdapter<ReadingRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `reading_records` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ReadingRecordEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteRecordsByBookId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM reading_records WHERE bookId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllRecords = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM reading_records";
        return _query;
      }
    };
  }

  @Override
  public Object insertRecord(final ReadingRecordEntity record,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfReadingRecordEntity.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRecord(final ReadingRecordEntity record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfReadingRecordEntity.handle(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRecordsByBookId(final long bookId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteRecordsByBookId.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, bookId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteRecordsByBookId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllRecords(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllRecords.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllRecords.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ReadingRecordEntity>> getRecordsByBookId(final long bookId) {
    final String _sql = "SELECT * FROM reading_records WHERE bookId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, bookId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"reading_records"}, new Callable<List<ReadingRecordEntity>>() {
      @Override
      @NonNull
      public List<ReadingRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBookId = CursorUtil.getColumnIndexOrThrow(_cursor, "bookId");
          final int _cursorIndexOfPagesRead = CursorUtil.getColumnIndexOrThrow(_cursor, "pagesRead");
          final int _cursorIndexOfFromPage = CursorUtil.getColumnIndexOrThrow(_cursor, "fromPage");
          final int _cursorIndexOfToPage = CursorUtil.getColumnIndexOrThrow(_cursor, "toPage");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<ReadingRecordEntity> _result = new ArrayList<ReadingRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReadingRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpBookId;
            _tmpBookId = _cursor.getLong(_cursorIndexOfBookId);
            final double _tmpPagesRead;
            _tmpPagesRead = _cursor.getDouble(_cursorIndexOfPagesRead);
            final double _tmpFromPage;
            _tmpFromPage = _cursor.getDouble(_cursorIndexOfFromPage);
            final double _tmpToPage;
            _tmpToPage = _cursor.getDouble(_cursorIndexOfToPage);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item = new ReadingRecordEntity(_tmpId,_tmpBookId,_tmpPagesRead,_tmpFromPage,_tmpToPage,_tmpDate,_tmpNote);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ReadingRecordEntity>> getAllRecords() {
    final String _sql = "SELECT * FROM reading_records ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"reading_records"}, new Callable<List<ReadingRecordEntity>>() {
      @Override
      @NonNull
      public List<ReadingRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBookId = CursorUtil.getColumnIndexOrThrow(_cursor, "bookId");
          final int _cursorIndexOfPagesRead = CursorUtil.getColumnIndexOrThrow(_cursor, "pagesRead");
          final int _cursorIndexOfFromPage = CursorUtil.getColumnIndexOrThrow(_cursor, "fromPage");
          final int _cursorIndexOfToPage = CursorUtil.getColumnIndexOrThrow(_cursor, "toPage");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<ReadingRecordEntity> _result = new ArrayList<ReadingRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReadingRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpBookId;
            _tmpBookId = _cursor.getLong(_cursorIndexOfBookId);
            final double _tmpPagesRead;
            _tmpPagesRead = _cursor.getDouble(_cursorIndexOfPagesRead);
            final double _tmpFromPage;
            _tmpFromPage = _cursor.getDouble(_cursorIndexOfFromPage);
            final double _tmpToPage;
            _tmpToPage = _cursor.getDouble(_cursorIndexOfToPage);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item = new ReadingRecordEntity(_tmpId,_tmpBookId,_tmpPagesRead,_tmpFromPage,_tmpToPage,_tmpDate,_tmpNote);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ReadingRecordEntity>> getRecordsByDate(final long startOfDay,
      final long endOfDay) {
    final String _sql = "SELECT * FROM reading_records WHERE date >= ? AND date < ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startOfDay);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endOfDay);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"reading_records"}, new Callable<List<ReadingRecordEntity>>() {
      @Override
      @NonNull
      public List<ReadingRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBookId = CursorUtil.getColumnIndexOrThrow(_cursor, "bookId");
          final int _cursorIndexOfPagesRead = CursorUtil.getColumnIndexOrThrow(_cursor, "pagesRead");
          final int _cursorIndexOfFromPage = CursorUtil.getColumnIndexOrThrow(_cursor, "fromPage");
          final int _cursorIndexOfToPage = CursorUtil.getColumnIndexOrThrow(_cursor, "toPage");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<ReadingRecordEntity> _result = new ArrayList<ReadingRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReadingRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpBookId;
            _tmpBookId = _cursor.getLong(_cursorIndexOfBookId);
            final double _tmpPagesRead;
            _tmpPagesRead = _cursor.getDouble(_cursorIndexOfPagesRead);
            final double _tmpFromPage;
            _tmpFromPage = _cursor.getDouble(_cursorIndexOfFromPage);
            final double _tmpToPage;
            _tmpToPage = _cursor.getDouble(_cursorIndexOfToPage);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item = new ReadingRecordEntity(_tmpId,_tmpBookId,_tmpPagesRead,_tmpFromPage,_tmpToPage,_tmpDate,_tmpNote);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Double> getTotalPagesReadOnDate(final long startOfDay, final long endOfDay) {
    final String _sql = "SELECT SUM(pagesRead) FROM reading_records WHERE date >= ? AND date < ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startOfDay);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endOfDay);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"reading_records"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Double> getTotalPagesReadSince(final long startTime) {
    final String _sql = "SELECT SUM(pagesRead) FROM reading_records WHERE date >= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"reading_records"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getRecordsByBookIdOnce(final long bookId,
      final Continuation<? super List<ReadingRecordEntity>> $completion) {
    final String _sql = "SELECT * FROM reading_records WHERE bookId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, bookId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ReadingRecordEntity>>() {
      @Override
      @NonNull
      public List<ReadingRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfBookId = CursorUtil.getColumnIndexOrThrow(_cursor, "bookId");
          final int _cursorIndexOfPagesRead = CursorUtil.getColumnIndexOrThrow(_cursor, "pagesRead");
          final int _cursorIndexOfFromPage = CursorUtil.getColumnIndexOrThrow(_cursor, "fromPage");
          final int _cursorIndexOfToPage = CursorUtil.getColumnIndexOrThrow(_cursor, "toPage");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<ReadingRecordEntity> _result = new ArrayList<ReadingRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReadingRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpBookId;
            _tmpBookId = _cursor.getLong(_cursorIndexOfBookId);
            final double _tmpPagesRead;
            _tmpPagesRead = _cursor.getDouble(_cursorIndexOfPagesRead);
            final double _tmpFromPage;
            _tmpFromPage = _cursor.getDouble(_cursorIndexOfFromPage);
            final double _tmpToPage;
            _tmpToPage = _cursor.getDouble(_cursorIndexOfToPage);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item = new ReadingRecordEntity(_tmpId,_tmpBookId,_tmpPagesRead,_tmpFromPage,_tmpToPage,_tmpDate,_tmpNote);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
