import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netflow_traffic_stats/netflow_traffic_stats_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelNetflowTrafficStats platform =
      MethodChannelNetflowTrafficStats();
  const MethodChannel channel = MethodChannel('com.netflow.app/traffic_stats');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
          return <String, int>{
            'totalRx': 1000,
            'totalTx': 2000,
            'mobileRx': 300,
            'mobileTx': 400,
            'wifiRx': 700,
            'wifiTx': 1600,
          };
        });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('getTrafficStats', () async {
    final stats = await platform.getTrafficStats();
    expect(stats['totalRx'], 1000);
    expect(stats['wifiTx'], 1600);
  });
}
