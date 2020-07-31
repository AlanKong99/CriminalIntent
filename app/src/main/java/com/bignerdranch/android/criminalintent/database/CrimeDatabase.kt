package com.bignerdranch.android.criminalintent.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bignerdranch.android.criminalintent.Crime

private const val DATABASE_NAME = "crime-database"
@Database(entities = [Crime::class], version = 4)
@TypeConverters(CrimeTypeConverters::class)
abstract class CrimeDatabase: RoomDatabase() {

    abstract fun crimeDao(): CrimeDao

    companion object {
        fun newDatabase(context: Context): CrimeDatabase {
            return Room.databaseBuilder(
                context,
                CrimeDatabase::class.java,
                DATABASE_NAME
            ).
            addMigrations(migration_1_2).
            addMigrations(migration_2_3).
            addMigrations(migration_3_4).
            build()
        }
    }
}
val migration_1_2 = object : Migration(1,2){
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE Crime ADD COLUMN suspect TEXT NOT NULL DEFAULT ''"
        )
    }

}

val migration_2_3 = object : Migration(2,3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE Crime ADD COLUMN detail TEXT NOT NULL DEFAULT ''"
        )
    }
}

val migration_3_4 = object : Migration(3,4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE Crime ADD COLUMN phoneNumber TEXT NOT NULL DEFAULT ''"
        )
    }
}