package com.example.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class AuthRepositoryImpl(
    private val auth: FirebaseAuth? = null,
    private val firestore: FirebaseFirestore? = null
) : AuthRepository {

    private val localUserFlow = kotlinx.coroutines.flow.MutableStateFlow<UserProfile?>(
        UserProfile(
            uid = "local_swar_user",
            email = "user@swarmusic.app",
            displayName = "SWAR Music Lover",
            photoUrl = null
        )
    )

    override val currentUser: Flow<UserProfile?> = callbackFlow {
        val currentAuth = auth
        if (currentAuth == null) {
            trySend(localUserFlow.value)
            localUserFlow.collect { trySend(it) }
            return@callbackFlow
        }

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            trySend(firebaseUser?.toUserProfile() ?: localUserFlow.value)
        }
        currentAuth.addAuthStateListener(listener)
        trySend(currentAuth.currentUser?.toUserProfile() ?: localUserFlow.value)

        awaitClose {
            currentAuth.removeAuthStateListener(listener)
        }
    }

    override fun getCurrentUser(): UserProfile? {
        return auth?.currentUser?.toUserProfile() ?: localUserFlow.value
    }

    override suspend fun signInWithGoogle(context: Context): Result<UserProfile> {
        val currentAuth = auth
        if (currentAuth == null) {
            val guest = UserProfile(
                uid = "local_swar_user",
                email = "user@swarmusic.app",
                displayName = "SWAR Guest User",
                photoUrl = null
            )
            localUserFlow.value = guest
            return Result.success(guest)
        }

        return try {
            val credentialManager = CredentialManager.create(context)

            // Try to resolve Web Client ID from google-services resources if available
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            val serverClientId = if (resId != 0) {
                context.getString(resId)
            } else {
                ""
            }

            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOptionBuilder = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)

            if (serverClientId.isNotBlank()) {
                googleIdOptionBuilder.setServerClientId(serverClientId)
            }

            val googleIdOption = googleIdOptionBuilder.build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = currentAuth.signInWithCredential(authCredential).await()

                val firebaseUser = authResult.user
                    ?: return Result.failure(IllegalStateException("Firebase user was null after Google sign-in"))

                val profile = firebaseUser.toUserProfile()
                syncUserProfile(profile)
                Result.success(profile)
            } else {
                Result.failure(IllegalArgumentException("Unexpected credential type received: ${credential.type}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth?.signOut()
            localUserFlow.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncUserProfile(user: UserProfile): Result<Unit> {
        val currentFirestore = firestore ?: return Result.success(Unit)
        return try {
            if (user.uid.isBlank()) return Result.success(Unit)

            val userDoc = currentFirestore.collection("users").document(user.uid)
            val data = hashMapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "photoUrl" to (user.photoUrl ?: ""),
                "lastLoginAt" to System.currentTimeMillis()
            )
            userDoc.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun FirebaseUser.toUserProfile(): UserProfile {
        return UserProfile(
            uid = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl?.toString(),
            createdAt = metadata?.creationTimestamp ?: System.currentTimeMillis(),
            lastLoginAt = metadata?.lastSignInTimestamp ?: System.currentTimeMillis()
        )
    }
}
