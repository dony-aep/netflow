import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'netflow_traffic_stats_platform_interface.dart';

/// An implementation of [NetflowTrafficStatsPlatform] that uses method channels.
class MethodChannelNetflowTrafficStats extends NetflowTrafficStatsPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('com.netflow.app/traffic_stats');

  @override
  Future<String?> getPlatformVersion() async {
    return methodChannel.invokeMethod<String>('getPlatformVersion');
  }

  @override
  Future<Map<String, int>> getTrafficStats() async {
    final rawStats = await methodChannel.invokeMethod<Map<dynamic, dynamic>>(
      'getTrafficStats',
    );

    if (rawStats == null) {
      return const {
        'totalRx': 0,
        'totalTx': 0,
        'mobileRx': 0,
        'mobileTx': 0,
        'wifiRx': 0,
        'wifiTx': 0,
      };
    }

    int toIntValue(String key) => (rawStats[key] as num?)?.toInt() ?? 0;

    return {
      'totalRx': toIntValue('totalRx'),
      'totalTx': toIntValue('totalTx'),
      'mobileRx': toIntValue('mobileRx'),
      'mobileTx': toIntValue('mobileTx'),
      'wifiRx': toIntValue('wifiRx'),
      'wifiTx': toIntValue('wifiTx'),
    };
  }
}
