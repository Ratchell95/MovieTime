package com.idat.movietime.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import com.idat.movietime.db.DatabaseHelper
import com.idat.movietime.model.LoginRequest
import com.idat.movietime.model.LoginResponse
import com.idat.movietime.network.ApiResponse
import com.idat.movietime.network.RetrofitClient
import com.idat.movietime.network.SessionManager

sealed class AuthResult {
    data class Success(val data: LoginResponse) : AuthResult()
    data class Error(val mensaje: String)        : AuthResult()
    object SinConexion                           : AuthResult()
}

class AuthRepository(private val context: Context) {

    private val sessionManager = SessionManager(context)
    private val dbHelper       = DatabaseHelper(context)

    suspend fun login(documento: String, password: String): AuthResult {
        return try {
            // 1. Intentamos con el servidor web (API)
            val response = RetrofitClient.api.login(
                LoginRequest(documento = documento, password = password)
            )

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (!apiResponse.success || apiResponse.data == null) {
                    // El API respondió pero dice que el usuario no existe.
                    // ¡En lugar de dar error, intentamos buscarlo localmente en SQLite!
                    return loginLocal(documento, password)
                }

                val body = apiResponse.data
                sessionManager.guardarSesion(
                    token     = body.token,
                    idUsuario = body.idUsuario,
                    nombres   = body.nombres,
                    rol       = body.rol,
                    email     = body.email
                )

                registrarHistorialAcceso(body.idUsuario, "Exitoso")
                AuthResult.Success(body)

            } else {
                // El servidor arrojó un error HTTP (ej. 401 o 404). Buscamos en local.
                loginLocal(documento, password)
            }

        } catch (e: Exception) {
            // Sin conexión a internet o el servidor está caído → buscamos en local
            loginLocal(documento, password)
        }
    }

    private fun loginLocal(documento: String, password: String): AuthResult {
        val db = dbHelper.readableDatabase

        // 1. Buscar el usuario por columna 'documento'
        val cursor = db.rawQuery(
            """
            SELECT u.id_usuario, u.nombres, u.apellidos, u.email,
                   u.password_hash, r.nombre as rol
            FROM usuarios u
            INNER JOIN roles r ON u.id_rol = r.id_rol
            WHERE u.documento = ?
              AND u.estado = 'Activo'
            """.trimIndent(),
            arrayOf(documento)
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            registrarHistorialAccesoFallido(documento)
            return AuthResult.Error("Documento o contraseña incorrectos")
        }

        val idUsuario    = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario"))
        val nombres      = cursor.getString(cursor.getColumnIndexOrThrow("nombres"))
        val apellidos    = cursor.getString(cursor.getColumnIndexOrThrow("apellidos"))
        val email        = cursor.getString(cursor.getColumnIndexOrThrow("email"))
        val rol          = cursor.getString(cursor.getColumnIndexOrThrow("rol"))
        val passwordHash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"))
        cursor.close()

        // 2. Verificar SHA-256 + salt (formato: "salt:hashSHA256")
        val partes = passwordHash.split(":")
        if (partes.size != 2) {
            return AuthResult.Error("Documento o contraseña incorrectos")
        }
        val salt         = partes[0]
        val hashEsperado = hashSHA256ConSalt(password, salt)

        if (hashEsperado != passwordHash) {
            registrarHistorialAccesoFallido(documento)
            return AuthResult.Error("Documento o contraseña incorrectos")
        }

        // 3. Login exitoso
        sessionManager.guardarSesion(
            token     = "local_token_$idUsuario",
            idUsuario = idUsuario,
            nombres   = nombres,
            rol       = rol,
            email     = email
        )
        registrarHistorialAcceso(idUsuario, "Exitoso")

        return AuthResult.Success(
            LoginResponse(
                token     = "local_token_$idUsuario",
                idUsuario = idUsuario,
                nombres   = nombres,
                apellidos = apellidos,
                email     = email,
                rol       = rol
            )
        )
    }

    // SHA-256 + salt — mismo algoritmo que RegistroActivity
    private fun hashSHA256ConSalt(password: String, salt: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest((salt + password).toByteArray(Charsets.UTF_8))
        return "$salt:${digest.joinToString("") { "%02x".format(it) }}"
    }

    private fun registrarHistorialAcceso(idUsuario: Int, resultado: String) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id_usuario",  idUsuario)
                put("dispositivo", Build.MODEL)
                put("resultado",   resultado)
            }
            db.insert(DatabaseHelper.TABLE_HISTORIAL_ACCESOS, null, values)
        } catch (e: Exception) { /* ignorar errores de log */ }
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
        } catch (e: Exception) { /* ignorar */ }
    }

    fun cerrarSesion() = sessionManager.cerrarSesion()
    fun isLoggedIn()   = sessionManager.isLoggedIn()
}