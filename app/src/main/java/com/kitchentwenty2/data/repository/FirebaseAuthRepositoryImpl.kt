package com.kitchentwenty2.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.kitchentwenty2.domain.model.AppUser
import com.kitchentwenty2.domain.model.UserRole
import com.kitchentwenty2.domain.model.toAppUser
import com.kitchentwenty2.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepositoryImpl @Inject constructor() : AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    private val _currentUser = MutableStateFlow<AppUser?>(
        auth.currentUser?.toAppUser()
    )

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser?.toAppUser()
        }
    }

    override val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    override fun getCurrentUser(): AppUser? = _currentUser.value

    override fun hasRole(role: UserRole): Boolean = _currentUser.value?.hasRole(role) == true

    override suspend fun signInWithEmailPassword(email: String, password: String): Result<AppUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
                val firebaseUser = result.user
                    ?: throw IllegalStateException("Authentication succeeded but no user was returned.")
                firebaseUser.toAppUser().also { _currentUser.value = it }
            }
        }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<AppUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val firebaseUser = result.user
                    ?: throw IllegalStateException("Google sign-in succeeded but no user was returned.")
                firebaseUser.toAppUser().also { _currentUser.value = it }
            }
        }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.signOut()
            _currentUser.value = null
        }
    }
}
