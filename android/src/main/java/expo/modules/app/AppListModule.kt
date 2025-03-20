package expo.modules.app

import android.Manifest
import android.content.Context
import android.net.sip.SipManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import expo.modules.interfaces.permissions.Permissions
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.Dispatchers
import android.content.pm.PackageManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter

const val MODULE_NAME = "ExpoInstall"

class AppListModule : Module() {
  private val APP_PERMISSION = Manifest.permission.QUERY_ALL_PACKAGES

  override fun definition() = ModuleDefinition {
    Name(MODULE_NAME)
    Constants {
      val telephonyManager = telephonyManager()
      mapOf(
        "MODULE_NAME" to MODULE_NAME,
        "API_VERSION" to 1,
        "ANDROID_VERSION" to Build.VERSION.SDK_INT,
        "MAX_API_LEVEL" to Build.VERSION_CODES.S_V2,
        "PERMISSION_NAME" to APP_PERMISSION,
        "MODULE_DESCRIPTION" to "提供设备安装应用查询功能",
        "BUILD_TIME" to BuildConfig.BUILD_TIME,
        "allowsVoip" to SipManager.isVoipSupported(context),
        "isoCountryCode" to telephonyManager?.simCountryIso,
        "carrier" to telephonyManager?.simOperatorName,
        "mobileCountryCode" to telephonyManager?.simOperator?.substring(0, 3),
        "mobileNetworkCode" to telephonyManager?.simOperator?.substring(3)
      )
    }

    AsyncFunction<Int>("getCellularGenerationAsync") {
      try {
        getCurrentGeneration()
      } catch (e: SecurityException) {
        Log.w(moduleName, "READ_PHONE_STATE permission is required to acquire network type", e)
        AppListGeneration.UNKNOWN.value
      }
    }

    AsyncFunction<Boolean>("allowsVoipAsync") {
      SipManager.isVoipSupported(context)
    }

    AsyncFunction<String?>("getIsoCountryCodeAsync") {
      telephonyManager()?.simCountryIso
    }

    AsyncFunction<String?>("getCarrierNameAsync") {
      telephonyManager()?.simOperatorName
    }

    AsyncFunction<String?>("getMobileCountryCodeAsync") {
      telephonyManager()?.simOperator?.substring(0, 3)
    }

    AsyncFunction<String?>("getMobileNetworkCodeAsync") {
      telephonyManager()?.simOperator?.substring(3)
    }

    AsyncFunction("requestPermissionsAsync") { promise: Promise ->
      Permissions.askForPermissionsWithPermissionsManager(
        permissionsManager,
        promise,
        Manifest.permission.READ_PHONE_STATE
      )
    }

    AsyncFunction("getPermissionsAsync") { promise: Promise ->
      Permissions.getPermissionsWithPermissionsManager(
        permissionsManager,
        promise,
        Manifest.permission.READ_PHONE_STATE
      )
    }

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

  private fun telephonyManager() =
    (context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager).takeIf {
      it?.simState == TelephonyManager.SIM_STATE_READY
    }

  private val context
    get() = requireNotNull(appContext.reactContext)

  private val permissionsManager: Permissions
    get() = appContext.permissions ?: throw Exceptions.PermissionsModuleNotFound()

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

  @SuppressLint("MissingPermission")
  private fun getCurrentGeneration(): Int {
    val telephonyManager = telephonyManager()
      ?: return AppListGeneration.UNKNOWN.value
    val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      telephonyManager.dataNetworkType
    } else {
      @Suppress("DEPRECATION")
      telephonyManager.networkType
    }
    return when (networkType) {
      TelephonyManager.NETWORK_TYPE_GPRS,
      TelephonyManager.NETWORK_TYPE_EDGE,
      TelephonyManager.NETWORK_TYPE_CDMA,
      TelephonyManager.NETWORK_TYPE_1xRTT,
      TelephonyManager.NETWORK_TYPE_IDEN -> {
        AppListGeneration.CG_2G.value
      }
      TelephonyManager.NETWORK_TYPE_UMTS,
      TelephonyManager.NETWORK_TYPE_EVDO_0,
      TelephonyManager.NETWORK_TYPE_EVDO_A,
      TelephonyManager.NETWORK_TYPE_HSDPA,
      TelephonyManager.NETWORK_TYPE_HSUPA,
      TelephonyManager.NETWORK_TYPE_HSPA,
      TelephonyManager.NETWORK_TYPE_EVDO_B,
      TelephonyManager.NETWORK_TYPE_EHRPD,
      TelephonyManager.NETWORK_TYPE_HSPAP -> {
        AppListGeneration.CG_3G.value
      }
      TelephonyManager.NETWORK_TYPE_LTE -> {
        AppListGeneration.CG_4G.value
      }
      TelephonyManager.NETWORK_TYPE_NR -> {
        AppListGeneration.CG_5G.value
      }
      else -> {
        AppListGeneration.UNKNOWN.value
      }
    }
  }
}
