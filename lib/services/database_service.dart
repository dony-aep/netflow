import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';

import '../models/models.dart';

/// Servicio para gestionar la base de datos SQLite
class DatabaseService {
  DatabaseService._();

  static Database? _database;
  static const String _tableName = 'daily_usage';
  static const int _dbVersion = 1;

  /// Obtiene la instancia de la base de datos
  static Future<Database> get database async {
    _database ??= await _initDatabase();
    return _database!;
  }

  /// Inicializa la base de datos
  static Future<Database> _initDatabase() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'netflow.db');

    return await openDatabase(
      path,
      version: _dbVersion,
      onCreate: _onCreate,
      onUpgrade: _onUpgrade,
    );
  }

  /// Crea las tablas de la base de datos
  static Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE $_tableName (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        date TEXT NOT NULL UNIQUE,
        wifiReceived INTEGER NOT NULL DEFAULT 0,
        wifiSent INTEGER NOT NULL DEFAULT 0,
        mobileReceived INTEGER NOT NULL DEFAULT 0,
        mobileSent INTEGER NOT NULL DEFAULT 0
      )
    ''');

    // Crear índice para búsquedas por fecha
    await db.execute('''
      CREATE INDEX idx_date ON $_tableName (date)
    ''');
  }

  /// Maneja actualizaciones de versión de la base de datos
  static Future<void> _onUpgrade(
    Database db,
    int oldVersion,
    int newVersion,
  ) async {
    // Manejar migraciones futuras aquí
  }

  // ============== OPERACIONES CRUD ==============

  /// Inserta o actualiza un registro diario
  static Future<int> upsertDailyUsage(DailyUsage usage) async {
    final db = await database;
    
    // Intentar actualizar primero
    final existing = await getDailyUsageByDate(usage.date);
    
    if (existing != null) {
      // Actualizar registro existente
      return await db.update(
        _tableName,
        usage.copyWith(id: existing.id).toMap(),
        where: 'id = ?',
        whereArgs: [existing.id],
      );
    } else {
      // Insertar nuevo registro
      return await db.insert(
        _tableName,
        usage.toMap(),
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
    }
  }

  /// Obtiene el registro de uso de una fecha específica
  static Future<DailyUsage?> getDailyUsageByDate(DateTime date) async {
    final db = await database;
    final dateString = _dateToString(date);

    final results = await db.query(
      _tableName,
      where: 'date = ?',
      whereArgs: [dateString],
      limit: 1,
    );

    if (results.isEmpty) {
      return null;
    }

    return DailyUsage.fromMap(results.first);
  }

  /// Obtiene el registro de hoy, creándolo si no existe
  static Future<DailyUsage> getTodayUsage() async {
    final today = DateTime.now();
    final existing = await getDailyUsageByDate(today);

    if (existing != null) {
      return existing;
    }

    // Crear registro para hoy
    final newUsage = DailyUsage.today();
    await upsertDailyUsage(newUsage);
    return newUsage;
  }

  /// Actualiza el uso de datos de hoy
  static Future<void> updateTodayUsage({
    required int receivedDelta,
    required int sentDelta,
    required NetworkType networkType,
  }) async {
    final today = await getTodayUsage();
    final updated = today.addUsage(
      received: receivedDelta,
      sent: sentDelta,
      networkType: networkType,
    );
    await upsertDailyUsage(updated);
  }

  /// Obtiene todos los registros ordenados por fecha descendente
  static Future<List<DailyUsage>> getAllDailyUsage() async {
    final db = await database;

    final results = await db.query(
      _tableName,
      orderBy: 'date DESC',
    );

    return results.map((map) => DailyUsage.fromMap(map)).toList();
  }

  /// Obtiene registros de los últimos N días
  static Future<List<DailyUsage>> getRecentDailyUsage(int days) async {
    final db = await database;
    final startDate = DateTime.now().subtract(Duration(days: days - 1));
    final startDateString = _dateToString(startDate);

    final results = await db.query(
      _tableName,
      where: 'date >= ?',
      whereArgs: [startDateString],
      orderBy: 'date DESC',
    );

    return results.map((map) => DailyUsage.fromMap(map)).toList();
  }

  /// Obtiene registros en un rango de fechas
  static Future<List<DailyUsage>> getDailyUsageRange(
    DateTime startDate,
    DateTime endDate,
  ) async {
    final db = await database;

    final results = await db.query(
      _tableName,
      where: 'date >= ? AND date <= ?',
      whereArgs: [_dateToString(startDate), _dateToString(endDate)],
      orderBy: 'date DESC',
    );

    return results.map((map) => DailyUsage.fromMap(map)).toList();
  }

  /// Obtiene el total de uso en un rango de fechas
  static Future<Map<String, int>> getTotalUsageInRange(
    DateTime startDate,
    DateTime endDate,
  ) async {
    final db = await database;

    final result = await db.rawQuery('''
      SELECT 
        SUM(wifiReceived) as totalWifiReceived,
        SUM(wifiSent) as totalWifiSent,
        SUM(mobileReceived) as totalMobileReceived,
        SUM(mobileSent) as totalMobileSent
      FROM $_tableName
      WHERE date >= ? AND date <= ?
    ''', [_dateToString(startDate), _dateToString(endDate)]);

    if (result.isEmpty) {
      return {
        'wifiReceived': 0,
        'wifiSent': 0,
        'mobileReceived': 0,
        'mobileSent': 0,
      };
    }

    final row = result.first;
    return {
      'wifiReceived': (row['totalWifiReceived'] as int?) ?? 0,
      'wifiSent': (row['totalWifiSent'] as int?) ?? 0,
      'mobileReceived': (row['totalMobileReceived'] as int?) ?? 0,
      'mobileSent': (row['totalMobileSent'] as int?) ?? 0,
    };
  }

  /// Elimina un registro por ID
  static Future<int> deleteDailyUsage(int id) async {
    final db = await database;
    return await db.delete(
      _tableName,
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  /// Elimina registros anteriores a una fecha
  static Future<int> deleteOldRecords(DateTime beforeDate) async {
    final db = await database;
    return await db.delete(
      _tableName,
      where: 'date < ?',
      whereArgs: [_dateToString(beforeDate)],
    );
  }

  /// Limpia todos los datos
  static Future<void> clearAllData() async {
    final db = await database;
    await db.delete(_tableName);
  }

  /// Cierra la base de datos
  static Future<void> close() async {
    if (_database != null) {
      await _database!.close();
      _database = null;
    }
  }

  // ============== UTILIDADES ==============

  /// Convierte fecha a string para almacenamiento
  static String _dateToString(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }
}
