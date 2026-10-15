# Validation du 15 octobre 2026

Version : 0.1.0, Android 10 minimum. Compilation Windows avec JDK JetBrains 21.0.5, Gradle 8.14.3, AGP 8.13.2, Kotlin 2.4.0, Room 2.8.5. Le lecteur de métadonnées Kotlin du processeur Room est explicitement aligné sur 2.4.0 ; cette correction a résolu l’incompatibilité constatée à la première compilation.

## Vérifications terminées

| Vérification | Résultat réel |
|---|---|
| `:core:test` | 16 tests, 0 échec, 0 ignoré |
| `:app:lintDebug` | 0 erreur, 11 avertissements |
| `:app:assembleDebug` | Succès, APK généré |
| `:app:assembleDebugAndroidTest` | Succès, APK de tests généré |
| Installation des deux APK | Succès sur émulateur Android 15, API 35 |
| AndroidJUnitRunner, SimulationTest | 3 tests, 0 échec, durée 61,94 s |
| `apksigner verify --verbose` | Signature APK v2 valide, 1 signataire (debug) |
| Logcat AndroidRuntime pendant les parcours | Aucun plantage retourné |
| Empreintes IPA originaux | Recalculées ; OBDLink et Carista 10.1 identiques aux anciens rapports |

Les tests du noyau couvrent les formules de mesure, réponses négatives et incomplètes, ambiguïtés entre calculateurs, bitmaps PID, compteurs DTC CAN, écho ELM, VIN sur plusieurs lignes, séquences et longueurs tronquées, refus des commandes libres/OEM, confirmation obligatoire, refus d’effacement en mouvement et relecture après effacement simulé.

Les trois tests d’interface exécutés sont `diagnosticBlockedUntilConnected`, `simulatedDiagnosticAndClearReadback` et `simulatedCustomizationCreatesExplicitHistory`. Ils vérifient notamment la désactivation du diagnostic hors connexion, le bandeau de simulation, la confirmation de l’effacement fictif et l’écriture d’un historique explicitement simulé dans Room. L’émulateur était lancé en lecture seule, sans modifier son état sauvegardé.

Les avertissements Lint concernent principalement les versions plus récentes de bibliothèques/outils, des attributs Android conditionnés à l’API, la recommandation KSP et l’icône de lancement non définie. Ils ne sont pas masqués par une baseline. L’icône Android par défaut est utilisée dans cette première version. Aucun test physique n’est remplacé par ces résultats.

## Éléments non testés et travail restant

- **Nexas NexLink physique** : appairage SPP, firmware, qualité ELM, interruptions et reconnexion ; le module est implémenté mais aucun boîtier n’était connecté à ce poste.
- **Corolla 1.8 Hybrid 2019 Europe** : VIN réel, PID annoncés, réponses DTC, mesures et effacement ; aucune commande n’a été envoyée à cette voiture.
- **Autres versions Android** : Android 10/11 et Android 12+ sur téléphones réels, refus/révocation de permissions et conditions radio.
- **Personnalisation Toyota réelle** : aucune lecture ou écriture implémentée ; aucune définition complète validée récupérée des IPA. Les 13 fiches sont des pistes, sans promesse de compatibilité.
- **Sauvegarde/restauration de réglages réels** : à développer avec des références ECU précises et des commandes validées. Le banc fictif n’est pas une validation d’un ECU.
- **Calculateurs et CAN** : identification précise des ECU, références/calibrations, en-têtes CAN et routage constructeur à développer.
- **Hybride Toyota** : DID batterie, températures, blocs, moteurs électriques à documenter et valider. Seul le PID générique 015B est préparé s’il est annoncé.
- **BLE** : découverte disponible, transport GATT non implémenté faute de profil confirmé.
- **Sécurité d’écriture OEM** : futur moteur transactionnel avec preuve de compatibilité, sauvegarde avant envoi et vérification de retour ; aucune écriture OEM n’est autorisée aujourd’hui.
- **Publication durable** : icône propre, clé de signature de distribution, schémas Room exportés et migrations, tests de résilience et distribution d’une version stable à préparer.

## Premier essai sur votre téléphone

Installer l’APK debug, ouvrir Paramètres, autoriser Bluetooth et appairer NEXAS dans Android. Rechercher l’appareil puis utiliser « Connecter via Bluetooth Classic SPP ». Commencer par l’identification et la lecture des défauts/mesures. Exporter ensuite le journal depuis Outils de développement pour vérifier les réponses réelles. L’effacement n’est pas nécessaire pour valider la communication et supprime des informations de diagnostic.

Le workflow GitHub Actions compile, exécute les 16 tests du noyau et Android Lint, puis joint l’APK de test. Les trois tests sur émulateur ont été exécutés localement ; ils ne sont pas lancés par ce workflow. Les journaux locaux bruts et rapports détaillés sont fournis avec la livraison séparée, sans les intégrer à l’APK.
