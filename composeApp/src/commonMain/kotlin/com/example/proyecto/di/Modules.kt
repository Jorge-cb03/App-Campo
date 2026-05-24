package com.example.proyecto.di

import com.example.proyecto.data.database.AppDatabase
import com.example.proyecto.data.database.getDatabaseBuilder
import com.example.proyecto.data.repository.AnimalRepository
import com.example.proyecto.data.repository.AuthRepository
import com.example.proyecto.data.repository.JardineraRepository
import com.example.proyecto.ui.animals.AnimalsViewModel
import com.example.proyecto.ui.chat.ChatViewModel
import com.example.proyecto.ui.garden.GardenViewModel
import com.example.proyecto.util.LocationProvider
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.auth.auth

val appModule = module {
    // Definición de la Base de Datos y DAOs
    single<AppDatabase> { getDatabaseBuilder().build() }
    single { get<AppDatabase>().jardineraDao() }
    single { get<AppDatabase>().bancalDao() }
    single { get<AppDatabase>().productoDao() }
    single { get<AppDatabase>().entradaDiarioDao() }
    single { get<AppDatabase>().animalDao() }

    // LocationProvider
    single { LocationProvider() }

    // Firebase Services
    single { Firebase.firestore }
    single { Firebase.auth }

    // Repositorios
    single {
        JardineraRepository(
            db = get<AppDatabase>(),
            firestore = get(),
            auth = get()
        )
    }

    single { AnimalRepository(get(), get(), get()) }

    single { AuthRepository() }

    // ViewModel
    viewModel { AnimalsViewModel(get(), get()) }
    viewModel {
        GardenViewModel(
            repository = get<JardineraRepository>(),
            authRepository = get<AuthRepository>(),
            locationProvider = get<LocationProvider>()
        )
    }
    viewModel { ChatViewModel() }
}