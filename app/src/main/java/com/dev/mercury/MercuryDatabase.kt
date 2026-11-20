
package com.dev.mercury

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artwork: String,
    val favourite: Boolean,
    val discoveredAt: Long
)

class MercuryDatabase(context: Context) :
    SQLiteOpenHelper(context, "mercury.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE songs (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                artwork TEXT,
                favourite INTEGER DEFAULT 0,
                discovered_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Preserve existing data when upgrading in future.
    }

    fun saveSong(
        id: String,
        title: String,
        artist: String,
        artwork: String
    ) {
        val db = writableDatabase

        val values = ContentValues().apply {
            put("id", id)
            put("title", title)
            put("artist", artist)
            put("artwork", artwork)
            put("discovered_at", System.currentTimeMillis())
        }

        val existing = db.query(
            "songs",
            arrayOf("id"),
            "id = ?",
            arrayOf(id),
            null, null, null
        )

        val alreadyExists = existing.use { it.moveToFirst() }

        if (alreadyExists) {
            val update = ContentValues().apply {
                put("title", title)
                put("artist", artist)
                put("artwork", artwork)
            }

            db.update("songs", update, "id = ?", arrayOf(id))
        } else {
            db.insert("songs", null, values)
        }
    }

    fun getAllSongs(): List<Song> {
        return getSongs("1=1", null)
    }

    fun getFavouriteSongs(): List<Song> {
        return getSongs("favourite = 1", null)
    }

    private fun getSongs(
        selection: String,
        args: Array<String>?
    ): List<Song> {

        val songs = mutableListOf<Song>()

        readableDatabase.query(
            "songs",
            null,
            selection,
            args,
            null,
            null,
            "discovered_at DESC"
        ).use { cursor ->

            while (cursor.moveToNext()) {
                songs.add(
                    Song(
                        id = cursor.getString(
                            cursor.getColumnIndexOrThrow("id")
                        ),
                        title = cursor.getString(
                            cursor.getColumnIndexOrThrow("title")
                        ),
                        artist = cursor.getString(
                            cursor.getColumnIndexOrThrow("artist")
                        ),
                        artwork = cursor.getString(
                            cursor.getColumnIndexOrThrow("artwork")
                        ) ?: "",
                        favourite = cursor.getInt(
                            cursor.getColumnIndexOrThrow("favourite")
                        ) == 1,
                        discoveredAt = cursor.getLong(
                            cursor.getColumnIndexOrThrow("discovered_at")
                        )
                    )
                )
            }
        }

        return songs
    }

    fun setFavourite(id: String, favourite: Boolean) {
        val values = ContentValues().apply {
            put("favourite", if (favourite) 1 else 0)
        }

        writableDatabase.update(
            "songs",
            values,
            "id = ?",
            arrayOf(id)
        )
    }

    fun deleteSong(id: String) {
        writableDatabase.delete(
            "songs",
            "id = ?",
            arrayOf(id)
        )
    }
}
