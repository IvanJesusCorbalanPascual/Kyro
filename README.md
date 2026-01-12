# Kyro 🦉
### Asistente Inteligente de Estudio y Bienestar Digital

![Status](https://img.shields.io/badge/Status-En_Desarrollo-yellow)
![Platform](https://img.shields.io/badge/Platform-Android_Nativo-green)
![Language](https://img.shields.io/badge/Language-Kotlin-purple)
![Backend](https://img.shields.io/badge/Backend-Supabase-emerald)

## 📖 Descripción del Proyecto

**Kyro** es una aplicación móvil desarrollada por el equipo **Green Tonic** que actúa en la intersección de la tecnología educativa (EdTech) y la optimización de la productividad personal.

En un entorno educativo saturado de información y estímulos digitales, Kyro soluciona la paradoja de tener acceso ilimitado a contenidos pero dificultades crecientes de concentración. No es solo un repositorio de apuntes; utiliza **Inteligencia Artificial** y **Procesamiento del Lenguaje Natural (PLN)** para transformar materiales pasivos en herramientas de aprendizaje activo, mientras gestiona el bienestar digital del estudiante.

## ✨ Funcionalidades Principales

### 🧠 Aprendizaje Adaptativo con IA
* **Generación de Contenido:** Transformación automática de apuntes (PDF/Texto) en preguntas tipo test y ejercicios de desarrollo mediante la API de Gemini.
* **Tutoría Personalizada:** La IA actúa bajo la **Teoría del Scaffolding**, adaptando la dificultad de los ejercicios basándose en los fallos y aciertos del usuario para reforzar las áreas débiles.

### 🛡️ Modo Focus y Bienestar Digital
* **Detección de Distracciones:** Sistema inteligente que detecta el uso excesivo de aplicaciones de "procrastinación" (RRSS, YouTube).
* **Re-enfoque Inmediato:** Envío de notificaciones push contextuales para redirigir al estudiante al estudio con una fricción mínima, fomentando el **Estado de Flow**.

### 🏆 Gamificación "Study to Win"
* **Sistema de Motivación:** Basado en la **Teoría de la Autodeterminación (SDT)**. Incluye rachas diarias, recompensas y desbloqueo progresivo de funcionalidades para mantener la constancia.

### 📅 Gestión Académica
* **Calendario Inteligente:** Gestión de fechas de exámenes y entregas independiente al calendario del sistema, con recordatorios automatizados.

## 🛠️ Stack Tecnológico

El proyecto está desarrollado de forma nativa para garantizar el máximo rendimiento y acceso a las APIs del sistema Android.

* **Frontend:** Android Nativo (Kotlin / XML & Jetpack Components).
* **Backend (BaaS):** Supabase.
    * **Authentication:** Gestión segura de sesiones (GoTrue).
    * **PostgreSQL:** Base de datos relacional para temarios, estadísticas y perfiles.
    * **Edge Functions:** Lógica de servidor para procesar datos.
* **IA & NLP:** Integración con Google Gemini Pro/Flash para la generación de tests y análisis de patrones de aprendizaje.

## 🔒 Privacidad y Marco Regulatorio

Kyro ha sido diseñado siguiendo estrictamente el **RGPD** y las políticas de Google Play:

1.  **Permisos de Accesibilidad:** El monitoreo de aplicaciones de terceros se realiza mediante permisos especiales, justificados estrictamente para la funcionalidad de "Bienestar Digital".
2.  **Cifrado de Datos:** Los datos académicos y personales se cifran tanto en tránsito como en reposo.
3.  **Anonimización:** La información enviada a la IA para generar ejercicios es anonimizada para prevenir brechas de datos personales.
4.  **Derecho al Olvido:** Los usuarios pueden solicitar la eliminación total de sus datos y progreso desde la app.

## 🚀 Instalación y Configuración

*(Instrucciones para desarrolladores)*

1.  Clonar el repositorio:
    ```bash
    git clone [https://github.com/GreenTonic/kyro-app.git](https://github.com/GreenTonic/kyro-app.git)
    ```
2.  Abrir el proyecto en **Android Studio** (Koala o superior recomendado).
3.  Configurar Secretos:
    * Crear un archivo `local.properties` en la raíz del proyecto.
    * Añadir tus claves API:
      ```properties
      SUPABASE_URL=tu_url_de_supabase
      SUPABASE_KEY=tu_anon_key
      GEMINI_API_KEY=tu_api_key_de_google
      ```
4.  Sincronizar Gradle y Ejecutar:
    * Pulsar "Sync Project with Gradle Files".
    * Seleccionar un emulador o dispositivo físico y pulsar **Run (Shift+F10)**.

## 👥 Autores - Equipo Green Tonic

* **Iván Jesús Corbalán Pascual**
* **Antonio José López Cortés**
* **Sergio Martínez Alonso**

---
*Este proyecto es parte del Trabajo de Fin de Grado (TFG) para la titulación en Desarrollo de Aplicaciones Multiplataforma.*
