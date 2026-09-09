package com.kitchentwenty2.domain.model

import com.google.firebase.auth.FirebaseUser

enum class UserRole {
    KITCHEN_STAFF,
    KITCHEN_MANAGER,
    CASHIER
}

data class AppUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val role: UserRole = UserRole.KITCHEN_STAFF,
    val providerId: String = "firebase"
) {
    fun hasRole(roleToCheck: UserRole): Boolean = role == roleToCheck
}

fun FirebaseUser.toAppUser(role: UserRole = UserRole.KITCHEN_STAFF): AppUser {
    return AppUser(
        uid = uid,
        email = email,
        displayName = displayName,
        role = role,
        providerId = providerData.firstOrNull()?.providerId ?: providerId
    )
}
