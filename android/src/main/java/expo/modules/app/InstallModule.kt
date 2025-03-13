package expo.modules.app

import android.content.Context
import android.Manifest
import expo.modules.interfaces.permissions.Permissions
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.Dispatchers
import android.content.pm.PackageManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter

class InstallModule : Module() {
  private val APP_PERMISSION = Manifest.permission.QUERY_ALL_PACKAGES

  override fun definition() = ModuleDefinition {
    Name("ExpoInstall")

    AsyncFunction("getInstalledApps") {
      withContext(Dispatchers.IO) {
        val context = requireContext()
        val packageManager = context.packageManager
        fetchInstalledApps(packageManager)
          try {
            mapOf<String, Any>(
              "appName" to packageManager.getApplicationLabel(app).toString(),
              "packageName" to app.packageName,
              "iconBase64" to drawableToBase64(packageManager.getApplicationIcon(app.packageName)),
              "versionCode" to packageManager.getPackageInfo(app.packageName, 0).longVersionCode,
              "isSystemApp" to (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
              "installTime" to packageManager.getPackageInfo(app.packageName, 0).firstInstallTime
            )
          } catch (e: Exception) {
            mapOf("error" to "${app.packageName}: ${e.message}")
          }
        }
      }
    
    AsyncFunction("requestAppPermissionsAsync") { promise: Promise ->
      Permissions.askForPermissionsWithPermissionsManager(
        permissionsManager,
        promise,
        APP_PERMISSION
      )
    }

    AsyncFunction("getAppPermissionsAsync") { promise: Promise ->
      Permissions.getPermissionsWithPermissionsManager(
        permissionsManager,
        promise,
        APP_PERMISSION
      )
    }

    // 添加应用状态监听
    OnActivityCreate {
      val filter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addAction(Intent.ACTION_PACKAGE_REPLACED)
        addDataScheme("package")
      }
      val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          val apps = getInstalledAppsSync()
          sendEvent("onAppsChange", mapOf(
            "type" to when(intent.action) {
              Intent.ACTION_PACKAGE_ADDED -> "installed"
              Intent.ACTION_PACKAGE_REMOVED -> "uninstalled"
              else -> "updated"
            },
            "apps" to apps
          ))
        }
      }
      it.registerReceiver(receiver, filter)
    }
  }

  private suspend fun fetchInstalledApps(packageManager: PackageManager): List<Map<String, Any>> {
    return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
      .map { app ->
        try {
          mapOf(
            "appName" to packageManager.getApplicationLabel(app).toString(),
            "packageName" to app.packageName,
            "iconBase64" to drawableToBase64(packageManager.getApplicationIcon(app.packageName)),
            "versionCode" to packageManager.getPackageInfo(app.packageName, 0).longVersionCode,
            "isSystemApp" to (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            "installTime" to packageManager.getPackageInfo(app.packageName, 0).firstInstallTime
          )
        } catch (e: Exception) {
          mapOf("error" to "${app.packageName}: ${e.message}")
        }
      }
  }

  private fun getInstalledAppsSync() = withContext(Dispatchers.IO) {
    val context = requireContext()
    fetchInstalledApps(context.packageManager)
  }

  private val permissionsManager: Permissions
    get() = appContext.permissions ?: throw Exceptions.PermissionsModuleNotFound()


  private fun drawableToBase64(drawable: Drawable): String {
    // 复用AppListModule的转换逻辑
    val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return Base64.encodeToString(ByteArrayOutputStream().apply {
      bitmap.compress(Bitmap.CompressFormat.PNG, 85, this)
    }.toByteArray(), Base64.DEFAULT)
  }
}