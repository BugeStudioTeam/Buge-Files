package com.buge.files.ui.recent

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.buge.files.databinding.FragmentRecentBinding

class RecentFragment : Fragment() {

    private var _binding: FragmentRecentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = ViewModelProvider(this)[RecentViewModel::class.java]
        _binding = FragmentRecentBinding.inflate(inflater, container, false)

        vm.recentFiles.observe(viewLifecycleOwner) { files ->
            binding.emptyView.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
            // Adapter binding handled here (reuse FileAdapter)
        }

        vm.load(Environment.getExternalStorageDirectory())
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}