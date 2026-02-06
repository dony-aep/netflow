# netflow_traffic_stats

Plugin local de Flutter para NetFlow que expone las estadísticas de tráfico de Android (`TrafficStats`) a cualquier isolate, incluido el background isolate del servicio foreground.

## Funcionalidades

- **TrafficStats**: Lee bytes totales, WiFi y móvil (Rx/Tx) vía `android.net.TrafficStats`.
- **Notificación dinámica**: Incluye `NotificationHelper` y `SpeedIconGenerator` para actualizar la notificación con un icono que muestra la velocidad en tiempo real.
- **Alerta de límite de datos**: Notificación de alta prioridad al alcanzar el límite configurado, con icono monocromático para la barra de estado.

## Uso

```dart
import 'package:netflow_traffic_stats/netflow_traffic_stats.dart';

final stats = await NetflowTrafficStats.getTrafficStats();
// stats['totalRx'], stats['totalTx'], stats['wifiRx'], etc.
```

## Plataforma

Solo Android (API 24+).

