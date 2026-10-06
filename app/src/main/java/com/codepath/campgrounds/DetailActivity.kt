package com.codepath.campgrounds

import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val campgroundNameTV =
            findViewById<TextView>(R.id.campgroundName)

        val campgroundDescriptionTV =
            findViewById<TextView>(R.id.campgroundDescription)

        val campgroundLatLongTV =
            findViewById<TextView>(R.id.campgroundLocation)

        val campgroundImageIV =
            findViewById<ImageView>(R.id.campgroundImage)

        val campground: Campground? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                intent.getSerializableExtra(
                    "campground",
                    Campground::class.java
                )

            } else {

                @Suppress("DEPRECATION")
                intent.getSerializableExtra("campground") as? Campground
            }

        if (campground == null) {
            finish()
            return
        }

        campgroundNameTV.text = campground.name
        campgroundDescriptionTV.text = campground.description
        campgroundLatLongTV.text = campground.latLong

        Glide.with(this)
            .load(campground.imageUrl)
            .centerCrop()
            .into(campgroundImageIV)
    }
}