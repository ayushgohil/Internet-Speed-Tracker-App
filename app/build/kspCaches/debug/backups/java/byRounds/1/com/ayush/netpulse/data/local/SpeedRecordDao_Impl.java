package com.ayush.netpulse.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
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
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SpeedRecordDao_Impl implements SpeedRecordDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SpeedRecordEntity> __insertionAdapterOfSpeedRecordEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOldRecords;

  public SpeedRecordDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSpeedRecordEntity = new EntityInsertionAdapter<SpeedRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `speed_records` (`id`,`timestamp`,`downloadBps`,`uploadBps`,`networkType`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SpeedRecordEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindLong(3, entity.getDownloadBps());
        statement.bindLong(4, entity.getUploadBps());
        statement.bindString(5, entity.getNetworkType());
      }
    };
    this.__preparedStmtOfDeleteOldRecords = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM speed_records WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final SpeedRecordEntity record,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSpeedRecordEntity.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOldRecords(final long before,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOldRecords.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, before);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOldRecords.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SpeedRecordEntity>> getRecentRecords(final int limit) {
    final String _sql = "SELECT * FROM speed_records ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"speed_records"}, new Callable<List<SpeedRecordEntity>>() {
      @Override
      @NonNull
      public List<SpeedRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDownloadBps = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadBps");
          final int _cursorIndexOfUploadBps = CursorUtil.getColumnIndexOrThrow(_cursor, "uploadBps");
          final int _cursorIndexOfNetworkType = CursorUtil.getColumnIndexOrThrow(_cursor, "networkType");
          final List<SpeedRecordEntity> _result = new ArrayList<SpeedRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpeedRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpDownloadBps;
            _tmpDownloadBps = _cursor.getLong(_cursorIndexOfDownloadBps);
            final long _tmpUploadBps;
            _tmpUploadBps = _cursor.getLong(_cursorIndexOfUploadBps);
            final String _tmpNetworkType;
            _tmpNetworkType = _cursor.getString(_cursorIndexOfNetworkType);
            _item = new SpeedRecordEntity(_tmpId,_tmpTimestamp,_tmpDownloadBps,_tmpUploadBps,_tmpNetworkType);
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
  public Object getRecordsSince(final long since,
      final Continuation<? super List<SpeedRecordEntity>> $completion) {
    final String _sql = "SELECT * FROM speed_records WHERE timestamp >= ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SpeedRecordEntity>>() {
      @Override
      @NonNull
      public List<SpeedRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDownloadBps = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadBps");
          final int _cursorIndexOfUploadBps = CursorUtil.getColumnIndexOrThrow(_cursor, "uploadBps");
          final int _cursorIndexOfNetworkType = CursorUtil.getColumnIndexOrThrow(_cursor, "networkType");
          final List<SpeedRecordEntity> _result = new ArrayList<SpeedRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpeedRecordEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpDownloadBps;
            _tmpDownloadBps = _cursor.getLong(_cursorIndexOfDownloadBps);
            final long _tmpUploadBps;
            _tmpUploadBps = _cursor.getLong(_cursorIndexOfUploadBps);
            final String _tmpNetworkType;
            _tmpNetworkType = _cursor.getString(_cursorIndexOfNetworkType);
            _item = new SpeedRecordEntity(_tmpId,_tmpTimestamp,_tmpDownloadBps,_tmpUploadBps,_tmpNetworkType);
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

  @Override
  public Object getCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM speed_records";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
