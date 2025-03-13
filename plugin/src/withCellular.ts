import {
  AndroidConfig,
  ConfigPlugin,
  createRunOncePlugin,
} from "expo/config-plugins";

const pkg = require("expo-install-apps/package.json");

const withCellular: ConfigPlugin = (config) => {
  config = AndroidConfig.Permissions.withPermissions(config, [
    "android.permission.READ_PHONE_STATE",
    "android.permission.QUERY_ALL_PACKAGES",
  ]);
  return config;
};

export default createRunOncePlugin(withCellular, pkg.name, pkg.version);
