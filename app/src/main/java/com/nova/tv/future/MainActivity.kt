package com.nova.tv.future

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

class MainActivity : AppCompatActivity() {

    private lateinit var tvClock: TextView
    private lateinit var tvRam: TextView
    private lateinit var tvNetworkSpeed: TextView
    private lateinit var tvVpn: TextView
    private lateinit var btnBoost: Button
    private lateinit var btnLaunch: Button
    private lateinit var btnThemes: Button
    private lateinit var heroTitle: TextView
    private lateinit var heroSubtitle: TextView
    private lateinit var heroAppIcon: ImageView
    private lateinit var ambientGlow: View
    private lateinit var rvApps: RecyclerView

    private val handler = Handler(Looper.getMainLooper())
    private var lastRxBytes: Long = 0
    private var selectedApp: AppInfo? = null
    private var currentThemeIndex = 0

    private val themes = listOf(
        Triple("CYBERPUNK", intArrayOf(Color.parseColor("#0A0918"), Color.parseColor("#131032"), Color.parseColor("#071824")), "#00F0FF"),
        Triple("JARVIS GOLD", intArrayOf(Color.parseColor("#140D05"), Color.parseColor("#2B1A08"), Color.parseColor("#0F0A04")), "#FFB700"),
        Triple("MATRIX GREEN", intArrayOf(Color.parseColor("#021206"), Color.parseColor("#06260E"), Color.parseColor("#010A03")), "#00FF66"),
        Triple("AMOLED VOID", intArrayOf(Color.parseColor("#000000"), Color.parseColor("#08080C"), Color.parseColor("#000000")), "#B84DFF")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvClock = findViewById(R.id.tv_clock)
        tvRam = findViewById(R.id.tv_ram)
        tvNetworkSpeed = findViewById(R.id.tv_network_speed)
        tvVpn = findViewById(R.id.tv_vpn)
        btnBoost = findViewById(R.id.btn_boost)
        btnLaunch = findViewById(R.id.btn_launch)
        btnThemes = findViewById(R.id.btn_themes)
        heroTitle = findViewById(R.id.hero_title)
        heroSubtitle = findViewById(R.id.hero_subtitle)
        heroAppIcon = findViewById(R.id.hero_app_icon)
        ambientGlow = findViewById(R.id.ambient_glow)
        rvApps = findViewById(R.id.rv_apps)

        lastRxBytes = TrafficStats.getTotalRxBytes()

        setupAppsList()
        setupButtons()
        startHudUpdater()
    }

    private fun setupAppsList() {
        val pm = packageManager
        val appsList = mutableListOf<AppInfo>()

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val leanbackIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }

        val resolvedApps = (pm.queryIntentActivities(leanbackIntent, 0) + pm.queryIntentActivities(mainIntent, 0))
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != packageName }

        for (info in resolvedApps) {
            val label = info.loadLabel(pm).toString()
            val pkg = info.activityInfo.packageName
            val icon = info.loadIcon(pm)
            appsList.add(AppInfo(label, pkg, icon))
        }

        appsList.sortBy { it.label.lowercase() }

        rvApps.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvApps.adapter = AppsAdapter(appsList) { app ->
            selectedApp = app
            heroTitle.text = app.label.uppercase()
            heroSubtitle.text = "Package: ${app.packageName} • Ready to Launch"
            heroAppIcon.setImageDrawable(app.icon)
        }

        if (appsList.isNotEmpty()) {
            selectedApp = appsList[0]
            heroTitle.text = appsList[0].label.uppercase()
            heroSubtitle.text = "Ready to Launch • Press OK"
            heroAppIcon.setImageDrawable(appsList[0].icon)
        }
    }

    private fun setupButtons() {
        btnLaunch.setOnClickListener {
            selectedApp?.let { launchApp(it.packageName) }
        }

        btnBoost.setOnClickListener {
            boostRam()
        }

        btnThemes.setOnClickListener {
            currentThemeIndex = (currentThemeIndex + 1) % themes.size
            val (name, colors, accent) = themes[currentThemeIndex]
            val gradient = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)
            ambientGlow.background = gradient
            btnThemes.text = "🎨 THEME: $name"
            btnThemes.setTextColor(Color.parseColor(accent))
            Toast.makeText(this, "Theme Switched: $name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchApp(pkg: String) {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg)
            ?: packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Cannot launch this app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun boostRam() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = am.runningAppProcesses
        var count = 0
        runningApps?.forEach { process ->
            if (process.pkgList != null) {
                for (pkg in process.pkgList) {
                    if (pkg != packageName) {
                        am.killBackgroundProcesses(pkg)
                        count++
                    }
                }
            }
        }
        updateRamStatus()
        Toast.makeText(this, "⚡ RAM Boosted! Background tasks cleared.", Toast.LENGTH_SHORT).show()
    }

    private fun startHudUpdater() {
        val runnable = object : Runnable {
            override fun run() {
                // Clock
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                tvClock.text = sdf.format(Date())

                // RAM
                updateRamStatus()

                // Network Speed
                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val speedBytes = (currentRxBytes - lastRxBytes).coerceAtLeast(0)
                lastRxBytes = currentRxBytes
                val speedText = when {
                    speedBytes > 1024 * 1024 -> String.format("%.1f MB/s", speedBytes / (1024.0 * 1024.0))
                    speedBytes > 1024 -> "${speedBytes / 1024} KB/s"
                    else -> "$speedBytes B/s"
                }
                tvNetworkSpeed.text = "🌐 $speedText"

                // VPN Check
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                val isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
                if (isVpn) {
                    tvVpn.text = "🛡️ VPN: ON"
                    tvVpn.setTextColor(Color.parseColor("#00FF66"))
                } else {
                    tvVpn.text = "🛡️ VPN: OFF"
                    tvVpn.setTextColor(Color.parseColor("#888888"))
                }

                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun updateRamStatus() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val usedPercent = (((memInfo.totalMem - memInfo.availMem).toDouble() / memInfo.totalMem) * 100).toInt()
        tvRam.text = "🧠 RAM: $usedPercent%"
    }

    override fun onBackPressed() {
        // Disable back button on Home Screen
    }

    inner class AppsAdapter(
        private val apps: List<AppInfo>,
        private val onAppFocused: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

        inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.iv_app_icon)
            val name: TextView = view.findViewById(R.id.tv_app_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            return AppViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val app = apps[position]
            holder.name.text = app.label
            holder.icon.setImageDrawable(app.icon)

            holder.itemView.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.14f).scaleY(1.14f).setDuration(150).start()
                    onAppFocused(app)
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                }
            }

            holder.itemView.setOnClickListener {
                launchApp(app.packageName)
            }
        }

        override fun getItemCount(): Int = apps.size
    }
}
