package com.idat.movietime.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import com.idat.movietime.db.DatabaseHelper
import com.idat.movietime.model.LoginResponse
import com.idat.movietime.network.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

sealed class AuthResult {
    data class Success(val data: LoginResponse) : AuthResult()
    data class Error(val mensaje: String)       : AuthResult()
    object SinConexion                          : AuthResult()
}

class AuthRepository(private val context: Context) {

    private val sessionManager = SessionManager(context)
    private val dbHelper       = DatabaseHelper(context)

    suspend fun login(documento: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT id_usuario, nombres, apellidos, email, rol, password_hash, estado FROM usuarios WHERE documento = ?",
                arrayOf(documento)
            )

            if (cursor.moveToFirst()) {
                val idUsuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario"))
                val nombresDb = cursor.getString(cursor.getColumnIndexOrThrow("nombres")) ?: ""
                val apellidosDb = cursor.getString(cursor.getColumnIndexOrThrow("apellidos")) ?: ""
                val emailDb = cursor.getString(cursor.getColumnIndexOrThrow("email")) ?: ""
                val rolDb = cursor.getString(cursor.getColumnIndexOrThrow("rol")) ?: "Cliente"
                val hashDb = cursor.getString(cursor.getColumnIndexOrThrow("password_hash")) ?: ""
                val estadoDb = cursor.getString(cursor.getColumnIndexOrThrow("estado")) ?: "Activo"

                cursor.close()

                if (estadoDb != "Activo") {
                    registrarHistorialAccesoFallido(documento)
                    return@withContext AuthResult.Error("El usuario se encuentra inactivo.")
                }

                val parts = hashDb.split(":")
                if (parts.size == 2) {
                    val salt = parts[0]
                    val hashCalculado = hashSHA256ConSalt(password, salt)

                    if (hashDb == hashCalculado) {
                        val userData = LoginResponse(
                            token = "token_local_${System.currentTimeMillis()}",
                            idUsuario = idUsuario,
                            nombres = nombresDb,
                            apellidos = apellidosDb,
                            email = emailDb,
                            rol = rolDb
                        )

                        sessionManager.guardarSesion(
                            token = userData.token,
                            idUsuario = userData.idUsuario,
                            nombres = userData.nombres,
                            rol = userData.rol,
                            email = userData.email
                        )

                        registrarHistorialAcceso(idUsuario, "Exitoso")
                        return@withContext AuthResult.Success(userData)
                    }
                }

                registrarHistorialAccesoFallido(documento)
                return@withContext AuthResult.Error("Credenciales incorrectas")

            } else {
                cursor.close()
                registrarHistorialAccesoFallido(documento)
                return@withContext AuthResult.Error("Credenciales incorrectas")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext AuthResult.Error("Error al acceder a la base de datos local.")
        }
    }

    suspend fun loginConGoogle(email: String, nombres: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            var idUsuario = -1
            var nombreCompleto = nombres
            var rolAsignado = "Cliente"

            val cursor = db.rawQuery(
                "SELECT id_usuario, nombres, apellidos, rol FROM usuarios WHERE email = ?",
                arrayOf(email)
            )

            if (cursor.moveToFirst()) {
                idUsuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario"))
                val nombresDb = cursor.getString(cursor.getColumnIndexOrThrow("nombres")) ?: ""
                val apellidosDb = cursor.getString(cursor.getColumnIndexOrThrow("apellidos")) ?: ""
                nombreCompleto = if (apellidosDb.isNotEmpty()) "$nombresDb $apellidosDb" else nombresDb
                rolAsignado = cursor.getString(cursor.getColumnIndexOrThrow("rol")) ?: "Cliente"
            } else {
                val values = ContentValues().apply {
                    put("nombres", nombres)
                    put("apellidos", "")
                    put("documento", "GOO-${System.currentTimeMillis().toString().takeLast(5)}")
                    put("email", email)
                    put("telefono", "")
                    put("password_hash", hashSHA256ConSalt("google_dummy_pass", "google_salt"))
                    put("rol", "Cliente")
                    put("estado", "Activo")
                }
                idUsuario = db.insert("usuarios", null, values).toInt()

                if (idUsuario == -1) {
                    return@withContext AuthResult.Error("Error al registrar usuario localmente.")
                }
            }
            cursor.close()

            val userData = LoginResponse(
                token = "token_firebase_${System.currentTimeMillis()}",
                idUsuario = idUsuario,
                nombres = nombreCompleto,
                apellidos = "",
                email = email,
                rol = rolAsignado
            )

            sessionManager.guardarSesion(
                token = userData.token,
                idUsuario = userData.idUsuario,
                nombres = userData.nombres,
                rol = userData.rol,
                email = userData.email
            )

            registrarHistorialAcceso(idUsuario, "Exitoso (Google)")
            AuthResult.Success(userData)

        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult.Error("Error interno procesando Google Login")
        }
    }

    private fun hashSHA256ConSalt(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest((salt + password).toByteArray(Charsets.UTF_8))
        return "$salt:${digest.joinToString("") { "%02x".format(it) }}"
    }

    private fun registrarHistorialAcceso(idUsuario: Int, resultado: String) {
        try {
            val db = dbHelper.writableDatabase
            db.execSQL("CREATE TABLE IF NOT EXISTS historial_accesos (id_acceso INTEGER PRIMARY KEY AUTOINCREMENT, id_usuario INTEGER, dispositivo TEXT, resultado TEXT)")

            val values = ContentValues().apply {
                put("id_usuario",  idUsuario)
                put("dispositivo", Build.MODEL)
                put("resultado",   resultado)
            }
            db.insert("historial_accesos", null, values)
        } catch (e: Exception) { }
    }

    fun isLoggedIn(): Boolean {
        return sessionManager.getToken() != null
    }

    private fun registrarHistorialAccesoFallido(documento: String) {
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT id_usuario FROM usuarios WHERE documento = ?",
                arrayOf(documento)
            )
            if (cursor.moveToFirst()) {
                val idUsuario = cursor.getInt(0)
                registrarHistorialAcceso(idUsuario, "Fallido")
            }
            cursor.close()
        } catch (e: Exception) { }
    }

    fun cerrarSesion() {
        sessionManager.cerrarSesion()
    }

    suspend fun registro(
        nombres: String,
        apellidos: String,
        email: String,
        documento: String,
        password: String
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase

            val cursorDoc = db.rawQuery(
                "SELECT id_usuario FROM usuarios WHERE documento = ?",
                arrayOf(documento)
            )
            if (cursorDoc.moveToFirst()) {
                cursorDoc.close()
                return@withContext AuthResult.Error("El documento ya está registrado.")
            }
            cursorDoc.close()

            val cursorEmail = db.rawQuery(
                "SELECT id_usuario FROM usuarios WHERE email = ?",
                arrayOf(email)
            )
            if (cursorEmail.moveToFirst()) {
                cursorEmail.close()
                return@withContext AuthResult.Error("El correo ya está registrado.")
            }
            cursorEmail.close()

            val salt = System.currentTimeMillis().toString()
            val passwordHash = hashSHA256ConSalt(password, salt)

            val values = ContentValues().apply {
                put("nombres",       nombres)
                put("apellidos",     apellidos)
                put("documento",     documento)
                put("email",         email)
                put("password_hash", passwordHash)
                put("rol",           "Cliente")
                put("estado",        "Activo")
            }
            val idUsuario = db.insert("usuarios", null, values).toInt()

            if (idUsuario == -1) {
                return@withContext AuthResult.Error("Error al crear la cuenta.")
            }

            val userData = LoginResponse(
                token     = "token_local_${System.currentTimeMillis()}",
                idUsuario = idUsuario,
                nombres   = nombres,
                apellidos = apellidos,
                email     = email,
                rol       = "Cliente"
            )
            sessionManager.guardarSesion(
                token     = userData.token,
                idUsuario = userData.idUsuario,
                nombres   = userData.nombres,
                rol       = userData.rol,
                email     = userData.email
            )

            registrarHistorialAcceso(idUsuario, "Registro exitoso")
            AuthResult.Success(userData)

        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult.Error("Error al registrar usuario.")
        }
    }
}