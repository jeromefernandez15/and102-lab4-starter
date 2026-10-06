package com.codepath.campgrounds

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CampgroundAdapter(
    private val context: Context,
    private val campgrounds: List<Campground>
) : RecyclerView.Adapter<CampgroundAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_campground, parent, false)

        return ViewHolder(view)
    }

    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        private val nameTextView =
            itemView.findViewById<TextView>(R.id.campgroundName)

        private val descriptionTextView =
            itemView.findViewById<TextView>(R.id.campgroundDescription)

        private val locationTextView =
            itemView.findViewById<TextView>(R.id.campgroundLocation)

        private val imageView =
            itemView.findViewById<ImageView>(R.id.campgroundImage)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(campground: Campground) {

            nameTextView.text = campground.name
            descriptionTextView.text = campground.description
            locationTextView.text = campground.latLong

            Glide.with(context)
                .load(campground.imageUrl)
                .centerCrop()
                .into(imageView)
        }

        override fun onClick(v: View?) {

            val position = absoluteAdapterPosition

            if (position != RecyclerView.NO_POSITION) {

                val campground = campgrounds[position]

                val intent =
                    Intent(context, DetailActivity::class.java)

                intent.putExtra(
                    "campground",
                    campground
                )

                context.startActivity(intent)
            }
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        holder.bind(campgrounds[position])
    }

    override fun getItemCount(): Int {
        return campgrounds.size
    }
}