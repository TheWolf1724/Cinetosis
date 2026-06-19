# Reglas de ProGuard/R8 para Cinetosis.
# La app no usa reflexión ni serialización compleja, así que las reglas por defecto bastan.

# Mantener metadatos de Kotlin.
-keepattributes *Annotation*, InnerClasses, Signature

# Componentes declarados en el manifiesto (Services, TileService) los conserva AGP
# automáticamente, pero los dejamos explícitos por claridad.
-keep class com.thewolf1724.cinetosis.service.OverlayService { *; }
-keep class com.thewolf1724.cinetosis.tile.CinetosisTileService { *; }
