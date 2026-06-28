import Database from "better-sqlite3";

export type SqliteDatabase = Database.Database;

export function openDatabase(databasePath: string): SqliteDatabase {
  const db = new Database(databasePath);
  db.pragma("foreign_keys = ON");
  return db;
}
