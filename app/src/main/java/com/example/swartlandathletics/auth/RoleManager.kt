package com.example.swartlandathletics.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UserRole {
    SPECTATOR, // Slegs-Kyk / View-only for parents, learners, schools
    OFFICIAL   // Beampte / Full access for judges, timekeepers, administrators
}

object RoleManager {
    private const val PREFS_NAME = "swartland_role_prefs"
    private const val KEY_ROLE = "selected_user_role"
    private const val KEY_PIN = "official_access_pin"
    private const val DEFAULT_PIN = "7300"

    private var prefs: SharedPreferences? = null

    private val _currentRole = MutableStateFlow(UserRole.SPECTATOR)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedRole = prefs?.getString(KEY_ROLE, UserRole.SPECTATOR.name) ?: UserRole.SPECTATOR.name
            _currentRole.value = try {
                UserRole.valueOf(savedRole)
            } catch (e: Exception) {
                UserRole.SPECTATOR
            }
        }
    }

    fun isOfficial(): Boolean = _currentRole.value == UserRole.OFFICIAL

    fun verifyPin(enteredPin: String): Boolean {
        val storedPin = prefs?.getString(KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        val isValid = enteredPin.trim() == storedPin.trim()
        if (isValid) {
            setRole(UserRole.OFFICIAL)
        }
        return isValid
    }

    fun setRole(role: UserRole) {
        _currentRole.value = role
        prefs?.edit()?.putString(KEY_ROLE, role.name)?.apply()
    }

    fun lockToSpectator() {
        setRole(UserRole.SPECTATOR)
    }

    fun getOfficialPin(): String {
        return prefs?.getString(KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    }

    fun updateOfficialPin(currentPin: String, newPin: String): Boolean {
        if (verifyPin(currentPin) && newPin.trim().length >= 4) {
            prefs?.edit()?.putString(KEY_PIN, newPin.trim())?.apply()
            return true
        }
        return false
    }
}
