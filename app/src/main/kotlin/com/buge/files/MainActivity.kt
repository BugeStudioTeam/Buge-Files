package com.buge.files

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.buge.files.databinding.ActivityMainBinding
import com.buge.files.util.PrefsManager
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PrefsManager(this)
        AppCompatDelegate.setDefaultNightMode(prefs.themeMode)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_files,
                R.id.nav_recent,
                R.id.nav_favorites,
                R.id.nav_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // FAB 点击 → 直接用接口通知当前 Fragment
        binding.appBarMain.fab.setOnClickListener {
            val navHost = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_content_main)
            val currentFragment = navHost?.childFragmentManager?.fragments?.firstOrNull()
            if (currentFragment is FabClickListener) {
                currentFragment.onFabClick()
            }
        }

        // 只在 Files 页面显示 FAB
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.nav_files) {
                binding.appBarMain.fab.show()
            } else {
                binding.appBarMain.fab.hide()
            }
        }
    }

    // MainActivity 不inflate任何menu，菜单完全由各Fragment控制
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                val newMode = when (prefs.themeMode) {
                    AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_YES
                    AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    else -> AppCompatDelegate.MODE_NIGHT_NO
                }
                prefs.themeMode = newMode
                AppCompatDelegate.setDefaultNightMode(newMode)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}

// 接口：让 Fragment 接收 FAB 点击
interface FabClickListener {
    fun onFabClick()
}