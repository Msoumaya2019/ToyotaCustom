# Toyota Custom — Android 0.5.0

Application native Kotlin/Compose, Android 10 minimum, française, locale et sans abonnement. Version de développement : diagnostic OBD CAN standard, suivi des répondants CAN et transport Bluetooth Classic implémentés ; compatibilité réelle NexLink/Corolla non testée. Aucun réglage constructeur Toyota ne peut être lu ou modifié dans cette version.

## Démarrer

Ouvrir ce dossier dans Android Studio. Installer SDK 36 et JDK 17 ou supérieur (JDK 21 utilisé localement). Le wrapper Gradle est fourni. `local.properties` est propre à la machine et ignoré par Git.

```powershell
.\gradlew.bat :core:test :app:assembleDebug :app:lintDebug
```

APK : `app/build/outputs/apk/debug/app-debug.apk`. Signature debug pour essais, pas une clé de distribution durable. Le workflow GitHub Actions fait les mêmes vérifications et publie l’APK en artefact, sans secrets de signature.

Sur téléphone : installer l’APK, ouvrir Paramètres, autoriser Bluetooth, appairer NEXAS dans Android (notice : PIN 1234), rechercher puis choisir l’appareil associé. Le bouton de connexion lance l’initialisation ELM et la lecture VIN/PID. Un VIN inconnu reste inconnu ; aucun modèle n’est prérempli comme détecté. Faire les premiers essais à l’arrêt. La simulation peut être lancée sans boîtier depuis Accueil ; son bandeau reste visible sur tous les écrans.

## Architecture

- `core` : Kotlin/JVM, politique de commandes, session sérialisée, décodage OBD et réassemblage ISO-TP avec identifiants CAN 11/29 bits, simulateur indépendant, tests unitaires.
- `app` : transports et découverte Android, MVVM/StateFlow, Compose, stockage Room de définitions et historique, export par le sélecteur Android.
- `app/src/main/assets/toyota-catalog.json` : schéma évolutif et fiches originales de recherche, commandes nulles, modèles compatibles vides.
- `docs/INVENTAIRE.md` : ancien travail retrouvé, preuves, manques explicites.
- `docs/SOURCES.md`, `docs/PROTOCOLES.md`, `docs/VALIDATION.md` : provenance, licences, limites et essais.

L’interface propose Accueil, Diagnostic, Personnalisation, Hybride, Historique et Paramètres, avec thèmes système/clair/sombre. Les journaux restent locaux, exportés seulement à votre initiative. Ils peuvent inclure le VIN. Aucun accès Internet n’est déclaré dans l’application.

## Développement restant

Confirmer le NexLink physique et ses profils ; tester permissions, interruption et reconnexion sur Android 10/12/16 ; capturer les références des ECU de la Corolla 2019 Europe ; obtenir des définitions publiques/autorisées de réglages ; valider la lecture puis l’écriture sur ECU exact avec sauvegarde durable, masque et relecture. Développer BLE GATT seulement avec profil confirmé. Étendre la compatibilité modèle par modèle après preuve. La sauvegarde et restauration réelles de réglages ne sont pas implémentées ; le banc fictif sert uniquement à tester l’historique.

Licence MIT pour le code original. Marque Toyota utilisée pour désigner le périmètre ; projet indépendant. Aucun code ou actif Carista/OBDLink embarqué. Voir THIRD_PARTY.md pour les dépendances et outils.


## Nouveautés 0.2.0

- Vérification du format avec en-têtes CAN reçus, suivi des adresses répondantes, réponses ISO-TP entrelacées. Les identifiants ne sont pas des identités ECU certifiées.
- Repli sur le format sans en-tête si la vérification échoue ; aucun changement d’adressage constructeur ni ajout de commande OEM.
- Rapport de compatibilité JSON avec commandes/réponses corrélées, capacités observées et indicateur simulation. La réponse VIN est masquée par défaut ; l’inclusion des identifiants nécessite de cocher l’option d’export.
- Rapport conservé après déconnexion et dernière session précédente conservée avec VIN masqué lors d’une nouvelle connexion. Les rapports immédiats restent en mémoire ; les sauvegardes durables ajoutées en 0.3 sont décrites ci-dessous.
- Appareil ayant communiqué mémorisé ; reconnexion automatique à son adresse après une recherche autorisée (option désactivable). Aucun appareil n’est choisi sur la seule base du nom NEXAS.
- Gestion des erreurs de recherche BLE, des changements d’appairage et protection contre les résultats tardifs d’une session annulée.

