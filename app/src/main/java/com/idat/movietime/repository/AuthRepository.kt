package com.idat.movietime.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import com.idat.movietime.db.MovieTimeDatabaseHelper
import com.idat.movietime.model.LoginRequest
import com.idat.movietime.model.LoginResponse
import com.idat.movietime.network.RetrofitClient
import com.idat.movietime.network.SessionManager

sealed class AuthResult {
    data class Success(val data: LoginResponse) : AuthResult()
    data class Error(val mensaje: String)       : AuthResult()
    object SinConexion                          : AuthResult()
}

class AuthRepository(private val context: Context) {

    private val sessionManager = SessionManager(context)
    private val dbHelper       = MovieTimeDatabaseHelper(context)


    suspend fun login(documento: String, password: String): AuthResult {
        return try {

            val response = RetrofitClient.api.login(
                LoginRequest(email = documento, password = password)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!


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

                registrarHistorialAccesoFallido(documento)
                AuthResult.Error("Documento o contraseña incorrectos")
            }

        } catch (e: Exception) {

            loginLocal(documento, password)
        }
    }


    private fun loginLocal(documento: String, password: String): AuthResult {
        val db = dbHelper.readableDatabase
        val hashPass = hashMD5(password)

        val cursor = db.rawQuery(
            """
            SELECT u.id_usuario, u.nombres, u.apellidos, u.email, r.nombre as rol
            FROM usuarios u
            INNER JOIN roles r ON u.id_rol = r.id_rol
            WHERE (u.email = ? OR u.nombres = ?)
              AND u.password_hash = ?
              AND u.estado = 'Activo'
            """.trimIndent(),
            arrayOf(documento, documento, hashPass)
        )

        return if (cursor.moveToFirst()) {
            val idUsuario = cursor.getInt(cursor.getColumnIndexOrThrow("id_usuario"))
            val nombres   = cursor.getString(cursor.getColumnIndexOrThrow("nombres"))
            val apellidos = cursor.getString(cursor.getColumnIndexOrThrow("apellidos"))
            val email     = cursor.getString(cursor.getColumnIndexOrThrow("email"))
            val rol       = cursor.getString(cursor.getColumnIndexOrThrow("rol"))
            cursor.close()


            sessionManager.guardarSesion(
                token     = "local_token_$idUsuario",
                idUsuario = idUsuario,
                nombres   = nombres,
                rol       = rol,
                email     = email
            )

            registrarHistorialAcceso(idUsuario, "Exitoso")

            AuthResult.Success(
                LoginResponse(
                    token     = "local_token_$idUsuario",
                    idUsuario = idUsuario,
                    nombres   = nombres,
                    apellidos = apellidos,
                    email     = email,
                    rol       = rol
                )
            )
        } else {
            cursor.close()
            registrarHistorialAccesoFallido(documento)
            AuthResult.SinConexion
        }
    }


    private fun registrarHistorialAcceso(idUsuario: Int, resultado: String) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id_usuario",  idUsuario)
                put("dispositivo", Build.MODEL)
                put("resultado",   resultado)
            }
            db.insert(MovieTimeDatabaseHelper.TABLE_HISTORIAL_ACCESOS, null, values)
        } catch (e: Exception) {

        }
    }


    private fun registrarHistorialAccesoFallido(documento: String) {
        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT id_usuario FROM usuarios WHERE email = ?",
                arrayOf(documento)
            )
            if (cursor.moveToFirst()) {
                val idUsuario = cursor.getInt(0)
                registrarHistorialAcceso(idUsuario, "Fallido")
            }
            cursor.close()
        } catch (e: Exception) { }
    }


    private fun hashMD5(input: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun cerrarSesion() = sessionManager.cerrarSesion()
    fun isLoggedIn()   = sessionManager.isLoggedIn()
}
