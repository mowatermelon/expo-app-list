import { AppInfo } from "./AppList";
import { AppListGeneration, PermissionResponse } from "./AppList.types";

export default {
  get allowsVoip(): null {
    return null;
  },
  get carrier(): null {
    return null;
  },
  get isoCountryCode(): null {
    return null;
  },
  get mobileCountryCode(): null {
    return null;
  },
  get mobileNetworkCode(): null {
    return null;
  },
  /**
   * Web平台实现：使用navigator.connection检测网络类型
   * @platform web
   * @returns 根据effectiveType返回对应的蜂窝网络代际
   */
  async getCellularGenerationAsync(): Promise<AppListGeneration> {
    const connection =
      // @ts-expect-error
      navigator.connection ||
      navigator.mozConnection ||
      navigator.webkitConnection ||
      null;
    if (connection !== null) {
      switch (connection.effectiveType) {
        case "slow-2g":
        case "2g":
          return AppListGeneration.CELLULAR_2G;
        case "3g":
          return AppListGeneration.CELLULAR_3G;
        case "4g":
          return AppListGeneration.CELLULAR_4G;
        default:
          return AppListGeneration.UNKNOWN;
      }
    } else {
      return AppListGeneration.UNKNOWN;
    }
  },

  /**
   * Web平台不支持VoIP检测
   * @platform web
   * @returns 固定返回null
   */
  async allowsVoipAsync(): Promise<boolean | null> {
    return null;
  },
  async getIsoCountryCodeAsync(): Promise<string | null> {
    return null;
  },
  /**
   * Web平台不支持运营商信息获取
   * @platform web
   * @returns 固定返回null
   */
  async getCarrierNameAsync(): Promise<string | null> {
    return null;
  },
  async getMobileCountryCodeAsync(): Promise<string | null> {
    return null;
  },
  async getMobileNetworkCodeAsync(): Promise<string | null> {
    return null;
  },

  async getAppPermissionsAsync(): Promise<PermissionResponse | null> {
    return null;
  },

  async requestAppPermissionsAsync(): Promise<PermissionResponse | null> {
    return null;
  },

  async getInstalledApps(): Promise<AppInfo[] | null> {
    return null;
  },
};
