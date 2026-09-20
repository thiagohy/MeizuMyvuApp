package com.yourapp.myvu

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yourapp.myvu.databinding.ActivityMainBinding
import com.yourapp.myvu.service.MyvuService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var myvuService: MyvuService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MyvuService.LocalBinder
            myvuService = binder.getService()
            isBound = true
            observeConnectionState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            myvuService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupNavigation()
        requestPermissions()
        startAndBindService()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.navigationFragment,
                R.id.assistantFragment,
                R.id.notificationsFragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1001)
        }
    }

    private fun startAndBindService() {
        val serviceIntent = Intent(this, MyvuService::class.java)
        startForegroundService(serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun observeConnectionState() {
        myvuService?.let { service ->
            // Observe connection state using lifecycle
            lifecycleScope.launchWhenStarted {
                service.connectionState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: me.panny777.myvu.core.ConnectionState) {
        when (state) {
            me.panny777.myvu.core.ConnectionState.READY -> {
                Toast.makeText(this, "Óculos conectados!", Toast.LENGTH_SHORT).show()
            }
            me.panny777.myvu.core.ConnectionState.CONNECTING -> {
                Toast.makeText(this, "Conectando...", Toast.LENGTH_SHORT).show()
            }
            me.panny777.myvu.core.ConnectionState.ERROR -> {
                Toast.makeText(this, "Erro na conexão", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    fun getService(): MyvuService? = myvuService

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
