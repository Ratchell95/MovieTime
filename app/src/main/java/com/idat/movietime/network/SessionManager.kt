package com.idat.movietime.network

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    companion object {
        private const val PREFS_NAME     = "movietime_prefs"
        private const val KEY_TOKEN      = "jwt_token"
        private const val KEY_ID_USUARIO = "id_usuario"
        private const val KEY_NOMBRES    = "nombres"
        private const val KEY_ROL        = "rol"
        private const val KEY_EMAIL      = "email"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun guardarSesion(
        token:     String,
        idUsuario: Int,
        nombres:   String,
        rol:       String,
        email:     String
    ) {
        prefs.edit()
            .putString(KEY_TOKEN,      token)
            .putInt   (KEY_ID_USUARIO, idUsuario)
            .putString(KEY_NOMBRES,    nombres)
            .putString(KEY_ROL,        rol)
            .putString(KEY_EMAIL,      email)
            .apply()
    }

    fun getToken():     String? = prefs.getString(KEY_TOKEN, null)
    fun getIdUsuario(): Int     = prefs.getInt(KEY_ID_USUARIO, -1)
    fun getNombres():   String? = prefs.getString(KEY_NOMBRES, null)
    fun getRol():       String? = prefs.getString(KEY_ROL, null)
    fun getEmail():     String? = prefs.getString(KEY_EMAIL, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}