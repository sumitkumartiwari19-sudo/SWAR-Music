package com.example.data.repository

import android.content.Context
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<UserProfile?>
    fun getCurrentUser(): UserProfile?
    suspend fun signInWithGoogle(context: Context): Result<UserProfile>
    suspend fun signOut(): Result<Unit>
    suspend fun syncUserProfile(user: UserProfile): Result<Unit>
}
