import {
  PermissionResponse,
  createPermissionHook,
  PermissionStatus,
} from "expo-modules-core";

import ExpoInstall from "./ExpoCellular";

export {
  PermissionResponse,
  PermissionStatus,
  PermissionHookOptions,
  PermissionExpiration,
} from "expo-modules-core";

export type AppInfo = {
  appName: string;
  packageName: string;
  iconBase64: string;
  versionCode: number;
  isSystemApp: boolean;
  installTime: number;
};

export enum AppChangeType {
  INSTALLED = "installed",
  UPDATED = "updated",
  UNINSTALLED = "uninstalled",
}

export type AppsChangeEvent = {
  type: AppChangeType;
  apps: AppInfo[];
};

export function addAppsChangeListener(
  listener: (event: AppsChangeEvent) => void,
) {
  return ExpoInstall.addListener("onAppsChange", listener);
}

export async function requestAppPermissionsAsync(): Promise<PermissionStatus> {
  return await ExpoInstall.requestAppPermissionsAsync();
}

export async function getInstalledApps(): Promise<AppInfo[]> {
  return await ExpoInstall.getInstalledApps();
}

/**
 * 获取当前安装权限状态
 * @platform android
 */
export async function getAppPermissionsAsync(): Promise<PermissionResponse> {
  const status = await requestAppPermissionsAsync();
  return {
    status,
    granted: status === PermissionStatus.GRANTED,
    expires: "never",
    canAskAgain: status !== PermissionStatus.DENIED,
  } as PermissionResponse;
}

/**
 * 自定义Hook集成安装权限与应用信息功能
 * @example
 * const [status, requestInstallAppPermission] = useInstallAppInfo();
 *
 * @remarks
 * Android需要 QUERY_ALL_PACKAGES 权限，iOS 无需额外权限
 */
export const useInstallAppInfo = createPermissionHook({
  getMethod: getAppPermissionsAsync,
  requestMethod: async () => {
    const status = await requestAppPermissionsAsync();
    return {
      status,
      granted: status === PermissionStatus.GRANTED,
      expires: "never",
      canAskAgain: status !== PermissionStatus.DENIED,
    } as PermissionResponse;
  },
});

// Android特有方法
/**
 * Android平台权限明细
 * @enum {string}
 * @platform android
 */
export enum AndroidPermissionDetail {
  QUERY_ALL_PACKAGES = "android.permission.QUERY_ALL_PACKAGES",
}

// iOS类型安全映射
declare module "./ExpoCellular" {
  export function addAppsChangeListener(): void;
}
