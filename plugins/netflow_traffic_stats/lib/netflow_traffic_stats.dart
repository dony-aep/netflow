import 'netflow_traffic_stats_platform_interface.dart';

class NetflowTrafficStats {
  Future<String?> getPlatformVersion() {
    return NetflowTrafficStatsPlatform.instance.getPlatformVersion();
  }

  static Future<Map<String, int>> getTrafficStats() {
    return NetflowTrafficStatsPlatform.instance.getTrafficStats();
  }
}
