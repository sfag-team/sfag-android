package com.droidhen.formalautosim.data.local

import android.content.ContentValues
import android.database.Cursor
import com.droidhen.formalautosim.core.entities.AutomataUser
import com.droidhen.formalautosim.core.entities.machines.FiniteMachine
import com.droidhen.formalautosim.core.entities.machines.Machine
import com.droidhen.formalautosim.core.entities.machines.MachineType
import com.droidhen.formalautosim.core.entities.machines.PushDownMachine
import com.droidhen.formalautosim.data.local.MachineCard.COLUMN_MACHINE_DATA
import com.droidhen.formalautosim.data.local.MachineCard.COLUMN_MACHINE_ID
import com.droidhen.formalautosim.data.local.MachineCard.COLUMN_MACHINE_NAME
import com.droidhen.formalautosim.data.local.MachineCard.COLUMN_MACHINE_TYPE
import com.droidhen.formalautosim.data.local.MachineCard.COLUMN_MACHINE_VERSION
import com.droidhen.formalautosim.data.local.MachineCard.COLUMN_SAVED_INPUTS
import com.droidhen.formalautosim.data.local.MachineCard.TABLE_MACHINES
import javax.inject.Inject

class AutomataSQLite @Inject constructor(private val database: AutomataSQLModel) {

    fun insertUser(user: AutomataUser): Long {
        val db = database.writableDatabase
        val values = ContentValues().apply {
            put("name", user.getName())
            put("email", user.getLogInData().first)
            put("password", user.getLogInData().second)
        }

        return db.insert("users", null, values)
    }

    fun insertOrUpdateMachine(
        name: String,
        version: Int,
        type: String,
        jffData: String,
        inputs: List<String>
    ) {
        val db = database.writableDatabase

        val cursor = db.rawQuery(
            "SELECT $COLUMN_MACHINE_ID FROM $TABLE_MACHINES WHERE $COLUMN_MACHINE_NAME = ?",
            arrayOf(name)
        )

        val values = ContentValues().apply {
            put(COLUMN_MACHINE_NAME, name)
            put(COLUMN_MACHINE_VERSION, version)
            put(COLUMN_MACHINE_TYPE, type)
            put(COLUMN_MACHINE_DATA, jffData)
            put(COLUMN_SAVED_INPUTS, inputs.joinToString(" "))
        }

        if (cursor.moveToFirst()) {
            db.update(
                TABLE_MACHINES,
                values,
                "$COLUMN_MACHINE_NAME = ?",
                arrayOf(name)
            )
        } else {
            db.insert(TABLE_MACHINES, null, values)
        }

        cursor.close()
        db.close()
    }

    fun getAllMachineNames(): List<String> {
        val db = database.readableDatabase
        val list = mutableListOf<String>()

        val cursor = db.rawQuery("SELECT $COLUMN_MACHINE_NAME FROM $TABLE_MACHINES", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return list
    }

    fun getMachineByName(name: String): Machine? {
        val db = database.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_MACHINE_NAME, $COLUMN_MACHINE_VERSION, $COLUMN_MACHINE_TYPE, $COLUMN_MACHINE_DATA, $COLUMN_SAVED_INPUTS " +
                    "FROM $TABLE_MACHINES WHERE $COLUMN_MACHINE_NAME = ?",
            arrayOf(name)
        )
        cursor.moveToFirst()

        val name = cursor.getString(0)
        val version = cursor.getInt(1)
        val type = cursor.getString(2)
        val jffData = cursor.getString(3)
        val savedInputs = cursor.getString(4).split(" ").map{ StringBuilder(it)}.toMutableList()
        cursor.close()
        db.close()
        val (states, transitions) = ExternalStorageController().parseJff(jffData)

        val result = if (type.equals(MachineType.Finite.tag)) FiniteMachine(
            name,
            version,
            states.toMutableList(),
            transitions.toMutableList(),
            savedInputs
        ) else PushDownMachine(
            name, version,
            states.toMutableList(),
            transitions.toMutableList(),
            savedInputs
        )
        return result
    }

    fun getUser(): AutomataUser? {
        val db = database.readableDatabase
        val cursor: Cursor = db.query(
            "users", null, null, null, null, null, null
        )
        var user: AutomataUser? = null

        with(cursor) {
            while (moveToNext()) {
                user = AutomataUser()
                val name = getString(getColumnIndexOrThrow("name"))
                val email = getString(getColumnIndexOrThrow("email"))
                val password = getString(getColumnIndexOrThrow("password"))
                user!!.setUserName(name)
                user!!.setEmail(email)
                user!!.setPassword(password)
            }
        }
        cursor.close()
        return user
    }
}

