package com.lpalminteri.homelauncher

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // Use WindowInsetsController instead of deprecated setSystemUiVisibility
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val apps = getFilteredApps()
        adapter = AppListAdapter(apps) { app ->
            launchApp(app.packageName)
        }
        recyclerView.adapter = adapter
    }

    private fun getFilteredApps(): List<AppInfo> {
        val pm = packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return installedApps
            .filter { appInfo ->
                (!isSystemApp(appInfo) || isImportantSystemApp(appInfo) || isPlayStoreApp(appInfo)) &&
                        pm.getLaunchIntentForPackage(appInfo.packageName) != null
            }
            .map { appInfo ->
                AppInfo(
                    appInfo.loadLabel(pm).toString(),
                    appInfo.packageName
                )
            }
            .sortedBy { it.name }
    }

    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    private fun isImportantSystemApp(appInfo: ApplicationInfo): Boolean {
        val importantPackages = listOf(
            "com.android.vending",
            "com.android.settings",
            "com.android.dialer",
            "com.android.contacts",
            "com.android.camera",
            "com.google.android.apps.maps",
            "com.google.android.gm",
            "com.google.android.apps.photos",
            "com.google.android.youtube",
            "com.google.android.calendar",
        )
        return importantPackages.contains(appInfo.packageName)
    }

    private fun isPlayStoreApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0)
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
}

data class AppInfo(val name: String, val packageName: String)

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val onItemClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.appName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.textView.text = app.name
        holder.itemView.setOnClickListener { onItemClick(app) }
    }

    override fun getItemCount() = apps.size
}