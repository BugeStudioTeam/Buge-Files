package com.buge.files.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.buge.files.R
import com.buge.files.databinding.FragmentSettingsBinding
import com.buge.files.util.PrefsManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: PrefsManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsManager(requireContext())

        // Theme
        binding.settingTheme.setOnClickListener { showThemeDialog() }
        updateThemeLabel()

        // Show hidden files
        binding.switchHidden.isChecked = prefs.showHiddenFiles
        binding.switchHidden.setOnCheckedChangeListener { _, checked ->
            prefs.showHiddenFiles = checked
        }

        // Version
        val versionName = try {
            requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0).versionName ?: "1.7.0"
        } catch (e: Exception) { "1.7.0" }
        binding.settingVersion.text = versionName
    }

    private fun showThemeDialog() {
        val options = arrayOf(
            getString(R.string.setting_theme_light),
            getString(R.string.setting_theme_dark),
            getString(R.string.setting_theme_system)
        )
        val modes = intArrayOf(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        val current = modes.indexOf(prefs.themeMode).let { if (it < 0) 2 else it }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.setting_theme)
            .setSingleChoiceItems(options, current) { dialog, which ->
                prefs.themeMode = modes[which]
                AppCompatDelegate.setDefaultNightMode(modes[which])
                updateThemeLabel()
                dialog.dismiss()
            }
            .show()
    }

    private fun updateThemeLabel() {
        binding.settingThemeValue.text = when (prefs.themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.setting_theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.setting_theme_dark)
            else -> getString(R.string.setting_theme_system)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}