package com.dev.mercury

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat

class ProfileActivity : AppCompatActivity() {

    private val backgroundColor = Color.BLACK
    private val surfaceColor = Color.parseColor("#190909")
    private val accentColor = Color.parseColor("#830000")
    private val softAccent = Color.parseColor("#B45B5B")
    private val primaryText = Color.WHITE
    private val secondaryText = Color.parseColor("#A69A9A")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = MercuryDatabase(this)
        val songs = database.getAllSongs()
        val favourites = database.getFavouriteSongs()
        val artists = songs.map { it.artist.trim().lowercase() }.distinct().size

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(24))
            setBackgroundColor(backgroundColor)
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
            addView(root)
        }
        setContentView(scroll)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = text("M E R C U R Y", 14f, softAccent, true)
        header.addView(brand, LinearLayout.LayoutParams(0, dp(45), 1f))

        val back = text("←", 27f, primaryText).apply {
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }
        header.addView(back, LinearLayout.LayoutParams(dp(45), dp(45)))
        root.addView(header)

        val avatar = TextView(this).apply {
            text = "♫"
            textSize = 40f
            gravity = Gravity.CENTER
            setTextColor(primaryText)
            background = rounded(accentColor, 90)
        }
        val avatarParams = LinearLayout.LayoutParams(dp(90), dp(90))
        avatarParams.gravity = Gravity.CENTER
        avatarParams.topMargin = dp(30)
        root.addView(avatar, avatarParams)

        root.addView(text("Music enthusiast", 24f, primaryText, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(15), 0, dp(5))
        })
        root.addView(text("MUSIC DISCOVERY, REIMAGINED.", 10f, softAccent, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
        })

        addSection(root, "YOUR MUSIC JOURNEY")
        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        addStat(stats, "DISCOVERED", songs.size.toString())
        addStat(stats, "FAVOURITES", favourites.size.toString())
        addStat(stats, "ARTISTS", artists.toString())
        root.addView(stats)

        addSection(root, "RECENT DISCOVERIES")
        if (songs.isEmpty()) {
            root.addView(text("Your music journey starts here.", 13f, secondaryText).apply {
                setPadding(0, dp(12), 0, dp(14))
            })
        } else {
            songs.take(5).forEach { song ->
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(15), dp(13), dp(15), dp(13))
                    background = rounded(surfaceColor, 18)
                }
                item.addView(text(song.title, 14f, primaryText, true))
                item.addView(text(song.artist, 12f, secondaryText).apply {
                    setPadding(0, dp(5), 0, 0)
                })
                val params = LinearLayout.LayoutParams(-1, -2)
                params.bottomMargin = dp(9)
                root.addView(item, params)
            }
        }

        addSection(root, "ABOUT MERCURY")
        val about = """
            Mercury is a music discovery application that identifies songs playing around you.

            TECHNOLOGY

            • Kotlin
            • Android Studio
            • Shazam Music Recognition API
            • RapidAPI
            • SQLite Database

            APPLICATION VERSION

            1.0
        """.trimIndent()

        root.addView(text(about, 12f, secondaryText).apply {
            setPadding(dp(17), dp(17), dp(17), dp(17))
            background = rounded(surfaceColor, 18)
            setLineSpacing(dp(5).toFloat(), 1f)
        })

        root.addView(text("MERCURY  •  MUSIC DISCOVERY, REIMAGINED.", 9f, secondaryText).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(25), 0, dp(10))
            letterSpacing = 0.08f
        })
    }

    private fun addSection(root: LinearLayout, title: String) {
        root.addView(text(title, 10f, softAccent, true).apply {
            letterSpacing = 0.13f
            setPadding(0, dp(27), 0, dp(13))
        })
    }

    private fun addStat(parent: LinearLayout, label: String, value: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(16), dp(5), dp(16))
            background = rounded(surfaceColor, 16)
        }
        box.addView(text(value, 24f, primaryText, true).apply {
            gravity = Gravity.CENTER
        })
        box.addView(text(label, 8f, secondaryText, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, 0)
        })
        val params = LinearLayout.LayoutParams(0, dp(82), 1f)
        params.marginEnd = dp(7)
        parent.addView(box, params)
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            val jakarta = ResourcesCompat.getFont(
                this@ProfileActivity,
                R.font.plus_jakarta_regular
            ) ?: Typeface.DEFAULT
            typeface = Typeface.create(
                jakarta,
                if (bold) Typeface.BOLD else Typeface.NORMAL
            )
        }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
