/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.android.trackmysleepquality.R
import com.example.android.trackmysleepquality.database.SleepDatabase
import com.example.android.trackmysleepquality.databinding.FragmentSleepTrackerBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * A fragment with buttons to record start and end times for sleep, which are saved in
 * a database. Cumulative data is displayed in a simple scrollable TextView.
 * (Because we have not learned about RecyclerView yet.)
 */
class SleepTrackerFragment : Fragment() {

    /**
     * Called when the Fragment is ready to display content to the screen.
     *
     * This function uses DataBindingUtil to inflate R.layout.fragment_sleep_quality.
     */
    // @InternalCoroutinesApi reporting use case to kotlinx
    @InternalCoroutinesApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        val application = requireNotNull(this.activity).application
        val dao = SleepDatabase.getInstance(application).sleepDatabaseDao
        val viewModelFactory = SleepTrackerViewModelFactory(dao, application)
        val viewModel = ViewModelProvider(this, viewModelFactory)
                .get(SleepTrackerViewModel::class.java)
        // Get a reference to the binding object and inflate the fragment views.
        val binding: FragmentSleepTrackerBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_sleep_tracker, container, false)

        val adapter = SleepNightAdapter(SleepNightAdapter.SleepNightListener {
            nightId ->
            viewModel.onSleepNightClicked(nightId)
        })
        binding.sleepList.adapter = adapter
        val manager = GridLayoutManager(activity, 3)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup(){
            override fun getSpanSize(position: Int): Int {
                return when (position){
                    0 -> 3
                    else -> 1
                }
            }

        }
        binding.sleepList.layoutManager = manager

        viewModel.nights.observe(viewLifecycleOwner){
            it?.let{
                adapter.addHeaderAndSubmitList(it)
            }
        }

        binding.lifecycleOwner = this
        binding.sleepTrackerViewModel = viewModel

        viewModel.navigationToSleepQuality.observe(viewLifecycleOwner){
            night ->
            night?.let {
                findNavController().navigate(
                        SleepTrackerFragmentDirections.
                        actionSleepTrackerFragmentToSleepQualityFragment(it.nightId)
                )
                viewModel.doneNavigating()
            }
        }

        viewModel.navigateToSleepDataQuality.observe(viewLifecycleOwner){
            it?.let{
                findNavController().navigate(SleepTrackerFragmentDirections
                        .actionSleepTrackerFragmentToSleepDetailFragment(it))
                viewModel.onSleepDataQualityNavigated()
            }
        }
        viewModel.showSnackbarEvent.observe(viewLifecycleOwner){
            if (it){
                Snackbar.make(
                        requireActivity().findViewById(android.R.id.content),
                        getString(R.string.cleared_message),
                        Snackbar.LENGTH_SHORT
                ).show()
                viewModel.doneShowingSnackbar()
            }
        }
        return binding.root
    }
}
