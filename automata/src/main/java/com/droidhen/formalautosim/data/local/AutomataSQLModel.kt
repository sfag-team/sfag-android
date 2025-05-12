package com.droidhen.formalautosim.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.droidhen.formalautosim.data.local.MachineCard.SQL_CREATE_MACHINES_TABLE
import com.droidhen.formalautosim.data.local.MachineCard.TABLE_MACHINES
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AutomataSQLModel @Inject constructor (@ApplicationContext context:Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "mydatabase.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_NAME = "users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_EMAIL = "email"

        private const val SQL_CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT,
                $COLUMN_PASSWORD TEXT,
                $COLUMN_EMAIL TEXT
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_TABLE)
        db.execSQL(SQL_CREATE_MACHINES_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MACHINES")
        onCreate(db)
    }
}


object MachineCard {

     const val TABLE_MACHINES = "machines"
     const val COLUMN_MACHINE_ID = "id"
     const val COLUMN_MACHINE_NAME = "name"
     const val COLUMN_MACHINE_VERSION = "version"
     const val COLUMN_MACHINE_TYPE = "type"
     const val COLUMN_MACHINE_DATA = "jff_data"
    const val COLUMN_SAVED_INPUTS = "saved_inputs"

     const val SQL_CREATE_MACHINES_TABLE = """
    CREATE TABLE $TABLE_MACHINES (
        $COLUMN_MACHINE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $COLUMN_MACHINE_NAME TEXT,
        $COLUMN_MACHINE_VERSION INTEGER,
        $COLUMN_MACHINE_TYPE TEXT,
        $COLUMN_MACHINE_DATA TEXT,
        $COLUMN_SAVED_INPUTS TEXT
    )
    """
}
