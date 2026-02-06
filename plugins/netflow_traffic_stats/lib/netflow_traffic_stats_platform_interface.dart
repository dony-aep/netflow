import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'netflow_traffic_stats_method_channel.dart';

abstract class NetflowTrafficStatsPlatform extends PlatformInterface {
  /// Constructs a NetflowTrafficStatsPlatform.
  NetflowTrafficStatsPlatform() : super(token: _token);

  static final Object _token = Object();

  static NetflowTrafficStatsPlatform _instance =
      MethodChannelNetflowTrafficStats();

  /// The default instance of [NetflowTrafficStatsPlatform] to use.
  ///
  /// Defaults to [MethodChannelNetflowTrafficStats].
  static NetflowTrafficStatsPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [NetflowTrafficStatsPlatform] when
  /// they register themselves.
  static set instance(NetflowTrafficStatsPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('getPlatformVersion() has not been implemented.');
  }

  Future<Map<String, int>> getTrafficStats() {
    throw UnimplementedError('getTrafficStats() has not been implemented.');
  }
}
