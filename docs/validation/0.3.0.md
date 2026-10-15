# Validation 0.3.0 — 15 octobre 2026

Les bilans précédents sont conservés dans `validation/0.1.0.md` et `validation/0.2.0.md`.

| Vérification | Résultat constaté |
|---|---|
| Noyau Kotlin/JVM | 46 tests réussis, aucun échec ni test ignoré |
| Android Lint | 0 erreur, 13 avertissements |
| APK application et APK de tests | Compilation réussie |
| Signature APK | Signature v2 valide, certificat debug |
| Android 15, API 35, émulateur en lecture seule | 6 tests réussis en 139.448 secondes |
| Migration de base 1 → 2 | Ancien historique et définitions conservés ; validation du schéma par Room réussie |
| Persistance d’un rapport | Rapport conservé après fermeture et réouverture de la base |

Compilation locale avec JDK JetBrains 21.0.5, Gradle 8.14.3, Kotlin 2.4.0, SDK 36 et Room 2.8.5. Les avertissements concernent principalement les versions de dépendances, attributs dépendants de l’API, recommandations KSP/KTX et l’icône Android par défaut.

## Nouvelles vérifications

Les 14 nouveaux tests du noyau couvrent les capacités mode 09 par identifiant CAN, le refus d’une attribution sans en-tête, les bitmaps tronqués ou contradictoires, les CALID multiples, les noms et CVN, les octets nuls intérieurs, les textes invalides ou vides, les longueurs incompatibles, les lectures limitées aux informations annoncées, les réponses absentes et les valeurs contradictoires. Les 32 tests OBD/CAN existants restent réussis.

Les 5 tests de parcours Android couvrent le diagnostic bloqué hors connexion, le diagnostic et l’effacement simulés, l’historique simulé, le masquage du VIN et la séparation des sessions, ainsi que la lecture des noms/CALID/CVN fictifs et l’apparition de rapports sauvegardés. Un sixième test crée une base version 1 selon le schéma des versions 0.1/0.2, la migre avec Room, vérifie l’historique et les définitions, sauvegarde un rapport puis le relit après réouverture. Aucun fichier de données utilisateur existant n’est remplacé par ce test.

## Résultats et limites

Le format pris en charge pour les informations standard est vérifié sur des fixtures originales et le simulateur interne. Les noms et calibrations sont des déclarations de l’ECU : ils ne prouvent pas une référence matérielle complète, un firmware authentique, un modèle Toyota ni une compatibilité de personnalisation. Les formats non pris en charge restent signalés comme inconnus ou invalides.

Aucun NexLink physique ni véhicule réel n’a été utilisé. L’appairage, la radio, les délais, les interruptions et les lectures sur Corolla 1.8 Hybrid 2019 Europe restent à tester. Le transport BLE GATT, l’identification Toyota constructeur précise, les données hybrides spécifiques et les commandes de confort ne sont pas ajoutés. Les 13 fiches Toyota restent « À vérifier » et leurs commandes restent nulles. Les écritures OEM restent désactivées.

## Utilisation de la nouvelle version

Après connexion, ouvrir Diagnostic puis « Lire les identifiants OBD des calculateurs ». Cette fonction requiert un VIN Toyota reconnu et des en-têtes CAN vérifiés. L’application lit 0900 puis seulement les informations annoncées parmi 0904, 0906 et 090A. Elle conserve le lien entre chaque réponse et son adresse CAN.

Les rapports masqués sont sauvegardés à la connexion réussie, après la lecture des codes défaut et après la lecture des identifiants. Les outils de développement permettent aussi une sauvegarde manuelle. Historique affiche les 50 rapports les plus récents et permet de les exporter ; les sauvegardes restent disponibles après fermeture de l’application. Une nouvelle sauvegarde de la même session actualise son rapport. Le VIN reste masqué dans les sauvegardes, même si l’utilisateur choisit de l’inclure dans un export immédiat distinct.

La base passe de la version 1 à 2 sans migration destructive. La mise à jour de l’APK local conserve l’identifiant d’application et la clé debug des versions précédentes ; elle ne nécessite pas de désinstaller l’application. Les APK GitHub Actions peuvent utiliser une clé debug différente : conserver une même provenance pour les mises à jour de test.

GitHub Actions exécute les 46 tests du noyau et Android Lint avant de publier l’APK. Les 6 tests Android sont exécutés localement sur émulateur.
