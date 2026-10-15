# Dépendances

Code original sous MIT ; aucune dépendance GPL ou CC-NC intégrée à l’application.

- AndroidX Compose, Activity, Lifecycle, Room : Apache-2.0, https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt
- Kotlin et kotlinx.coroutines : Apache-2.0, https://github.com/JetBrains/kotlin et https://github.com/Kotlin/kotlinx.coroutines
- Android Gradle Plugin : Apache-2.0, outil de compilation ; SDK soumis aux conditions Android SDK, non redistribué dans les sources.
- Gradle wrapper : Apache-2.0, https://github.com/gradle/gradle/blob/master/LICENSE ; launcher standard réutilisé depuis un projet local, sans code applicatif de cet autre projet.
- JUnit 4 : EPL-1.0, dépendance de test seulement, https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt

Les AAR/JAR de dépendances conservent leurs métadonnées de licence à la construction. Les notices de référence sont documentées ci-dessus ; les vérifications de redistribution devront être actualisées à chaque changement de dépendances. Les binaires ELM327-emulator, AndrOBD, python-OBD, Toyota Diagnostics, OBDLink et Carista ne sont pas inclus. Leur étude est détaillée dans docs/SOURCES.md.
