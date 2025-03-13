# expo-install-apps

提供获取设备安装应用列表和安装权限管理的Expo模块

github 地址: https://github.com/mowatermelon/expo-app-list

## 安装

```bash
npx expo install expo-install-apps
```

## 配置

在app.json中添加插件配置：

```json
{
  "plugins": [
    [
      "expo-install-apps",
      {
        "androidPermissionDescription": "Allow $(PRODUCT_NAME) to install packages",
        "iosSchemes": ["itms-apps"]
      }
    ]
  ]
}
```

## API参考

### `requestPermissionsAsync()`

请求安装未知来源应用的权限

**返回 Promise<PermissionStatus>**

### `getInstalledApps()`

获取已安装应用列表

**返回 Promise<AppInfo[]>**

### `addAppsChangeListener(listener)`

监听应用安装/更新/卸载事件

**参数:**

- listener: (event: AppsChangeEvent) => void

**返回 Subscription**

### `useInstallAppInfo()`

集成权限管理的React Hook

**返回** [PermissionResponse, requestPermission: () => Promise<void>]

## 类型定义

```ts
type AppInfo = {
  appName: string;
  packageName: string;
  iconBase64: string;
  versionCode: number;
  isSystemApp: boolean;
  installTime: number;
};

enum AppChangeType {
  INSTALLED = "installed",
  UPDATED = "updated",
  UNINSTALLED = "uninstalled",
}

type AppsChangeEvent = {
  type: AppChangeType;
  apps: AppInfo[];
};

interface PermissionResponse {
  status: PermissionStatus;
  granted: boolean;
  canAskAgain: boolean;
  expires: "never";
}
```

## 平台要求

| 平台    | 版本要求 | 权限要求                     |
| ------- | -------- | ---------------------------- |
| Android | 5.0+     | 需要`QUERY_ALL_PACKAGES`权限 |
| iOS     | 11.0+    | 无需额外权限                 |

## 高级配置

### Android

自动添加以下权限：

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

### iOS

自动配置应用白名单：

```xml
<key>LSApplicationQueriesSchemes</key>
<array>
  <string>itms-apps</string>
  <string>itms-beta</string>
</array>
```

## 使用流程

### 1. 权限检查与请求

```tsx
import ExpoInstall, { PermissionStatus } from "expo-install-apps";

useEffect(() => {
  const checkPermission = async () => {
    const { status } = await ExpoInstall.requestPermissionsAsync();
    if (status !== PermissionStatus.GRANTED) {
      console.log("需要手动开启安装权限");
      // Android需要跳转到设置页面
      if (Platform.OS === "android") {
        Linking.openSettings();
      }
    }
  };
  checkPermission();
}, []);
```

### 2. 初始应用列表加载

```tsx
import ExpoInstall from "expo-install-apps";

const loadInstalledApps = async () => {
  try {
    const apps = await ExpoInstall.getInstalledApps();
    setApps(apps);
  } catch (error) {
    console.error("获取应用列表失败:", error);
  }
};

// 在权限授予后调用
useEffect(() => {
  if (permission.granted) {
    loadInstalledApps();
  }
}, [permission.granted]);
```

### 3. 实时监听应用变更

```tsx
import ExpoInstall, type { AppsChangeEvent } from "expo-install-apps";

useEffect(() => {
  const subscription = ExpoInstall.addAppsChangeListener((event) => {
    switch (event.type) {
      case AppChangeType.INSTALLED:
        setApps((prev) => [...prev, ...event.apps]);
        break;
      case AppChangeType.UPDATED:
        // 处理应用更新逻辑
        break;
      case AppChangeType.UNINSTALLED:
        // 处理应用卸载逻辑
        break;
    }
  });

  return () => subscription.remove();
}, []);
```

## 平台差异说明

- **Android**: 需要动态请求`REQUEST_INSTALL_PACKAGES`权限，未授权时需引导用户到系统设置
- **iOS**: 仅支持监听应用安装/更新事件，无法获取完整应用列表

## 示例

```tsx
import ExpoInstall, {
  AppInfo,
  AppsChangeEvent,
  useInstallAppInfo,
  AppChangeType,
} from "expo-install-apps";

function AppList() {
  const [apps, setApps] = useState<AppInfo[]>([]);
  const [permission] = useInstallAppInfo();

  useEffect(() => {
    const subscription = ExpoInstall.addAppsChangeListener((event) => {
      if (event.type === "installed") {
        setApps((prev) => [...prev, ...event.apps]);
      }
    });

    return () => subscription.remove();
  }, []);

  if (!permission.granted) {
    return <Text>需要开启安装权限</Text>;
  }

  return (
    <FlatList
      data={apps}
      renderItem={({ item }) => (
        <View style={{ padding: 10 }}>
          <Image
            source={{ uri: `data:image/png;base64,${item.iconBase64}` }}
            style={{ width: 40, height: 40 }}
          />
          <Text>
            {item.appName} ({item.packageName})
          </Text>
        </View>
      )}
    />
  );
}
```

## 贡献

欢迎通过 GitHub 提交 issue 或 pull request
