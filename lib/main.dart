import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'theme/app_theme.dart';
import 'screens/home_screen.dart';
import 'services/network_monitor_service.dart';
import 'providers/providers.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Inicializar datos de localización para DateFormat
  await initializeDateFormatting('es_ES', null);
  
  runApp(const NetFlowApp());
}

/// Aplicación principal de NetFlow
/// Monitorea el uso de datos móviles y WiFi en tiempo real
class NetFlowApp extends StatelessWidget {
  const NetFlowApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(
          create: (_) => NetworkMonitorProvider()..init(),
        ),
        ChangeNotifierProvider(
          create: (_) => ThemeProvider()..init(),
        ),
      ],
      child: Consumer<ThemeProvider>(
        builder: (context, themeProvider, child) {
          return WithForegroundTask(
            child: MaterialApp(
              title: 'NetFlow',
              debugShowCheckedModeBanner: false,
              
              // Temas Material Design 3 Expressive
              theme: AppTheme.lightTheme,
              darkTheme: AppTheme.darkTheme,
              themeMode: themeProvider.themeMode,
              
              // Pantalla principal
              home: const HomeScreen(),
            ),
          );
        },
      ),
    );
  }
}
