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
import com.readtrack.data.local.database.Converters;
import com.readtrack.data.local.entity.BookEntity;
import com.readtrack.data.local.entity.BookListCrossRef;
import com.readtrack.data.local.entity.BookListEntity;
import com.readtrack.domain.model.BookStatus;
import com.readtrack.domain.model.ProgressType;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Integer;
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
public final class BookListDao_Impl implements BookListDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BookListEntity> __insertionAdapterOfBookListEntity;

  private final EntityInsertionAdapter<BookListCrossRef> __insertionAdapterOfBookListCrossRef;

  private final EntityDeletionOrUpdateAdapter<BookListEntity> __deletionAdapterOfBookListEntity;

  private final EntityDeletionOrUpdateAdapter<BookListEntity> __updateAdapterOfBookListEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBookListById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllBookLists;

  private final SharedSQLiteStatement __preparedStmtOfRemoveBookFromList;

  private final SharedSQLiteStatement __preparedStmtOfUpdateBookCount;

  private final SharedSQLiteStatement __preparedStmtOfClearBookList;

  private final Converters __converters = new Converters();

  public BookListDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBookListEntity = new EntityInsertionAdapter<BookListEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `book_lists` (`id`,`name`,`description`,`coverPath`,`coverBookId`,`bookCount`,`createdAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BookListEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        if (entity.getCoverPath() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getCoverPath());
        }
        if (entity.getCoverBookId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getCoverBookId());
        }
        statement.bindLong(6, entity.getBookCount());
        statement.bindLong(7, entity.getCreatedAt());
        statement.bindLong(8, entity.getUpdatedAt());
      }
    };
    this.__insertionAdapterOfBookListCrossRef = new EntityInsertionAdapter<BookListCrossRef>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `book_list_cross_ref` (`bookListId`,`bookId`,`addedAt`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BookListCrossRef entity) {
        statement.bindLong(1, entity.getBookListId());
        statement.bindLong(2, entity.getBookId());
        statement.bindLong(3, entity.getAddedAt());
      }
    };
    this.__deletionAdapterOfBookListEntity = new EntityDeletionOrUpdateAdapter<BookListEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `book_lists` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BookListEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfBookListEntity = new EntityDeletionOrUpdateAdapter<BookListEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `book_lists` SET `id` = ?,`name` = ?,`description` = ?,`coverPath` = ?,`coverBookId` = ?,`bookCount` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BookListEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDescription());
        }
        if (entity.getCoverPath() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getCoverPath());
        }
        if (entity.getCoverBookId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getCoverBookId());
        }
        statement.bindLong(6, entity.getBookCount());
        statement.bindLong(7, entity.getCreatedAt());
        statement.bindLong(8, entity.getUpdatedAt());
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteBookListById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM book_lists WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllBookLists = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM book_lists";
        return _query;
      }
    };
    this.__preparedStmtOfRemoveBookFromList = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM book_list_cross_ref WHERE bookListId = ? AND bookId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateBookCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE book_lists\n"
                + "        SET bookCount = (\n"
                + "            SELECT COUNT(*) FROM book_list_cross_ref WHERE bookListId = ?\n"
                + "        ), updatedAt = ?\n"
                + "        WHERE id = ?\n"
                + "    ";
        return _query;
      }
    };
    this.__preparedStmtOfClearBookList = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM book_list_cross_ref WHERE bookListId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertBookList(final BookListEntity bookList,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfBookListEntity.insertAndReturnId(bookList);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object addBookToList(final BookListCrossRef crossRef,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBookListCrossRef.insert(crossRef);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object addBooksToList(final List<BookListCrossRef> crossRefs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBookListCrossRef.insert(crossRefs);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBookList(final BookListEntity bookList,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfBookListEntity.handle(bookList);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateBookList(final BookListEntity bookList,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfBookListEntity.handle(bookList);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBookListById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBookListById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfDeleteBookListById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllBookLists(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllBookLists.acquire();
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
          __preparedStmtOfDeleteAllBookLists.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object removeBookFromList(final long bookListId, final long bookId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRemoveBookFromList.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, bookListId);
        _argIndex = 2;
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
          __preparedStmtOfRemoveBookFromList.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateBookCount(final long bookListId, final long updatedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateBookCount.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, bookListId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, updatedAt);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, bookListId);
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
          __preparedStmtOfUpdateBookCount.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearBookList(final long bookListId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearBookList.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, bookListId);
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
          __preparedStmtOfClearBookList.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<BookListEntity>> getAllBookLists() {
    final String _sql = "SELECT * FROM book_lists ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"book_lists"}, new Callable<List<BookListEntity>>() {
      @Override
      @NonNull
      public List<BookListEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCoverPath = CursorUtil.getColumnIndexOrThrow(_cursor, "coverPath");
          final int _cursorIndexOfCoverBookId = CursorUtil.getColumnIndexOrThrow(_cursor, "coverBookId");
          final int _cursorIndexOfBookCount = CursorUtil.getColumnIndexOrThrow(_cursor, "bookCount");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<BookListEntity> _result = new ArrayList<BookListEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BookListEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCoverPath;
            if (_cursor.isNull(_cursorIndexOfCoverPath)) {
              _tmpCoverPath = null;
            } else {
              _tmpCoverPath = _cursor.getString(_cursorIndexOfCoverPath);
            }
            final Long _tmpCoverBookId;
            if (_cursor.isNull(_cursorIndexOfCoverBookId)) {
              _tmpCoverBookId = null;
            } else {
              _tmpCoverBookId = _cursor.getLong(_cursorIndexOfCoverBookId);
            }
            final int _tmpBookCount;
            _tmpBookCount = _cursor.getInt(_cursorIndexOfBookCount);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new BookListEntity(_tmpId,_tmpName,_tmpDescription,_tmpCoverPath,_tmpCoverBookId,_tmpBookCount,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<BookListEntity> getBookListById(final long id) {
    final String _sql = "SELECT * FROM book_lists WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"book_lists"}, new Callable<BookListEntity>() {
      @Override
      @Nullable
      public BookListEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCoverPath = CursorUtil.getColumnIndexOrThrow(_cursor, "coverPath");
          final int _cursorIndexOfCoverBookId = CursorUtil.getColumnIndexOrThrow(_cursor, "coverBookId");
          final int _cursorIndexOfBookCount = CursorUtil.getColumnIndexOrThrow(_cursor, "bookCount");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final BookListEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCoverPath;
            if (_cursor.isNull(_cursorIndexOfCoverPath)) {
              _tmpCoverPath = null;
            } else {
              _tmpCoverPath = _cursor.getString(_cursorIndexOfCoverPath);
            }
            final Long _tmpCoverBookId;
            if (_cursor.isNull(_cursorIndexOfCoverBookId)) {
              _tmpCoverBookId = null;
            } else {
              _tmpCoverBookId = _cursor.getLong(_cursorIndexOfCoverBookId);
            }
            final int _tmpBookCount;
            _tmpBookCount = _cursor.getInt(_cursorIndexOfBookCount);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new BookListEntity(_tmpId,_tmpName,_tmpDescription,_tmpCoverPath,_tmpCoverBookId,_tmpBookCount,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getBookListByIdOnce(final long id,
      final Continuation<? super BookListEntity> $completion) {
    final String _sql = "SELECT * FROM book_lists WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BookListEntity>() {
      @Override
      @Nullable
      public BookListEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCoverPath = CursorUtil.getColumnIndexOrThrow(_cursor, "coverPath");
          final int _cursorIndexOfCoverBookId = CursorUtil.getColumnIndexOrThrow(_cursor, "coverBookId");
          final int _cursorIndexOfBookCount = CursorUtil.getColumnIndexOrThrow(_cursor, "bookCount");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final BookListEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCoverPath;
            if (_cursor.isNull(_cursorIndexOfCoverPath)) {
              _tmpCoverPath = null;
            } else {
              _tmpCoverPath = _cursor.getString(_cursorIndexOfCoverPath);
            }
            final Long _tmpCoverBookId;
            if (_cursor.isNull(_cursorIndexOfCoverBookId)) {
              _tmpCoverBookId = null;
            } else {
              _tmpCoverBookId = _cursor.getLong(_cursorIndexOfCoverBookId);
            }
            final int _tmpBookCount;
            _tmpBookCount = _cursor.getInt(_cursorIndexOfBookCount);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new BookListEntity(_tmpId,_tmpName,_tmpDescription,_tmpCoverPath,_tmpCoverBookId,_tmpBookCount,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<BookEntity>> getBooksInBookList(final long bookListId) {
    final String _sql = "\n"
            + "        SELECT b.* FROM books b\n"
            + "        INNER JOIN book_list_cross_ref ref ON b.id = ref.bookId\n"
            + "        WHERE ref.bookListId = ?\n"
            + "        ORDER BY ref.addedAt DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, bookListId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"books",
        "book_list_cross_ref"}, new Callable<List<BookEntity>>() {
      @Override
      @NonNull
      public List<BookEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "author");
          final int _cursorIndexOfPublisher = CursorUtil.getColumnIndexOrThrow(_cursor, "publisher");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfProgressType = CursorUtil.getColumnIndexOrThrow(_cursor, "progressType");
          final int _cursorIndexOfTotalPages = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPages");
          final int _cursorIndexOfCurrentPage = CursorUtil.getColumnIndexOrThrow(_cursor, "currentPage");
          final int _cursorIndexOfTotalChapters = CursorUtil.getColumnIndexOrThrow(_cursor, "totalChapters");
          final int _cursorIndexOfCurrentChapter = CursorUtil.getColumnIndexOrThrow(_cursor, "currentChapter");
          final int _cursorIndexOfCoverPath = CursorUtil.getColumnIndexOrThrow(_cursor, "coverPath");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfRating = CursorUtil.getColumnIndexOrThrow(_cursor, "rating");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfLastReadAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastReadAt");
          final List<BookEntity> _result = new ArrayList<BookEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BookEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpAuthor;
            if (_cursor.isNull(_cursorIndexOfAuthor)) {
              _tmpAuthor = null;
            } else {
              _tmpAuthor = _cursor.getString(_cursorIndexOfAuthor);
            }
            final String _tmpPublisher;
            if (_cursor.isNull(_cursorIndexOfPublisher)) {
              _tmpPublisher = null;
            } else {
              _tmpPublisher = _cursor.getString(_cursorIndexOfPublisher);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final ProgressType _tmpProgressType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfProgressType);
            _tmpProgressType = __converters.toProgressType(_tmp);
            final double _tmpTotalPages;
            _tmpTotalPages = _cursor.getDouble(_cursorIndexOfTotalPages);
            final double _tmpCurrentPage;
            _tmpCurrentPage = _cursor.getDouble(_cursorIndexOfCurrentPage);
            final Integer _tmpTotalChapters;
            if (_cursor.isNull(_cursorIndexOfTotalChapters)) {
              _tmpTotalChapters = null;
            } else {
              _tmpTotalChapters = _cursor.getInt(_cursorIndexOfTotalChapters);
            }
            final int _tmpCurrentChapter;
            _tmpCurrentChapter = _cursor.getInt(_cursorIndexOfCurrentChapter);
            final String _tmpCoverPath;
            if (_cursor.isNull(_cursorIndexOfCoverPath)) {
              _tmpCoverPath = null;
            } else {
              _tmpCoverPath = _cursor.getString(_cursorIndexOfCoverPath);
            }
            final BookStatus _tmpStatus;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toBookStatus(_tmp_1);
            final Float _tmpRating;
            if (_cursor.isNull(_cursorIndexOfRating)) {
              _tmpRating = null;
            } else {
              _tmpRating = _cursor.getFloat(_cursorIndexOfRating);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Long _tmpLastReadAt;
            if (_cursor.isNull(_cursorIndexOfLastReadAt)) {
              _tmpLastReadAt = null;
            } else {
              _tmpLastReadAt = _cursor.getLong(_cursorIndexOfLastReadAt);
            }
            _item = new BookEntity(_tmpId,_tmpTitle,_tmpAuthor,_tmpPublisher,_tmpDescription,_tmpProgressType,_tmpTotalPages,_tmpCurrentPage,_tmpTotalChapters,_tmpCurrentChapter,_tmpCoverPath,_tmpStatus,_tmpRating,_tmpCreatedAt,_tmpUpdatedAt,_tmpLastReadAt);
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
  public Flow<List<BookListEntity>> getBookListsForBook(final long bookId) {
    final String _sql = "\n"
            + "        SELECT bl.* FROM book_lists bl\n"
            + "        INNER JOIN book_list_cross_ref ref ON bl.id = ref.bookListId\n"
            + "        WHERE ref.bookId = ?\n"
            + "        ORDER BY bl.updatedAt DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, bookId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"book_lists",
        "book_list_cross_ref"}, new Callable<List<BookListEntity>>() {
      @Override
      @NonNull
      public List<BookListEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCoverPath = CursorUtil.getColumnIndexOrThrow(_cursor, "coverPath");
          final int _cursorIndexOfCoverBookId = CursorUtil.getColumnIndexOrThrow(_cursor, "coverBookId");
          final int _cursorIndexOfBookCount = CursorUtil.getColumnIndexOrThrow(_cursor, "bookCount");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<BookListEntity> _result = new ArrayList<BookListEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BookListEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCoverPath;
            if (_cursor.isNull(_cursorIndexOfCoverPath)) {
              _tmpCoverPath = null;
            } else {
              _tmpCoverPath = _cursor.getString(_cursorIndexOfCoverPath);
            }
            final Long _tmpCoverBookId;
            if (_cursor.isNull(_cursorIndexOfCoverBookId)) {
              _tmpCoverBookId = null;
            } else {
              _tmpCoverBookId = _cursor.getLong(_cursorIndexOfCoverBookId);
            }
            final int _tmpBookCount;
            _tmpBookCount = _cursor.getInt(_cursorIndexOfBookCount);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new BookListEntity(_tmpId,_tmpName,_tmpDescription,_tmpCoverPath,_tmpCoverBookId,_tmpBookCount,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object isBookInBookList(final long bookListId, final long bookId,
      final Continuation<? super Boolean> $completion) {
    final String _sql = "SELECT EXISTS(SELECT 1 FROM book_list_cross_ref WHERE bookListId = ? AND bookId = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, bookListId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, bookId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp != 0;
          } else {
            _result = false;
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
