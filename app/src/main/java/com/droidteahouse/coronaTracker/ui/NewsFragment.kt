package com.droidteahouse.coronaTracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.droidteahouse.coronaTracker.R
import com.droidteahouse.coronaTracker.vo.Area
import kotlinx.android.synthetic.main.fragment_coronatracker.*


/**
 *
 */
class NewsFragment : BaseFragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_coronatracker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title.text = resources.getString(R.string.daily_news)
        initSwipeToRefresh()
        checkNetwork()
        network.observe(viewLifecycleOwner, Observer {
            no_network.visibility = if (it == true) View.GONE else View.VISIBLE
            no_network.invalidate()
        })

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initAdapter()
    }


    private fun initAdapter() {
        mapview?.visibility = View.GONE
        val adapter = AreaAdapter(glide) {
            if (checkNetwork()) {
                model.retry()
            }
        }
        list.adapter = adapter
        val horizontalDecoration = DividerItemDecoration(context!!,
                DividerItemDecoration.VERTICAL)
        val horizontalDivider = ContextCompat.getDrawable(context!!, R.drawable.list_divider)
        horizontalDecoration.setDrawable(horizontalDivider!!)
        list.addItemDecoration(horizontalDecoration)

        list.addItemDecoration(DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL))
        list.setHasFixedSize(true);
        list.setItemAnimator(DefaultItemAnimator())



        model.resultList.observe(viewLifecycleOwner, Observer<PagedList<Area>> {
            adapter.submitList(it) {
                // Workaround for an issue where RecyclerView incorrectly uses the loading / spinner
                // item added to the end of the list as an anchor during initial load.
                val layoutManager = GridLayoutManager(activity, 5, GridLayoutManager.VERTICAL, false)
                val position = layoutManager.findFirstCompletelyVisibleItemPosition()
                list.layoutManager = layoutManager
                if (position != RecyclerView.NO_POSITION) {
                    list.scrollToPosition(position)
                }
            }
        })
        model.networkState.observe(viewLifecycleOwner, Observer {
            adapter.setNetworkState(it)
        })


    }

}


