# Validation 0.2.0 — 15 octobre 2026

La validation 0.1.0 est conservée dans `validation/0.1.0.md`. Cette version conserve l’implémentation originale et la licence MIT, et ajoute le suivi des répondants CAN ainsi qu’un rapport de compatibilité.

## Résultats constatés

| Vérification | Résultat |
|---|---|
| Noyau Kotlin/JVM | 32 tests, 0 échec, 0 ignoré |
| Android Lint | 0 erreur, 13 avertissements |
| APK application et APK de tests | Compilation réussie |
| Signature APK | Signature v2 vérifiée avec apksigner, certificat debug |
| Émulateur Android 15, API 35 | 4 tests réussis en 74.072 secondes |

Environnement local : JDK JetBrains 21.0.5, Gradle 8.14.3, Kotlin 2.4.0, SDK 36, Room 2.8.5. L’émulateur est démarré en lecture seule ; son état sauvegardé n’est pas modifié. Les avertissements Lint concernent des versions de dépendances, attributs dépendants de l’API, recommandations KSP/KTX et l’icône Android par défaut.

## Couverture nouvelle

Les tests CAN vérifient les identifiants 11 et 29 bits avec ou sans espaces, la suppression du padding, le réassemblage ISO-TP entrelacé entre plusieurs sources, le passage du compteur de séquence de F à 0, les messages incomplets, les doublons, les séquences invalides, les trames simples tronquées, les mélanges de formats, les octets malformés d’un second répondant et les valeurs contradictoires. Un test prouve que des réponses OK sans en-têtes réellement reçus n’activent pas la capacité CAN. Le simulateur vérifie VIN, DTC et effacement avec relecture en mode avec en-têtes.

Les quatre tests d’interface vérifient le blocage du diagnostic hors connexion, la lecture et l’effacement simulés des défauts, l’historique simulé, les répondants 7E8/7E9 fictifs, l’indicateur simulation du rapport JSON, le masquage de la réponse VIN, l’inclusion volontaire du VIN, la conservation du rapport après déconnexion et le redémarrage d’une session avec ses propres échanges. Ces adresses sont des fixtures du simulateur ; elles ne sont pas des observations sur votre voiture.

## Limites matérielles et fonctions à développer

Aucun essai n’a été effectué sur un NexLink physique ni sur votre Corolla. La découverte, l’appairage, la mémorisation d’un appareil réel, sa reconnexion et le traitement des interruptions doivent être vérifiés sur téléphone avec le boîtier. Android 10/11 et les refus/révocations de permissions demandent aussi des essais physiques. Les tests sur émulateur ne prouvent pas la compatibilité radio.

Les identifiants CAN observés ne certifient ni la fonction, ni la référence matérielle/logicielle d’un ECU. Aucun balayage de carrosserie ni DID constructeur n’est ajouté. Les 13 familles de personnalisation issues des anciennes analyses restent « À vérifier », avec commandes nulles et compatibilités inconnues. Les écritures OEM, leurs sauvegardes/restaurations, les données hybrides Toyota et le transport BLE GATT restent à développer sur la base de définitions et d’essais propres à la configuration cible. Freinage, airbags, stabilité et avertissements de ceinture restent exclus.

## Premier essai sur Corolla

Installer la 0.2.0 (même identifiant d’application et même clé debug locale que la 0.1.0), autoriser Bluetooth, appairer NEXAS puis sélectionner l’appareil. Commencer par l’identification et les lectures OBD. Dans Paramètres, activer Outils de développement et exporter le rapport de compatibilité JSON. La réponse VIN est masquée par défaut. Après une rupture, le rapport de la session précédente reste exportable séparément. Les deux rapports restent en mémoire jusqu’à la fermeture de l’application ; les exporter pour les conserver. Les journaux texte bruts peuvent contenir des identifiants.

Une réponse valide permet de constater le comportement de cette configuration. Elle ne suffit pas à attribuer une fonction de confort à une adresse CAN. L’effacement n’est pas nécessaire pour tester la communication.

GitHub Actions exécute les 32 tests du noyau et Android Lint, puis publie l’APK en artefact. Les 4 tests d’interface sont exécutés localement sur émulateur. La clé debug de GitHub Actions peut différer de la clé locale : conserver une même provenance d’APK pour les mises à jour de test.
