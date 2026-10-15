# Communication implémentée

Transport réel Android : Bluetooth Classic RFCOMM sécurisé, UUID SPP standard 00001101-0000-1000-8000-00805f9b34fb. Cet UUID est celui du profil public, pas un UUID NexLink découvert. Appairage Android nécessaire ; découverte Classic et BLE au premier plan. Connexion 15 s, réponse 12 s, première interrogation 0100 jusqu’à 30 s. Fermeture du socket lors d’un délai dépassé pour éviter de réutiliser une réponse tardive. Deux nouvelles tentatives initiales, aucune répétition automatique d’effacement. Après une rupture détectée lors d’une opération, une nouvelle connexion peut être tentée sans rejouer cette opération. L’adresse du dernier appareil ayant communiqué est conservée ; après une recherche de 12 secondes, la connexion automatique utilise uniquement cette adresse si elle est retrouvée et associée, et si l’option est active.

Initialisation : ATZ, ATE0, ATL0, ATS1, ATH0, ATSP0, ATAT1, ATSTFF. Chaque AT après reset doit recevoir OK. ATI lit la chaîne déclarée par l’adaptateur, qui ne certifie pas son authenticité. 0100 provoque la recherche du protocole ; ATDP/ATDPN rapportent le résultat. Seuls 6, 7, 8, 9 (CAN ISO 15765-4) sont acceptés par cette version.

Après confirmation du protocole CAN, la session essaie ATCAF1, ATD0 et ATH1. La prise en charge n’est reconnue qu’après une réponse positive 0100 décodable avec un identifiant CAN ; un simple OK ne suffit pas. En cas de refus ou de format invalide, ATH0 doit être confirmé avant de conserver le mode sans en-tête.

ELM continue de formater les requêtes et d’assurer le contrôle de flux. Le récepteur reconstruit les réponses ISO-TP normales de CAN classique, avec identifiants 11 ou 29 bits, trames simples et messages jusqu’à 4095 octets. Il suit séparément les séquences par identifiant, y compris le passage de F à 0, et refuse les trames tronquées, incomplètes, dupliquées, les mélanges de formats et les séquences incorrectes. CAN FD et adressage étendu ne sont pas pris en charge.

Les adresses sont affichées comme « répondants CAN observés ». Aucun nom de calculateur ou modèle n’est déduit d’une adresse seule. Aucun balayage de carrosserie, changement de filtre ou adressage constructeur n’est envoyé. La liste des PID supportés reste l’union des réponses standard ; une mesure ambiguë entre répondants reste indisponible, notamment pour les contrôles avant effacement.

PID : 0100 puis blocs 0120/0140/0160 si annoncés. Lectures disponibles si annoncées : 0104 charge, 0105 température, 010C régime, 010D vitesse, 0111 papillon, 0142 tension, 015B charge générique batterie. Formules : A*100/255, A-40, (256A+B)/4, A, A*100/255, (256A+B)/1000, A*100/255. 015B ne vaut pas une validation de l’état de charge Toyota ni des blocs de batterie.

0902 : VIN, 17 caractères, service 49/PID02/compteur01, assemblage ELM. WMI Toyota limité à une liste explicite : l’absence dans cette liste n’est pas une preuve d’une autre marque. Le modèle, l’année et la version ne sont pas déduits du profil demandé par l’utilisateur. Diagnostic réel bloqué sans WMI reconnu ; aucune écriture constructeur dans tous les cas.

03/07/0A : DTC mémorisés/en attente/permanents, réponses 43/47/4A, compteur puis paires de codes. Pas de descriptions propriétaires ni de diagnostic de cause présumé. 04 : effacement standard confirmé, WMI Toyota reconnu, vitesse et régime lus à zéro immédiatement avant l’envoi, réponse 44 requise puis relecture 03. L’utilisateur confirme le contact mis, mode READY désactivé et frein de stationnement ; le logiciel ne prétend pas mesurer ces trois conditions. Aucune tentative de forcer l’effacement de DTC permanents.

Sécurité : liste fermée, mutex, aucune console arbitraire. Services UDS 10/27/2E/31, routines, codages, fonctions ABS/airbag/ceinture non exposés. La personnalisation réelle est désactivée structurellement : aucun appel transport pour un réglage. Le simulateur ne contacte aucun Bluetooth et marque ses données comme fictives.


## Rapport de compatibilité

`toyota-custom-session-v1` contient le mode simulation, le protocole, la vérification des en-têtes, les adresses CAN observées, les PID supportés et au plus 200 échanges corrélés par commande et horodatage. Le VIN en clair et l’adresse Bluetooth sont omis par défaut, et la réponse à 0902 est entièrement remplacée. Les journaux texte bruts restent un export distinct pouvant contenir des identifiants. Le rapport ne certifie jamais le modèle du véhicule ni l’identité d’un ECU. Une nouvelle connexion repart avec des observations vides ; le rapport de la session précédente est séparé et conserve son indicateur de simulation.


## Informations standard par répondant — 0.3.0

Sur demande, 0900 retourne les informations annoncées pour le mode 09. Le format pris en charge est 49 00 suivi de quatre octets de bitmap, attribué à un identifiant CAN visible. Les requêtes 0904, 0906 et 090A sont envoyées seulement lorsqu’au moins un répondant les annonce. Ce sont des requêtes fonctionnelles OBD ; l’application n’envoie aucun nouveau routage constructeur.

Les formats décodés sont 49 + InfoType + nombre d’éléments, puis respectivement 16 octets par CALID, 4 par CVN et 20 pour un nom unique. Les longueurs, compteurs et caractères sont vérifiés. Les octets nuls intérieurs des textes sont rendus visibles, afin de ne pas concaténer artificiellement des identifiants. Une réponse incohérente, absente ou contradictoire produit une observation d’erreur ; aucun identifiant supposé n’est substitué. Les nombres CALID/CVN différents ne donnent pas lieu à une association présumée.

L’adresse, le nom déclaré, les identifiants de calibration et les CVN sont des observations, pas une preuve d’authenticité du firmware, d’identité matérielle complète ou de compatibilité Toyota. Les conditions et commandes de personnalisation restent inconnues.

Le format de rapport conserve sa version compatible `toyota-custom-session-v1` et ajoute `sessionId`, `sessionStartedAt` et `declaredStandardInfo`. Les rapports masqués sont sauvegardés dans `session_reports`. Les exports bruts ou incluant le VIN ne sont pas automatiquement stockés dans cette table. La migration 1 → 2 crée cette table sans réinitialiser `history` ni `definitions` ; aucune migration destructive n’est configurée.
