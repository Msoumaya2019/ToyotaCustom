# Essai terrain NexLink — version 0.5.0

Ce protocole prépare les premières observations réelles. Aucun essai radio ou Corolla n’a été effectué pendant le développement. Ne pas confondre les données SIMULATION avec les observations terrain.

## Inventaire BLE du boîtier

1. Mettre le boîtier sous tension conformément à sa notice. Conserver le véhicule immobilisé.
2. Ouvrir Paramètres → Rechercher et autoriser le Bluetooth. Accepter les autorisations Android nécessaires. Attendre l’apparition de l’appareil. Le nom « NEX » ne prouve pas l’identité d’un appareil : contrôler celui effectivement utilisé.
3. Si l’appareil est marqué « Détecté en BLE », choisir « Examiner les services BLE (sans écriture) ». L’examen est disponible hors connexion OBD. Il ne nécessite pas d’appairage préalable imposé par l’application ; Android ou le boîtier peuvent toutefois refuser la connexion.
4. Attendre au maximum 20 secondes ou utiliser « Déconnecter / annuler ». Une erreur doit être conservée telle quelle ; elle ne prouve pas l’absence définitive de BLE.
5. Si des services sont découverts, exporter immédiatement l’inventaire BLE. Il n’est pas sauvegardé automatiquement. Les caractéristiques, propriétés et descripteurs annoncés sont listés ; aucune valeur n’est lue et aucune caractéristique n’est écrite.

L’adresse est visible localement pour identifier le boîtier mais absente du fichier JSON. Le rapport ne contient pas de nom d’appareil ou de VIN. Ses UUID peuvent décrire un produit. Il est distinct des rapports OBD et ne vaut pas validation d’un transport ELM sur GATT. Si le boîtier n’apparaît qu’en appareil associé/Classic, aucune inspection BLE n’est proposée : ne pas inventer un UUID pour le forcer.

## Diagnostic Classic existant

1. Appairer l’appareil dans les paramètres Bluetooth Android selon sa notice.
2. Rechercher dans l’application et sélectionner « Connecter via Bluetooth Classic SPP ».
3. Vérifier les informations réelles : version adaptateur, protocole, VIN/WMI. Le modèle Corolla et l’année restent à confirmer ; le profil souhaité affiché à l’accueil n’est pas une identification.
4. Dans Diagnostic, lire les codes défaut puis, si le bouton est disponible, les identifiants OBD des calculateurs. Ne pas effacer les défauts pour cette première collecte.
5. Dans Historique, exporter le rapport sauvegardé (VIN masqué). Conserver séparément l’inventaire BLE, la référence exacte du boîtier, la version Android et les conditions de l’essai.

## Informations manquantes pour progresser

Les anciens IPA et rapports ont été réutilisés dans l’inventaire du projet, mais n’établissent ni les UUID série de ce NexLink, ni une commande de réglage vérifiée sur cette Corolla. Une observation réelle GATT peut guider l’étude d’interopérabilité ; elle ne suffit pas à autoriser une écriture. Les noms/CALID/CVN obtenus en OBD restent des déclarations des répondants. Aucune fonction Toyota n’est activée automatiquement à partir de ces seules informations.


## Mesures par source

Après une connexion OBD réussie, ouvrir Diagnostic → Actualiser les mesures. Vérifier les PID effectivement annoncés, la source CAN de chaque mesure et la date de réception. Le régime et la vitesse doivent correspondre aux conditions réelles ; une réponse inhabituelle ou indisponible doit être conservée telle quelle, sans forcer une valeur. Les adresses sont des répondants observés et non une identification des calculateurs Toyota.

Exporter ensuite le rapport sauvegardé depuis Historique. La section liveMeasurements contient les dernières valeurs ou erreurs, par source. Deux sources peuvent déclarer des valeurs différentes : ne pas choisir arbitrairement celle qui paraît plausible. Sans en-tête vérifié, les sources restent inconnues. Ce test ne nécessite aucun effacement ni modification. Les mesures conservées après déconnexion sont étiquetées comme anciennes ; une nouvelle connexion les réinitialise.
