package com.codepath.campgrounds

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import com.codepath.campgrounds.databinding.ActivityMainBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers

fun createJson() = Json {
    isLenient = true
    ignoreUnknownKeys = true
    useAlternativeNames = false
}

private const val TAG = "CampgroundsMain/"
private val PARKS_API_KEY = BuildConfig.API_KEY

private val CAMPGROUNDS_URL =
    "https://developer.nps.gov/api/v1/campgrounds?api_key=$PARKS_API_KEY"

class MainActivity : AppCompatActivity() {

    private lateinit var campgroundsRecyclerView: RecyclerView
    private lateinit var binding: ActivityMainBinding

    private val campgrounds = mutableListOf<Campground>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        campgroundsRecyclerView =
            findViewById(R.id.campgrounds)

        val campgroundAdapter =
            CampgroundAdapter(this, campgrounds)

        campgroundsRecyclerView.adapter =
            campgroundAdapter

        campgroundsRecyclerView.layoutManager =
            LinearLayoutManager(this).also {

                val dividerItemDecoration =
                    DividerItemDecoration(
                        this,
                        it.orientation
                    )

                campgroundsRecyclerView.addItemDecoration(
                    dividerItemDecoration
                )
            }

        val client = AsyncHttpClient()

        client.get(
            CAMPGROUNDS_URL,
            object : JsonHttpResponseHandler() {

                override fun onFailure(
                    statusCode: Int,
                    headers: Headers?,
                    response: String?,
                    throwable: Throwable?
                ) {

                    Log.e(
                        TAG,
                        "Failed to fetch campgrounds: $statusCode"
                    )
                }

                override fun onSuccess(
                    statusCode: Int,
                    headers: Headers,
                    json: JSON
                ) {

                    Log.i(
                        TAG,
                        "Successfully fetched campgrounds"
                    )

                    try {

                        val parsedJson =
                            createJson().decodeFromString<CampgroundResponse>(
                                json.jsonObject.toString()
                            )

                        parsedJson.data?.let { list ->

                            campgrounds.clear()
                            campgrounds.addAll(list)

                            campgroundAdapter.notifyDataSetChanged()
                        }

                    } catch (e: Exception) {

                        Log.e(
                            TAG,
                            "Exception: $e"
                        )
                    }
                }
            }
        )
    }
}