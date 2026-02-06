import 'package:flutter_test/flutter_test.dart';
import 'package:netflow_traffic_stats/netflow_traffic_stats.dart';
import 'package:netflow_traffic_stats/netflow_traffic_stats_platform_interface.dart';
import 'package:netflow_traffic_stats/netflow_traffic_stats_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockNetflowTrafficStatsPlatform
    with MockPlatformInterfaceMixin
    implements NetflowTrafficStatsPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<Map<String, int>> getTrafficStats() => Future.value(const {
    'totalRx': 10,
    'totalTx': 20,
    'mobileRx': 3,
    'mobileTx': 4,
    'wifiRx': 7,
    'wifiTx': 16,
  });
}

void main() {
  final NetflowTrafficStatsPlatform initialPlatform =
      NetflowTrafficStatsPlatform.instance;

  test('$MethodChannelNetflowTrafficStats is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelNetflowTrafficStats>());
  });

  test('getTrafficStats', () async {
    MockNetflowTrafficStatsPlatform fakePlatform =
        MockNetflowTrafficStatsPlatform();
    NetflowTrafficStatsPlatform.instance = fakePlatform;

    final stats = await NetflowTrafficStats.getTrafficStats();
    expect(stats['totalRx'], 10);
  });
}
