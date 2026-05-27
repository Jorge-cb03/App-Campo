# 🌾 AgroFather - Gestión Profesional de Cultivos y Granjas

![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-blue.svg?logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-Multiplatform-4285F4.svg?logo=android)
![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%7C%20Auth-FFCA28.svg?logo=firebase)
![Room](https://img.shields.io/badge/Room-Database-3DDC84.svg?logo=android)
![Koin](https://img.shields.io/badge/Koin-DI-FF4154.svg)

**AgroFather** es una aplicación multiplataforma (Android e iOS) diseñada para modernizar y facilitar la gestión de pequeños y medianos huertos y granjas familiares. 

Este proyecto nace como un **Trabajo de Fin de Grado (TFG)** con el objetivo de dotar al agricultor de una herramienta moderna, basada en una arquitectura *Offline-First* conectada a la nube, que unifique el registro de tareas, el control de inventario y la trazabilidad de flora y fauna.

---

## ✨ Características Principales

* 🌱 **Mi Huerta (Gestión Agrícola):** Creación de jardineras y bancales. Control de siembra, riego, tratamientos y recolección de cosechas (que se suman automáticamente al inventario).
* 🐓 **Animales (Gestión Ganadera):** Organización por cercados, catálogo de especies, alertas de compatibilidad de convivencia, y control de alimentación y producción (ej. recogida de huevos).
* 📖 **Diario de Trazabilidad:** Cuaderno de campo digital. Registra tareas de forma automática o manual, permitiendo adjuntar fotografías como evidencia.
* 📦 **Inventario Inteligente:** Almacén digital de semillas, fertilizantes, herramientas y cosechas. Integración con API botánica para autocompletar fichas técnicas.
* 🤖 **Asistente IA (Gemini):** Chatbot agronómico impulsado por inteligencia artificial para resolver dudas sobre plagas, riego o cuidados de los animales.
* 🌤️ **Clima en Tiempo Real:** Dashboard con datos meteorológicos de la parcela (temperatura, humedad, índice UV) para la toma de decisiones.

## 🛠️ Stack Tecnológico y Arquitectura

La aplicación está construida utilizando las tecnologías y metodologías más modernas de desarrollo móvil:

* **Framework UI:** Jetpack Compose / Compose Multiplatform.
* **Lenguaje:** Kotlin.
* **Arquitectura:** MVVM (Model-View-ViewModel).
* **Inyección de Dependencias:** Koin.
* **Base de Datos Local:** Room Database (SQLite) operando como *Single Source of Truth* (SSOT).
* **Backend y Sincronización:** Firebase Authentication y Cloud Firestore.
* **Navegación:** Jetpack Navigation Compose.
* **Gestión de Medios:** Coil3 (imágenes asíncronas) y soporte para cámara/galería multiplataforma.

### Arquitectura Offline-First
La app está diseñada para funcionar en entornos rurales con baja conectividad. Todas las lecturas y escrituras se realizan en la base de datos local (`Room`). Un servicio en segundo plano se encarga de sincronizar bidireccionalmente los datos con `Firebase` en cuanto el dispositivo detecta conexión a internet.

---

## 🚀 Requisitos Previos

Para clonar y compilar este proyecto, necesitarás:

* **Android Studio** (versión Jellyfish o superior recomendada).
* **Xcode** (solo si deseas compilar el proyecto para iOS en macOS).
* **JDK 17**.
* Plugin de **Kotlin Multiplatform** instalado en Android Studio.

## ⚙️ Instalación y Configuración

1. **Clonar el repositorio:**
   ```bash
   git clone [https://github.com/Jorge-cb03/app-campo.git](https://github.com/Jorge-cb03/app-campo.git)
   cd app-campo

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
