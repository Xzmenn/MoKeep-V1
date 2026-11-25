package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.DefaultCategoryCatalog
import com.example.data.local.SessionStore
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.SubCategoryEntity
import com.example.data.local.entity.UserEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AuthSettingsRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val sessionStore: SessionStore
) {
    private val dao = db.dao()
    private val firebaseAuth: FirebaseAuth = Firebase.auth

    val firebaseUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    val currentUserId: Flow<String?> = firebaseUserFlow.map { it?.uid }

    fun observeUser(userId: String): Flow<UserEntity?> = dao.observeUser(userId)

    fun observeSetting(userId: String): Flow<SettingEntity?> = dao.observeSetting(userId)

    suspend fun ensureGoogleUserProvisioned(firebaseUser: FirebaseUser): UserEntity {
        val uid = firebaseUser.uid
        val existing = dao.getUserById(uid)
        val displayName = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "Pengguna MoKeep"
        val email = firebaseUser.email?.trim()?.lowercase().orEmpty()
        val photo = firebaseUser.photoUrl?.toString()

        if (existing != null) {
            var updated = existing
            if (updated.imageUri.isNullOrBlank() && !photo.isNullOrBlank()) {
                updated = updated.copy(imageUri = photo, updatedAt = System.currentTimeMillis())
                dao.updateUser(updated)
            }
            if (dao.getSetting(uid) == null) {
                dao.upsertSetting(SettingEntity(id = UUID.randomUUID().toString(), userId = uid))
            }
            sessionStore.setCurrentUserId(uid)
            return updated
        }

        val user = UserEntity(
            id = uid,
            name = displayName,
            email = if (email.isNotBlank()) email else "$uid@google.user",
            passwordHash = "",
            imageUri = photo
        )
        db.withTransaction {
            dao.insertUser(user)
            dao.upsertSetting(
                SettingEntity(
                    id = UUID.randomUUID().toString(),
                    userId = uid
                )
            )
            for (cat in DefaultCategoryCatalog.items) {
                val catId = UUID.randomUUID().toString()
                dao.insertCategory(
                    CategoryEntity(
                        id = catId,
                        userId = uid,
                        name = cat.name,
                        type = cat.type,
                        icon = cat.icon,
                        color = cat.color,
                        isDefault = true
                    )
                )
                for (sub in cat.subs) {
                    dao.insertSubCategory(
                        SubCategoryEntity(
                            id = UUID.randomUUID().toString(),
                            categoryId = catId,
                            name = sub
                        )
                    )
                }
            }
        }
        sessionStore.setCurrentUserId(uid)
        return user
    }

    suspend fun signOut(credentialManager: CredentialManager) {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                firebaseAuth.signOut()
                sessionStore.setCurrentUserId(null)
            }
        }
    }

    suspend fun updateProfile(userId: String, name: String): UserEntity {
        val cleanName = name.trim()
        if (cleanName.length < 2) throw IllegalArgumentException("Nama minimal 2 karakter.")
        val current = dao.getUserById(userId) ?: throw IllegalStateException("Pengguna tidak ditemukan.")
        val updated = current.copy(name = cleanName, updatedAt = System.currentTimeMillis())
        dao.updateUser(updated)
        return updated
    }

    suspend fun saveAvatarFromUri(userId: String, sourceUri: Uri): String {
        val current = dao.getUserById(userId) ?: throw IllegalStateException("Pengguna tidak ditemukan.")
        val avatarsDir = File(context.filesDir, "avatars").apply { mkdirs() }
        val targetFile = File(avatarsDir, "avatar_$userId.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Gagal membaca gambar profil")
        val localUri = Uri.fromFile(targetFile).toString()
        dao.updateUser(current.copy(imageUri = localUri, updatedAt = System.currentTimeMillis()))
        return localUri
    }

    suspend fun updateSettings(
        userId: String,
        currency: String,
        locale: String,
        theme: String,
        weekStartsOn: Int,
        budgetAlertThreshold: Int
    ) {
        val existing = dao.getSetting(userId)
        dao.upsertSetting(
            SettingEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                userId = userId,
                currency = currency,
                locale = locale,
                theme = theme,
                weekStartsOn = weekStartsOn.coerceIn(0, 1),
                budgetAlertThreshold = budgetAlertThreshold.coerceIn(1, 100),
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
