# SingleRadio App (Alofoke FM)

Esta es una aplicación móvil de radio para Android que permite la transmisión de contenido en vivo con soporte para múltiples formatos y redes de anuncios.

## Características Principales

- **Streaming de Audio**: Soporte para formatos HLS, RTSP, DASH y MP3 mediante Media3 ExoPlayer.
- **Panel de Control**: Diseño moderno con paneles deslizantes (`SlidingUpPanel`).
- **Notificaciones Push**: Integración con Firebase Messaging para alertas en tiempo real.
- **Monetización**: Integración de publicidad mediante Admob Ads SDK.
- **Persistencia Local**: Uso de Room para almacenamiento de favoritos o configuración.
- **UI/UX Premium**: Efectos de carga (Shimmer), imágenes redondeadas y ecualizador visual dinámico.

## Requisitos Técnicos

- **Android SDK**: Mínimo API 23 (Marshmallow), Target API 36.
- **Lenguaje/Herramientas**:
  - Gradle 8.x+
  - Java 17
  - AndroidX & Jetpack Libraries
- **Librerías Clave**:
  - `Media3 ExoPlayer`: Para la reproducción de medios.
  - `Retrofit 2` & `OkHttp`: Para comunicación con APIs.
  - `Glide`: Para carga y procesamiento de imágenes.
  - `Firebase`: Análisis y Notificaciones.

## Configuración del Proyecto

### Requisitos de Construcción
1. Clonar el repositorio.
2. Abrir con **Android Studio** (versión recomendada: Ladybug o superior).
3. Asegurarse de tener configurado el JDK 17 en la configuración del proyecto.
4. Sincronizar Gradle.

### Firmado de Aplicación (Keystores)
El proyecto incluye archivos de firmado para diferentes versiones:
- `keyStoreMatrix.jks`
- `keystoreAlofoke.jks`

### Firebase
Para habilitar las funciones de Firebase, asegúrate de incluir el archivo `google-services.json` correspondiente en el directorio `app/`.

## Estructura del Proyecto
- `app/`: Contiene el código fuente de la aplicación, recursos y configuraciones de construcción.
- `gradle/`: Archivos de configuración del wrapper de Gradle.

---
© 2024 YourSingleRadio - Desarrollado bajo estándares modernos de arquitectura Android.
