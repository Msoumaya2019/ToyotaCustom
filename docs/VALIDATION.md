# Validation 0.4.0 — 15 octobre 2026

Les bilans antérieurs sont conservés dans validation/0.1.0.md, 0.2.0.md et 0.3.0.md.

| Vérification | Résultat |
|---|---|
| Noyau Kotlin/JVM | 46 tests réussis |
| Tests Android sur émulateur Android 15/API 35 | 8 tests réussis |
| Android Lint | 0 erreur, 13 avertissements |
| APK application et tests | Compilation réussie |

Les six tests Android antérieurs couvrent diagnostic/effacement simulés, personnalisation explicitement simulée, rapports masqués, identification standard fictive, migration Room 1 → 2 et persistance après réouverture. Deux nouveaux tests vérifient le rapport GATT : adresse masquée, indicateurs de non-validation, UUID multiples préservés par instance, propriétés brutes et huit bits connus, bits inconnus conservés.

La découverte GATT utilise uniquement connectGatt et discoverServices, puis ferme la connexion. Le contrôle du code confirme l’absence de lecture de valeur, abonnement et écriture de caractéristique/descripteur. Le délai de 20 secondes, les erreurs et l’annulation sont implémentés ; les callbacks radio, les délais réels, la fermeture sur boîtier et l’interopérabilité NexLink ne sont pas exercés par ces tests. Les tests d’export utilisent des données synthétiques et ne constituent pas une observation du NexLink.

Aucun téléphone avec NexLink ni véhicule physique n’a été utilisé. Le transport BLE ELM, les données hybrides constructeur et les réglages Toyota restent non validés et désactivés. Le transport diagnostic Classic, le décodeur et la simulation existants sont conservés. Aucun nouveau service diagnostic ou commande Toyota n’est ajouté. Les 13 fiches Toyota restent À vérifier, sans commandes.

Le rapport BLE est séparé des sessions OBD et reste en mémoire jusqu’à la prochaine inspection ou connexion. Son export explicite masque l’adresse et omet le nom du périphérique. Un inventaire ne prouve ni un service série, ni l’identité d’un ECU, ni la compatibilité Corolla. Voir ESSAI-NEXLINK.md pour la collecte terrain.

La version affichée et celle des rapports proviennent désormais de BuildConfig.VERSION_NAME. Le libellé 015B devient « Indicateur batterie » pour éviter une assimilation non validée à la charge de la batterie Toyota.

Compilation locale : JDK JetBrains 21, Gradle 8.14.3, Kotlin 2.4.0, SDK 36 ; Android 10 minimum. Schéma Room inchangé (version 2). Signature de test, non destinée à une distribution durable. Conserver une même provenance d’APK pour les mises à jour : une signature GitHub Actions peut différer de la signature locale.
