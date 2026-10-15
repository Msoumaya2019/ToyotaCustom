# Toyota Custom — Android 0.1.0

Application native Kotlin/Compose, Android 10 minimum, française, locale et sans abonnement. Première version de développement : diagnostic OBD CAN standard et transport Bluetooth Classic implémentés ; compatibilité réelle NexLink/Corolla non testée. Aucun réglage constructeur Toyota ne peut être lu ou modifié dans cette version.

## Démarrer

Ouvrir ce dossier dans Android Studio. Installer SDK 36 et JDK 17 ou supérieur (JDK 21 utilisé localement). Le wrapper Gradle est fourni. `local.properties` est propre à la machine et ignoré par Git.

```powershell
.\gradlew.bat :core:test :app:assembleDebug :app:lintDebug
```

APK : `app/build/outputs/apk/debug/app-debug.apk`. Signature debug pour essais, pas une clé de distribution durable. Le workflow GitHub Actions fait les mêmes vérifications et publie l’APK en artefact, sans secrets de signature.

Sur téléphone : installer l’APK, ouvrir Paramètres, autoriser Bluetooth, appairer NEXAS dans Android (notice : PIN 1234), rechercher puis choisir l’appareil associé. Le bouton de connexion lance l’initialisation ELM et la lecture VIN/PID. Un VIN inconnu reste inconnu ; aucun modèle n’est prérempli comme détecté. Faire les premiers essais à l’arrêt. La simulation peut être lancée sans boîtier depuis Accueil ; son bandeau reste visible sur tous les écrans.

## Architecture

- `core` : Kotlin/JVM, politique de commandes, session sérialisée, décodage OBD CAN formaté par ELM, simulateur indépendant, tests unitaires.
- `app` : transports et découverte Android, MVVM/StateFlow, Compose, stockage Room de définitions et historique, export par le sélecteur Android.
- `app/src/main/assets/toyota-catalog.json` : schéma évolutif et fiches originales de recherche, commandes nulles, modèles compatibles vides.
- `docs/INVENTAIRE.md` : ancien travail retrouvé, preuves, manques explicites.
- `docs/SOURCES.md`, `docs/PROTOCOLES.md`, `docs/VALIDATION.md` : provenance, licences, limites et essais.

L’interface propose Accueil, Diagnostic, Personnalisation, Hybride, Historique et Paramètres, avec thèmes système/clair/sombre. Les journaux restent locaux, exportés seulement à votre initiative. Ils peuvent inclure le VIN. Aucun accès Internet n’est déclaré dans l’application.

## Développement restant

Confirmer le NexLink physique et ses profils ; tester permissions, interruption et reconnexion sur Android 10/12/16 ; capturer les références des ECU de la Corolla 2019 Europe ; obtenir des définitions publiques/autorisées de réglages ; valider la lecture puis l’écriture sur ECU exact avec sauvegarde durable, masque et relecture. Développer BLE GATT seulement avec profil confirmé. Étendre la compatibilité modèle par modèle après preuve. La sauvegarde et restauration réelles de réglages ne sont pas implémentées ; le banc fictif sert uniquement à tester l’historique.

Licence MIT pour le code original. Marque Toyota utilisée pour désigner le périmètre ; projet indépendant. Aucun code ou actif Carista/OBDLink embarqué. Voir THIRD_PARTY.md pour les dépendances et outils.
