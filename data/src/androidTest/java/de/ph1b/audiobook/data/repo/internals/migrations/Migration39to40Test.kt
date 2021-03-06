package de.ph1b.audiobook.data.repo.internals.migrations

import android.annotation.SuppressLint
import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.arch.persistence.db.SupportSQLiteQueryBuilder
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory
import android.arch.persistence.room.OnConflictStrategy
import android.content.ContentValues
import android.support.test.InstrumentationRegistry
import androidx.core.database.getInt
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.data.repo.internals.mapRows
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Test for the database 39->40 version migration
 */
class Migration39to40Test {

  private lateinit var db: SupportSQLiteDatabase
  private lateinit var helper: SupportSQLiteOpenHelper

  @Before
  fun setUp() {
    val context = InstrumentationRegistry.getTargetContext()
    val config = SupportSQLiteOpenHelper.Configuration
      .builder(context)
      .callback(object : SupportSQLiteOpenHelper.Callback(39) {
        override fun onCreate(db: SupportSQLiteDatabase) {
          db.execSQL(BookTable.CREATE_TABLE)
        }

        override fun onUpgrade(db: SupportSQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        }
      })
      .build()
    helper = FrameworkSQLiteOpenHelperFactory().create(config)
    db = helper.writableDatabase
  }

  @After
  fun tearDown() {
    helper.close()
  }

  @Test
  fun negativeNumbersBecomeZero() {
    val bookCvWithNegativeTime = contentValuesForBookWithTime(-100)
    db.insert(BookTable.TABLE_NAME, OnConflictStrategy.FAIL, bookCvWithNegativeTime)

    val bookCvWithPositiveTime = contentValuesForBookWithTime(5000)
    db.insert(BookTable.TABLE_NAME, OnConflictStrategy.FAIL, bookCvWithPositiveTime)

    Migration39to40().migrate(db)

    val query = SupportSQLiteQueryBuilder.builder(BookTable.TABLE_NAME)
      .columns(arrayOf(BookTable.TIME))
      .create()
    val times = db.query(query)
      .mapRows { getInt(BookTable.TIME) }
    assertThat(times).containsExactly(0, 5000)
  }

  @SuppressLint("SdCardPath")
  private fun contentValuesForBookWithTime(time: Int) = ContentValues().apply {
    put(BookTable.NAME, "firstBookName")
    put(BookTable.CURRENT_MEDIA_PATH, "/sdcard/file1.mp3")
    put(BookTable.PLAYBACK_SPEED, 1F)
    put(BookTable.ROOT, "/sdcard")
    put(BookTable.TIME, time)
    put(BookTable.TYPE, "COLLECTION_FOLDER")
  }

  object BookTable {
    const val ID = "bookId"
    const val NAME = "bookName"
    const val AUTHOR = "bookAuthor"
    const val CURRENT_MEDIA_PATH = "bookCurrentMediaPath"
    const val PLAYBACK_SPEED = "bookSpeed"
    const val ROOT = "bookRoot"
    const val TIME = "bookTime"
    const val TYPE = "bookType"
    const val ACTIVE = "BOOK_ACTIVE"
    const val TABLE_NAME = "tableBooks"
    const val CREATE_TABLE = """
      CREATE TABLE $TABLE_NAME (
        $ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $NAME TEXT NOT NULL,
        $AUTHOR TEXT,
        $CURRENT_MEDIA_PATH TEXT NOT NULL,
        $PLAYBACK_SPEED REAL NOT NULL,
        $ROOT TEXT NOT NULL,
        $TIME INTEGER NOT NULL,
        $TYPE TEXT NOT NULL,
        $ACTIVE INTEGER NOT NULL DEFAULT 1
      )
    """
  }
}
