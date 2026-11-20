
package com.dev.mercury

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var mercuryDatabase: MercuryDatabase

    private val executor = Executors.newSingleThreadExecutor()

    private val sampleRate = 44100
    private val recordingDuration = 4000

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startRecognition()
            } else {
                showMessage("Microphone permission is required.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mercuryDatabase = MercuryDatabase(this)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            view.setPadding(
                bars.left, bars.top, bars.right, bars.bottom
            )
            insets
        }

        findViewById<View>(R.id.listenButton)
            .setOnClickListener {
                checkMicrophonePermission()
            }

        findViewById<View>(R.id.libraryButton)
            .setOnClickListener {
                showSongList(false)
            }

        findViewById<View>(R.id.favouritesButton)
            .setOnClickListener {
                showSongList(true)
            }

        findViewById<View>(R.id.profileButton)
            .setOnClickListener {
                startActivity(
                    android.content.Intent(
                        this,
                        ProfileActivity::class.java
                    )
                )
            }

        populateRecentDiscoveries()
    }

    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startRecognition()
        } else {
            permissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    private fun startRecognition() {
        if (BuildConfig.RAPIDAPI_KEY.isBlank()) {
            showMessage("RapidAPI key is missing.")
            return
        }

        findViewById<View>(R.id.listenButton).isEnabled = false

        showMessage("Listening... play a song nearby.")

        executor.execute {
            try {
                val audioData = recordAudio()

                val encodedAudio = Base64.encodeToString(
                    audioData,
                    Base64.NO_WRAP
                )

                val result = identifySong(encodedAudio)

                runOnUiThread {
                    findViewById<View>(R.id.listenButton)
                        .isEnabled = true
                    showResult(result)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    findViewById<View>(R.id.listenButton)
                        .isEnabled = true
                    showMessage(
                        "Recognition failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun recordAudio(): ByteArray {
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize <= 0) {
            throw Exception("Audio recording is unavailable.")
        }

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(bufferSize, sampleRate * 2)
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            throw Exception("Could not initialise microphone.")
        }

        val output = ByteArrayOutputStreamCompat()

        try {
            val buffer = ByteArray(bufferSize)

            audioRecord.startRecording()

            val startTime = System.currentTimeMillis()

            while (
                System.currentTimeMillis() - startTime <
                recordingDuration
            ) {
                val count = audioRecord.read(
                    buffer, 0, buffer.size
                )

                if (count > 0) {
                    output.write(buffer, count)
                } else if (count < 0) {
                    throw Exception("Microphone read error: $count")
                }
            }

        } finally {
            if (audioRecord.recordingState ==
                AudioRecord.RECORDSTATE_RECORDING
            ) {
                audioRecord.stop()
            }
            audioRecord.release()
        }

        if (output.size() == 0) {
            throw Exception("No audio was recorded.")
        }

        return output.toByteArray()
    }

    private fun identifySong(base64Audio: String): JSONObject {
        val timezone = URLEncoder.encode(
            "Asia/Kolkata", "UTF-8"
        )
        val locale = URLEncoder.encode("en-US", "UTF-8")

        val endpoint = java.net.URL(
            "https://shazam.p.rapidapi.com/songs/v3/detect" +
                    "?timezone=$timezone&locale=$locale"
        )

        val connection = endpoint.openConnection()
                as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20000
            connection.readTimeout = 20000
            connection.doOutput = true

            connection.setRequestProperty(
                "Content-Type", "text/plain"
            )
            connection.setRequestProperty(
                "X-RapidAPI-Host", "shazam.p.rapidapi.com"
            )
            connection.setRequestProperty(
                "X-RapidAPI-Key", BuildConfig.RAPIDAPI_KEY
            )
            connection.setRequestProperty(
                "Accept", "application/json"
            )

            connection.outputStream.use {
                it.write(base64Audio.toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode

            val responseStream =
                if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val response = responseStream
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: ""

            if (status !in 200..299) {
                throw Exception(
                    "API error $status: $response"
                )
            }

            return JSONObject(response)

        } finally {
            connection.disconnect()
        }
    }






    private fun showResult(data: JSONObject) {

        val results = data.optJSONObject("results")
        val matches = results?.optJSONArray("matches")
        val firstMatch = matches?.optJSONObject(0)

        if (firstMatch == null) {
            showMessage("No song found. Try again.")
            return
        }

        val songId = firstMatch.optString("id")

        val resources = data.optJSONObject("resources")
        val track = resources
            ?.optJSONObject("shazam-songs")
            ?.optJSONObject(songId)

        val attributes = track?.optJSONObject("attributes")

        if (attributes == null) {
            showMessage("Song details unavailable.")
            return
        }

        val title = attributes.optString("title", "Unknown song")
        val artist = attributes.optString("artist", "Unknown artist")

        val images = attributes.optJSONObject("images")
        val artworkUrl = images?.optString("coverArt", "") ?: ""
        mercuryDatabase.saveSong(
            songId,
            title,
            artist,
            artworkUrl
        )
        populateRecentDiscoveries()

        // Main result card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))

            background = GradientDrawable().apply {
                setColor(Color.parseColor("#190909"))
                cornerRadius = dp(24).toFloat()
            }
        }

        // Mercury branding
        val brand = TextView(this).apply {
            text = "M E R C U R Y"
            textSize = 13f
            setTextColor(Color.parseColor("#B45B5B"))
            gravity = Gravity.CENTER
        }

        card.addView(
            brand,
            LinearLayout.LayoutParams(-1, dp(35))
        )

        // Album artwork
        val artwork = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#3A1111"))
                cornerRadius = dp(12).toFloat()
            }
            clipToOutline = true
        }

        val artworkParams = LinearLayout.LayoutParams(
            dp(220), dp(220)
        ).apply {
            topMargin = dp(18)
            bottomMargin = dp(24)
        }

        card.addView(artwork, artworkParams)

        // Identification status
        val status = TextView(this).apply {
            text = "✦  SONG IDENTIFIED"
            textSize = 11f
            setTextColor(Color.parseColor("#B45B5B"))
            gravity = Gravity.CENTER
        }

        card.addView(
            status,
            LinearLayout.LayoutParams(-1, dp(28))
        )

        // Song title
        val songTitle = TextView(this).apply {
            text = title
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(5))
        }

        card.addView(songTitle)

        // Artist
        val songArtist = TextView(this).apply {
            text = artist
            textSize = 16f
            setTextColor(Color.parseColor("#A69A9A"))
            gravity = Gravity.CENTER
        }

        card.addView(
            songArtist,
            LinearLayout.LayoutParams(-1, dp(35))
        )

        // Song ID
        val idText = TextView(this).apply {
            text = "Shazam ID: $songId"
            textSize = 11f
            setTextColor(Color.parseColor("#746565"))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(8))
        }

        card.addView(idText)

        val dialog = AlertDialog.Builder(this)
            .setView(card)
            .setPositiveButton("DONE", null)
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        // Download artwork without blocking the UI
        if (artworkUrl.startsWith("https://")) {
            executor.execute {
                try {
                    val connection = java.net.URL(artworkUrl)
                        .openConnection()

                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    val bitmap = connection.getInputStream().use {
                        BitmapFactory.decodeStream(it)
                    }

                    runOnUiThread {
                        if (!isFinishing && !isDestroyed &&
                            bitmap != null
                        ) {
                            artwork.setImageBitmap(bitmap)
                        }
                    }

                } catch (e: Exception) {
                    android.util.Log.e(
                        "Mercury",
                        "Artwork loading failed",
                        e
                    )
                }
            }
        }
    }

    private fun populateRecentDiscoveries() {
        val container = findViewById<LinearLayout>(R.id.recentSongsContainer)
            ?: return
        container.removeAllViews()

        val songs = mercuryDatabase.getAllSongs().take(4)
        if (songs.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Your music journey starts here.\\nIdentify a song and it will appear here."
                textSize = 13f
                setTextColor(Color.parseColor("#A69A9A"))
                setPadding(dp(16), dp(18), dp(16), dp(18))
                gravity = Gravity.CENTER_VERTICAL
            }
            container.addView(empty)
            return
        }

        songs.forEach { song ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#190909"))
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), Color.parseColor("#351010"))
                }
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(76)
                )
                params.bottomMargin = dp(9)
                layoutParams = params
                setOnClickListener { showSongDetails(song, false) }
            }

            val cover = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.parseColor("#3A1111"))
                clipToOutline = true
            }
            row.addView(cover, LinearLayout.LayoutParams(dp(54), dp(54)))

            val details = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(13), 0, 0, 0)
            }
            val title = TextView(this).apply {
                text = song.title
                textSize = 13f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(Color.WHITE)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val artist = TextView(this).apply {
                text = song.artist
                textSize = 11f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(Color.parseColor("#A69A9A"))
                setPadding(0, dp(4), 0, 0)
            }
            details.addView(title)
            details.addView(artist)
            row.addView(
                details,
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            val marker = TextView(this).apply {
                text = if (song.favourite) "♥" else "♪"
                textSize = 19f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#B45B5B"))
            }
            row.addView(marker, LinearLayout.LayoutParams(dp(30), dp(48)))
            container.addView(row)

            if (song.artwork.startsWith("https://")) {
                executor.execute {
                    try {
                        val connection = URL(song.artwork).openConnection()
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000
                        val bitmap = connection.getInputStream().use {
                            BitmapFactory.decodeStream(it)
                        }
                        runOnUiThread {
                            if (!isFinishing && !isDestroyed && bitmap != null) {
                                cover.setImageBitmap(bitmap)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Mercury", "Recent artwork failed", e)
                    }
                }
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }






    private fun showSongList(favouritesOnly: Boolean) {

        val songs = if (favouritesOnly) {
            mercuryDatabase.getFavouriteSongs()
        } else {
            mercuryDatabase.getAllSongs()
        }

        val heading = if (favouritesOnly) "SAVED" else "YOUR LIBRARY"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(16))
            setBackgroundColor(Color.BLACK)
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brand = TextView(this).apply {
            text = "M E R C U R Y"
            textSize = 14f
            setTextColor(Color.parseColor("#B45B5B"))
        }

        header.addView(
            brand,
            LinearLayout.LayoutParams(0, dp(40), 1f)
        )

        val close = TextView(this).apply {
            text = "✕"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(12), 0, dp(4), 0)
        }

        header.addView(close)
        root.addView(header)

        // Page title
        val pageTitle = TextView(this).apply {
            text = if (favouritesOnly)
                "Your favourites"
            else
                "Your discoveries"

            textSize = 27f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, dp(24), 0, dp(5))
        }

        root.addView(pageTitle)

        val subtitle = TextView(this).apply {
            text = "${songs.size} songs in your collection"
            textSize = 13f
            setTextColor(Color.parseColor("#A69A9A"))
            setPadding(0, 0, 0, dp(22))
        }

        root.addView(subtitle)

        // Scrollable song collection
        val scrollView = android.widget.ScrollView(this)

        val songContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        if (songs.isEmpty()) {

            val empty = TextView(this).apply {
                text = if (favouritesOnly)
                    "No favourites yet.\n\nOpen your Library and tap the heart on songs you love."
                else
                    "Your music journey starts here.\n\nIdentify your first song to see it here."

                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#A69A9A"))
                setPadding(dp(20), dp(70), dp(20), dp(40))
            }

            songContainer.addView(empty)

        } else {

            songs.forEach { song ->

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(12), dp(12), dp(12))

                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#190909"))
                        cornerRadius = dp(14).toFloat()
                    }

                    val params = LinearLayout.LayoutParams(
                        -1, dp(88)
                    )
                    params.bottomMargin = dp(12)
                    layoutParams = params
                }

                // Album artwork
                val image = ImageView(this).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(Color.parseColor("#3A1111"))
                    clipToOutline = true
                }

                row.addView(
                    image,
                    LinearLayout.LayoutParams(dp(64), dp(64))
                )

                // Song title and artist
                val info = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(13), 0, dp(5), 0)
                }

                val songName = TextView(this).apply {
                    text = song.title
                    textSize = 14f
                    maxLines = 2
                    setTextColor(Color.WHITE)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }

                info.addView(songName)

                val artistName = TextView(this).apply {
                    text = song.artist
                    textSize = 12f
                    maxLines = 1
                    setTextColor(Color.parseColor("#A69A9A"))
                    setPadding(0, dp(5), 0, 0)
                }

                info.addView(artistName)

                row.addView(
                    info,
                    LinearLayout.LayoutParams(0, -2, 1f)
                )

                // Favourite button
                val heart = TextView(this).apply {
                    text = if (song.favourite) "♥" else "♡"
                    textSize = 25f
                    gravity = Gravity.CENTER
                    setTextColor(
                        if (song.favourite)
                            Color.parseColor("#B45B5B")
                        else
                            Color.LTGRAY
                    )
                    setPadding(dp(8), 0, dp(4), 0)
                }

                row.addView(
                    heart,
                    LinearLayout.LayoutParams(dp(42), dp(55))
                )

                heart.setOnClickListener {
                    mercuryDatabase.setFavourite(
                        song.id,
                        !song.favourite
                    )
                    showSongList(favouritesOnly)
                }

                row.setOnClickListener {
                    showSongDetails(song, favouritesOnly)
                }

                songContainer.addView(row)

                // Download album artwork
                if (song.artwork.startsWith("https://")) {
                    executor.execute {
                        try {
                            val connection =
                                java.net.URL(song.artwork)
                                    .openConnection()

                            connection.connectTimeout = 10000
                            connection.readTimeout = 10000

                            val bitmap = connection.getInputStream().use {
                                BitmapFactory.decodeStream(it)
                            }

                            runOnUiThread {
                                if (!isFinishing &&
                                    !isDestroyed &&
                                    bitmap != null
                                ) {
                                    image.setImageBitmap(bitmap)
                                }
                            }

                        } catch (e: Exception) {
                            android.util.Log.e(
                                "Mercury",
                                "Library artwork failed",
                                e
                            )
                        }
                    }
                }
            }
        }

        scrollView.addView(songContainer)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams( -1, 0, 1f)
        )

        val footer = TextView(this).apply {
            text = "MUSIC DISCOVERY, REIMAGINED."
            gravity = Gravity.CENTER
            textSize = 10f
            letterSpacing = 0.15f
            setTextColor(Color.parseColor("#746565"))
            setPadding(0, dp(16), 0, dp(5))
        }

        root.addView(footer)

        val dialog = AlertDialog.Builder(this)
            .setView(root)
            .create()

        close.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawableResource(
                android.R.color.transparent
            )
            setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }


    private fun showSongDetails(
        song: Song,
        favouritesOnly: Boolean
    ) {

        AlertDialog.Builder(this)
            .setTitle(song.title)
            .setMessage(
                "Artist: ${song.artist}\n\n" +
                        "Shazam ID: ${song.id}\n\n" +
                        if (song.favourite)
                            "Added to your favourites."
                        else
                            "Not in your favourites yet."
            )
            .setPositiveButton(
                if (song.favourite) "Remove Favourite"
                else "Add to Favourites"
            ) { _, _ ->

                mercuryDatabase.setFavourite(
                    song.id,
                    !song.favourite
                )

                showSongList(favouritesOnly)
            }
            .setNeutralButton("Delete Song") { _, _ ->
                AlertDialog.Builder(this)
                    .setTitle("Delete discovery?")
                    .setMessage("This will permanently remove this song from Mercury.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        mercuryDatabase.deleteSong(song.id)
                        showSongList(favouritesOnly)
                    }
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }
}

/* Simple byte accumulator for the recorded PCM sample. */
private class ByteArrayOutputStreamCompat {
    private val output =
        java.io.ByteArrayOutputStream()

    fun write(data: ByteArray, count: Int) {
        output.write(data, 0, count)
    }

    fun size(): Int = output.size()

    fun toByteArray(): ByteArray = output.toByteArray()
}