Pour préparer une validation sur Corolla : Paramètres → Outils de développement → Exporter le rapport de compatibilité JSON. Ce rapport décrit uniquement les observations ; aucun état « écriture validée » n’en est déduit.


## Nouveautés 0.3.0

Le bouton « Lire les identifiants OBD des calculateurs » lit les capacités standard du mode 09 (0900), puis demande uniquement les informations annoncées parmi CALID (0904), CVN (0906) et nom déclaré (090A). Les réponses sont associées à leur identifiant CAN ; aucune adresse n’est assimilée d’office à un moteur ou à un équipement Toyota. Les en-têtes vérifiés et l’identification Toyota initiale sont requis. Une information déclarée n’authentifie pas le calculateur et n’active pas de personnalisation.

Les rapports JSON masqués sont désormais enregistrés dans Room après la connexion, la lecture des codes défaut ou l’identification des calculateurs terminées. Une sauvegarde manuelle est aussi disponible dans les outils de développement. L’onglet Historique affiche les 50 rapports les plus récents et permet de les exporter après redémarrage de l’application. Une sauvegarde ultérieure d’une même session actualise son rapport. Les anciennes entrées d’historique sont préservées par la migration de base 1 → 2.

Les exports immédiats incluant volontairement le VIN et les journaux bruts restent des actions distinctes. Les sauvegardes de rapports conservent le masquage du VIN, indépendamment de l’option d’export immédiat.


## Nouveautés 0.4.0 — inventaire BLE

Dans Paramètres, recherchez les appareils. Pour un appareil réellement observé en BLE, « Examiner les services BLE (sans écriture) » établit une connexion GATT temporaire, découvre les UUID des services, caractéristiques et descripteurs, puis ferme la connexion. Les instances et propriétés brutes sont conservées. Aucun UUID série NexLink n’est présumé. Aucune valeur de caractéristique n’est lue, aucun abonnement ni écriture GATT n’est effectué, aucune commande ELM/CAN n’est envoyée.

L’opération est bloquée pendant une connexion OBD ou une autre opération ; elle est annulable et limitée à 20 secondes. La radio NexLink n’a pas été testée ici. Une découverte réussie n’établit pas un protocole série ni la compatibilité véhicule. Le transport diagnostic reste Classic SPP.

Le résultat reste en mémoire jusqu’à la prochaine inspection ou connexion. « Exporter l’inventaire BLE (adresse masquée) » permet de le conserver séparément des rapports OBD ; le fichier ne contient ni adresse Bluetooth, ni nom de l’appareil, ni VIN. Les UUID propriétaires peuvent néanmoins caractériser un produit. Ne pas désinstaller l’ancienne application pour mettre à jour avec un APK de même signature.


## Nouveautés 0.5.0 — mesures attribuées aux sources CAN

Diagnostic affiche désormais les mesures séparément pour chaque identifiant CAN observé, avec leur date de réception. Sans en-tête, aucune adresse n’est inventée. Une réponse tronquée, trop longue, négative ou contradictoire pour une même source est affichée comme indisponible. Les valeurs différentes de deux sources restent distinctes ; aucune source n’est choisie arbitrairement.

Les rapports JSON ajoutent liveMeasurements (PID, source, date, valeur ou erreur), sauvegardé avec le rapport de session après une actualisation terminée. Il s’agit des dernières mesures, pas d’un enregistreur de séries temporelles. Les requêtes restent fonctionnelles : aucune adresse physique constructeur n’est ciblée et aucun PID supplémentaire n’est ajouté.

Après déconnexion, les dernières mesures sont conservées pour l’export avec leur date et un libellé explicite de session déconnectée. Une nouvelle connexion réinitialise la liste. Pendant une nouvelle actualisation, les anciennes mesures sont retirées et les résultats sont ajoutés au fur et à mesure. La valeur de consensus interne, utilisée pour les contrôles d’effacement, n’existe que si toutes les réponses décodées sont valides et identiques. Les contrôles vitesse/régime restent des lectures fraîches, indépendantes de l’écran.
