# Kyro 🦉
### Asistente Inteligente de Estudio y Bienestar Digital

![Status](https://img.shields.io/badge/Status-En_Desarrollo-yellow)
![Platform](https://img.shields.io/badge/Platform-Android-green)
![Backend](https://img.shields.io/badge/Backend-Firebase-orange)

## 📖 Descripción del Proyecto

[cite_start]**Kyro** es una aplicación móvil desarrollada por el equipo **Green Tonic** que actúa en la intersección de la tecnología educativa (EdTech) y la optimización de la productividad personal[cite: 11].

[cite_start]En un entorno educativo saturado de información y estímulos digitales, Kyro soluciona la paradoja de tener acceso ilimitado a contenidos pero dificultades crecientes de concentración[cite: 10]. [cite_start]No es solo un repositorio de apuntes; utiliza **Inteligencia Artificial** y **Procesamiento del Lenguaje Natural (PLN)** para transformar materiales pasivos en herramientas de aprendizaje activo, mientras gestiona el bienestar digital del estudiante[cite: 14, 15].

## ✨ Funcionalidades Principales

### 🧠 Aprendizaje Adaptativo con IA
* [cite_start]**Generación de Contenido:** Transformación automática de apuntes (PDF/Texto) en preguntas tipo test y ejercicios de desarrollo[cite: 67].
* [cite_start]**Tutoría Personalizada:** La IA actúa bajo la **Teoría del Scaffolding**, adaptando la dificultad de los ejercicios basándose en los fallos y aciertos del usuario para reforzar las áreas débiles[cite: 86, 88].

### 🛡️ Modo Focus y Bienestar Digital
* [cite_start]**Detección de Distracciones:** Sistema inteligente que detecta el uso excesivo de aplicaciones de "procrastinación" (RRSS, YouTube)[cite: 29].
* [cite_start]**Re-enfoque Inmediato:** Envío de notificaciones push contextuales para redirigir al estudiante al estudio con una fricción mínima (un solo clic), fomentando el **Estado de Flow**[cite: 15, 124].

### 🏆 Gamificación "Study to Win"
* **Sistema de Motivación:** Basado en la **Teoría de la Autodeterminación (SDT)**. [cite_start]Incluye rachas diarias, recompensas y desbloqueo progresivo de funcionalidades para mantener la constancia[cite: 18, 93].

### 📅 Gestión Académica
* [cite_start]**Calendario Inteligente:** Gestión de fechas de exámenes y entregas independiente al calendario del sistema, con recordatorios automatizados[cite: 28, 188].

## 🛠️ Stack Tecnológico

[cite_start]La arquitectura del proyecto es híbrida/multiplataforma para garantizar escalabilidad y mantenimiento unificado[cite: 37].

* [cite_start]**Frontend:** Desarrollo Multiplataforma (Enfocado en Android)[cite: 176].
* **Backend (BaaS):** Google Firebase.
    * [cite_start]**Authentication:** Gestión segura de sesiones[cite: 59].
    * [cite_start]**Firestore (NoSQL):** Almacenamiento de temarios, estadísticas y perfiles[cite: 60].
    * [cite_start]**Cloud Functions:** Lógica de servidor para procesar datos y conectar con modelos de IA sin gestionar infraestructura física.
* [cite_start]**IA & NLP:** Modelos de procesamiento de lenguaje para la generación de tests y análisis de patrones de aprendizaje[cite: 64, 69].

## 🔒 Privacidad y Marco Regulatorio

Kyro ha sido diseñado siguiendo estrictamente el **RGPD** y las políticas de las tiendas de aplicaciones:

1.  [cite_start]**Permisos de Accesibilidad:** El monitoreo de aplicaciones de terceros se realiza mediante permisos especiales, justificados estrictamente para la funcionalidad de "Bienestar Digital" y sin fines maliciosos[cite: 159, 160].
2.  [cite_start]**Cifrado de Datos:** Los datos académicos y personales se cifran tanto en tránsito como en reposo[cite: 167].
3.  [cite_start]**Anonimización:** La información enviada a la IA para generar ejercicios es anonimizada para prevenir brechas de datos personales[cite: 167].
4.  [cite_start]**Derecho al Olvido:** Los usuarios pueden solicitar la eliminación total de sus datos y progreso[cite: 156].

## 🚀 Instalación y Configuración

*(Instrucciones para desarrolladores)*

1.  Clonar el repositorio:
    ```bash
    git clone [https://github.com/GreenTonic/kyro-app.git](https://github.com/GreenTonic/kyro-app.git)
    ```
2.  Instalar dependencias:
    ```bash
    flutter pub get  # (O npm install si usamos React Native)
    ```
3.  Configurar Firebase:
    * Añadir el archivo `google-services.json` en la carpeta `android/app`.
4.  Ejecutar la aplicación:
    ```bash
    flutter run
    ```

## 👥 Autores - Equipo Green Tonic

* **Iván Jesús Corbalán Pascual**
* **Antonio José López Cortés**
* **Sergio Martínez Alonso**

---
*Este proyecto es parte del Trabajo de Fin de Grado (TFG) para la titulación en Desarrollo de Aplicaciones Multiplataforma.*