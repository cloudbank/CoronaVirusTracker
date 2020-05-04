package com.droidteahouse.coronaTracker.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import com.droidteahouse.GlideApp
import com.droidteahouse.GlideRequests
import com.droidteahouse.coronaTracker.R
import com.droidteahouse.coronaTracker.ServiceLocator
import com.droidteahouse.coronaTracker.repository.NetworkState
import kotlinx.android.synthetic.main.fragment_coronatracker.*

open class BaseFragment : Fragment() {
    var network: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    lateinit var glide: GlideRequests

    val model: CoronaTrackerViewModel by viewModels {
        object : AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : ViewModel?> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
            ): T {
                val repo = ServiceLocator.instance(context!!)
                        .getRepository()
                @Suppress("UNCHECKED_CAST")
                return CoronaTrackerViewModel(repo) as T
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        glide = GlideApp.with(context!!)
    }


    //@todo this will not work with a VPN connection like reverse tethering to detect if the relay server is cut
    fun checkNetwork(): Boolean {
        var result = false
        val cm = activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            val activeNetworkInfo = cm.getActiveNetworkInfo()
            result = activeNetworkInfo != null && activeNetworkInfo.isConnected
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork
            val capabilities = cm
                    .getNetworkCapabilities(network)
            result = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        network.value = result
        if (result == false) swipe_refresh.isRefreshing = false
        return result
    }

    fun initSwipeToRefresh() {
        swipe_refresh.setColorSchemeResources(
                R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark)
        model.refreshState.observe(this, Observer {
            swipe_refresh.isRefreshing = (it == NetworkState.LOADING)
            checkNetwork()
        })

        swipe_refresh.setOnRefreshListener {
            if (checkNetwork()) {
                model.refresh()
            }
        }
    }


}
