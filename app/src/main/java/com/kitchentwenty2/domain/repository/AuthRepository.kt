package com.kitchentwenty2.domain.repository

import com.kitchentwenty2.domain.model.AppUser
import com.kitchentwenty2.domain.model.UserRole
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<AppUser?>

    fun getCurrentUser(): AppUser?
    fun hasRole(role: UserRole): Boolean

    suspend fun signInWithEmailPassword(email: String, password: String): Result<AppUser>
    suspend fun signInWithGoogleIdToken(idToken: String): Result<AppUser>
    suspend fun signOut(): Result<Unit>
}
