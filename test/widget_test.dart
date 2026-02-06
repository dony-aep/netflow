// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter_test/flutter_test.dart';

import 'package:netflow/main.dart';

void main() {
  testWidgets('NetFlow app loads correctly', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const NetFlowApp());

    // Verify that the app title is displayed
    expect(find.text('NetFlow'), findsOneWidget);

    // Verify that the main UI elements are present
    expect(find.text('Velocidad actual'), findsOneWidget);
    expect(find.text('Uso de hoy'), findsOneWidget);
    expect(find.text('Iniciar monitoreo'), findsOneWidget);
  });
}
