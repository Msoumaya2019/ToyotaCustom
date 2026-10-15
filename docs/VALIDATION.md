# Validation 0.5.0 — 15 octobre 2026

Les bilans précédents sont conservés dans validation/0.1.0.md à validation/0.4.0.md.

| Vérification | Résultat |
|---|---|
| Noyau Kotlin/JVM | 58 tests réussis, aucun échec ni test ignoré |
| Tests Android sur émulateur Android 15/API 35 | 9 tests réussis |
| Android Lint | 0 erreur, 13 avertissements |
| APK application et tests | Compilation réussie |

## Mesures et contrôles

Les 12 nouveaux tests du noyau couvrent les valeurs différentes par source, le consensus de plusieurs sources identiques, les contradictions persistantes d’une même source, la réponse mal formée mêlée à une réponse valide, les doublons identiques, l’absence d’en-tête, les réponses négatives, les PID inattendus, NO DATA, les tailles exactes et bornes des formules, le refus d’un PID sans décodeur et le blocage de l’effacement en cas de vitesse mal formée ou négative. Les 46 tests antérieurs restent réussis.

Le nouveau parcours Android actualise les mesures dans le simulateur, vérifie l’affichage de la source CAN, les dates, le contenu JSON et leur sauvegarde dans le rapport Room. Il vérifie que les dernières mesures restent conservées après déconnexion, tandis que la valeur de consensus courante est retirée. Les huit tests antérieurs de diagnostic, effacement simulé, historique, rapports, identification, migration et inventaire BLE restent réussis.

Les mesures de sources différentes sont affichées séparément. La valeur commune interne ne peut servir aux contrôles d’effacement que si toutes les réponses décodées sont valides et identiques. Les lectures de sécurité restent fraîches et ne consomment jamais les valeurs mémorisées à l’écran. Une longueur de charge utile incorrecte est refusée, y compris des octets supplémentaires après les données attendues. Le remplissage extérieur à la longueur ISO-TP est traité par le décodeur CAN existant.

## Limites de validation

Ces tests utilisent des fixtures originales et le simulateur interne. Aucun NexLink physique, téléphone relié au boîtier ou véhicule réel n’a été testé. Les identifiants CAN restent des sources observées, sans identité matérielle vérifiée. Aucun nouveau PID, adresse constructeur, DID Toyota ou commande de personnalisation n’est ajouté. Le transport diagnostic reste Classic SPP ; l’inventaire BLE ne constitue pas un transport ELM validé. Les écritures Toyota restent désactivées.

Les réponses sans en-tête restent de source inconnue. Les dernières mesures horodatées sont sauvegardées à l’issue d’une actualisation terminée, dans le rapport existant de session ; il ne s’agit pas d’une série temporelle complète. Après déconnexion, l’interface indique explicitement que les valeurs sont conservées. Une nouvelle connexion les réinitialise. Un balayage interrompu peut laisser des résultats partiels visibles, avec le statut d’erreur et les échanges bruts disponibles.

Compilation locale avec JDK JetBrains 21, Gradle 8.14.3, Kotlin 2.4.0 et SDK 36. Android 10 minimum. Schéma Room inchangé (version 2). Les avertissements Lint existants concernent principalement les dépendances et recommandations Android. La signature est une signature de test ; conserver une même provenance d’APK pour les mises à jour.
