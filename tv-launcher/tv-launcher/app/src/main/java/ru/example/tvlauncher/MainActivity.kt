package ru.example.tvlauncher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var grid: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var dateView: TextView
    private val adapter = AppAdapter(
        onClick = { launch(it) },
        onLongClick = { openAppInfo(it) }
    )

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        grid = findViewById(R.id.apps_grid)
        emptyView = findViewById(R.id.empty_view)
        dateView = findViewById(R.id.date_view)

        grid.layoutManager = GridLayoutManager(this, COLUMNS)
        grid.adapter = adapter
        grid.setHasFixedSize(true)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        updateDate()
        grid.requestFocus()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Нажатие кнопки «Домой»: возвращаемся к началу списка
        grid.scrollToPosition(0)
        grid.requestFocus()
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    // Лаунчер не должен закрываться кнопкой «Назад»
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        grid.scrollToPosition(0)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Кнопка меню на пульте — настройки системы
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun updateDate() {
        val fmt = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        val text = fmt.format(Date())
        dateView.text = text.replaceFirstChar { it.uppercase() }
    }

    private fun loadApps() {
        val pm = packageManager
        val seen = HashSet<String>()
        val result = ArrayList<AppEntry>()

        // Сначала приложения для ТВ, затем обычные (на случай APK без leanback-значка)
        val categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        for (category in categories) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in pm.queryIntentActivities(intent, 0)) {
                val pkg = info.activityInfo.packageName
                if (pkg == packageName) continue
                if (!seen.add(pkg)) continue
                result.add(
                    AppEntry(
                        label = info.loadLabel(pm).toString(),
                        packageName = pkg,
                        component = android.content.ComponentName(pkg, info.activityInfo.name),
                        icon = info.loadIcon(pm)
                    )
                )
            }
        }

        result.sortBy { it.label.lowercase() }
        adapter.submit(result)
        emptyView.visibility = if (result.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun launch(app: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = app.component
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.cant_launch, app.label), Toast.LENGTH_SHORT).show()
        }
    }

    // Долгое нажатие OK — сведения о приложении (там можно удалить или остановить)
    private fun openAppInfo(app: AppEntry) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.no_app_info, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val COLUMNS = 6
    }
}
