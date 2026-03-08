package com.forzaball.app.feature.auth

import android.util.Patterns

object AuthValidation {

    fun validateFirstName(value: String): String? {
        return when {
            value.isBlank() -> "First name is required"
            value.length < 2 -> "First name must be at least 2 characters"
            else -> null
        }
    }

    fun validateLastName(value: String): String? {
        return when {
            value.isBlank() -> "Last name is required"
            value.length < 2 -> "Last name must be at least 2 characters"
            else -> null
        }
    }

    fun validateEmail(value: String): String? {
        return when {
            value.isBlank() -> "Email is required"
            !Patterns.EMAIL_ADDRESS.matcher(value).matches() -> "Enter a valid email"
            else -> null
        }
    }

    fun validatePhone(value: String): String? {
        val digits = value.filter { it.isDigit() }
        return when {
            value.isBlank() -> "Phone is required"
            digits.length < 10 -> "Enter a valid phone number"
            else -> null
        }
    }

    fun validatePassword(value: String): String? {
        return when {
            value.isBlank() -> "Password is required"
            value.length < 8 -> "Password must be at least 8 characters"
            !value.any { it.isDigit() } -> "Password must contain at least one digit"
            !value.any { it.isLetter() } -> "Password must contain at least one letter"
            else -> null
        }
    }

    fun validateConfirmPassword(password: String, confirm: String): String? {
        return when {
            confirm.isBlank() -> "Please confirm your password"
            password != confirm -> "Passwords do not match"
            else -> null
        }
    }

    /** For sign-in: accept email or phone (E.164 or digits). */
    fun validateEmailOrPhone(value: String): String? {
        return when {
            value.isBlank() -> "Email or phone is required"
            value.contains("@") -> validateEmail(value)
            else -> validatePhone(value)
        }
    }
}
