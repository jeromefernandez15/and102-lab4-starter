package com.codepath.campgrounds

import android.net.NetworkRequest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import com.codepath.campgrounds.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private lateinit var binding: ActivityMainBinding
    private lateinit var campgroundAdapter: CampgroundAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var offlineBanner: TextView
    private lateinit var searchView: SearchView
    private lateinit var cacheSwitch: SwitchCompat

    private val allCampgrounds = mutableListOf<Campground>()
    private val displayedCampgrounds = mutableListOf<Campground>()

    private var currentQuery = ""
    private var wasOffline = false

    private val preferences by lazy {
        getSharedPreferences("campground_preferences", Context.MODE_PRIVATE)
    }

    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                runOnUiThread {
                    offlineBanner.visibility = View.GONE

                    if (wasOffline) {
                        wasOffline = false
                        fetchCampgrounds()
                    }
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    wasOffline = true
                    offlineBanner.visibility = View.VISIBLE
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recyclerView =
            findViewById<RecyclerView>(R.id.campgrounds)

        swipeRefreshLayout =
            findViewById(R.id.swipeRefreshLayout)

        offlineBanner =
            findViewById(R.id.offlineBanner)

        searchView =
            findViewById(R.id.searchView)

        cacheSwitch =
            findViewById(R.id.cacheSwitch)

        campgroundAdapter =
            CampgroundAdapter(
                this,
                displayedCampgrounds
            )

        recyclerView.adapter = campgroundAdapter

        recyclerView.layoutManager =
            LinearLayoutManager(this).also {

                recyclerView.addItemDecoration(
                    DividerItemDecoration(
                        this,
                        it.orientation
                    )
                )
            }

        // Load saved preference
        val cachingEnabled =
            preferences.getBoolean(
                "cache_enabled",
                true
            )

        cacheSwitch.isChecked = cachingEnabled

        // Stretch 1: Swipe to Refresh
        swipeRefreshLayout.setOnRefreshListener {
            fetchCampgrounds()
        }

        // Stretch 2: Cache preference using SharedPreferences
        cacheSwitch.setOnCheckedChangeListener { _, isChecked ->

            preferences
                .edit()
                .putBoolean(
                    "cache_enabled",
                    isChecked
                )
                .apply()

            if (isChecked) {

                Toast.makeText(
                    this,
                    "Offline caching enabled",
                    Toast.LENGTH_SHORT
                ).show()

                fetchCampgrounds()

            } else {

                Toast.makeText(
                    this,
                    "Offline caching disabled",
                    Toast.LENGTH_SHORT
                ).show()

                lifecycleScope.launch(Dispatchers.IO) {

                    AppDatabase
                        .getDatabase(applicationContext)
                        .campgroundDao()
                        .deleteAll()
                }
            }
        }

        // Stretch 3: Search UI
        searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(
                    query: String?
                ): Boolean {

                    currentQuery =
                        query.orEmpty()

                    filterCampgrounds()

                    return true
                }

                override fun onQueryTextChange(
                    newText: String?
                ): Boolean {

                    currentQuery =
                        newText.orEmpty()

                    filterCampgrounds()

                    return true
                }
            }
        )

        updateNetworkBanner()

        // Required offline functionality
        loadCachedCampgrounds()

        // Try getting newest API data
        if (isOnline()) {
            fetchCampgrounds()
        }
    }

    private fun fetchCampgrounds() {

        swipeRefreshLayout.isRefreshing = true

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
                        "Network request failed: $statusCode"
                    )

                    swipeRefreshLayout.isRefreshing = false
                    offlineBanner.visibility = View.VISIBLE

                    loadCachedCampgrounds()
                }

                override fun onSuccess(
                    statusCode: Int,
                    headers: Headers,
                    json: JSON
                ) {

                    try {

                        val parsedJson =
                            createJson()
                                .decodeFromString<CampgroundResponse>(
                                    json.jsonObject.toString()
                                )

                        val networkList =
                            parsedJson.data ?: emptyList()

                        val cachingEnabled =
                            preferences.getBoolean(
                                "cache_enabled",
                                true
                            )

                        if (cachingEnabled) {

                            lifecycleScope.launch(
                                Dispatchers.IO
                            ) {

                                val dao =
                                    AppDatabase
                                        .getDatabase(
                                            applicationContext
                                        )
                                        .campgroundDao()

                                dao.deleteAll()

                                dao.insertAll(
                                    networkList.map {
                                        CampgroundEntity(
                                            it.name,
                                            it.description,
                                            it.latLong,
                                            it.imageUrl
                                        )
                                    }
                                )

                                loadCachedCampgroundsFromIo()
                            }

                        } else {

                            allCampgrounds.clear()
                            allCampgrounds.addAll(networkList)

                            runOnUiThread {
                                filterCampgrounds()
                            }
                        }

                        runOnUiThread {
                            swipeRefreshLayout.isRefreshing =
                                false

                            offlineBanner.visibility =
                                View.GONE
                        }

                    } catch (e: Exception) {

                        Log.e(
                            TAG,
                            "Parsing error",
                            e
                        )

                        swipeRefreshLayout.isRefreshing =
                            false
                    }
                }
            }
        )
    }

    private fun loadCachedCampgrounds() {

        lifecycleScope.launch(
            Dispatchers.IO
        ) {
            loadCachedCampgroundsFromIo()
        }
    }

    private suspend fun loadCachedCampgroundsFromIo() {

        val databaseList =
            AppDatabase
                .getDatabase(applicationContext)
                .campgroundDao()
                .all

        val mappedList =
            databaseList.map { entity ->

                Campground(
                    entity.name,
                    entity.description,
                    entity.latLong,
                    listOf(
                        CampgroundImage(
                            entity.imageUrl,
                            null
                        )
                    )
                )
            }

        withContext(Dispatchers.Main) {

            allCampgrounds.clear()
            allCampgrounds.addAll(mappedList)

            filterCampgrounds()
        }
    }

    private fun filterCampgrounds() {

        val query =
            currentQuery.trim()

        val filtered =
            if (query.isEmpty()) {

                allCampgrounds

            } else {

                allCampgrounds.filter { campground ->

                    campground.name
                        ?.contains(
                            query,
                            ignoreCase = true
                        ) == true ||

                            campground.description
                                ?.contains(
                                    query,
                                    ignoreCase = true
                                ) == true ||

                            campground.latLong
                                ?.contains(
                                    query,
                                    ignoreCase = true
                                ) == true
                }
            }

        displayedCampgrounds.clear()
        displayedCampgrounds.addAll(filtered)

        campgroundAdapter.notifyDataSetChanged()
    }

    private fun isOnline(): Boolean {

        val network =
            connectivityManager.activeNetwork
                ?: return false

        val capabilities =
            connectivityManager
                .getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
    }

    private fun updateNetworkBanner() {

        if (isOnline()) {

            offlineBanner.visibility =
                View.GONE

            wasOffline = false

        } else {

            offlineBanner.visibility =
                View.VISIBLE

            wasOffline = true
        }
    }

    override fun onStart() {
        super.onStart()

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(
            networkRequest,
            networkCallback
        )
    }
    override fun onStop() {
        super.onStop()

        try {
            connectivityManager.unregisterNetworkCallback(
                networkCallback
            )
        } catch (_: Exception) {
        }
    }
}