package com.example.proyecto.data.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.GoogleAuthProvider

class AuthRepository {
    private val auth = Firebase.auth

    suspend fun login(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
    }

    suspend fun register(email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass)
    }

    // NUEVO: Método para acceso anónimo (Invitado)
    suspend fun signInAnonymously() {
        auth.signInAnonymously()
    }

    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.credential(idToken, null)
        auth.signInWithCredential(credential)
    }

    fun getCurrentUser() = auth.currentUser

    suspend fun logout() {
        auth.signOut()
    }
}