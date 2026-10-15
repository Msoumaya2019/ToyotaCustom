# Communication implémentée

Transport réel Android : Bluetooth Classic RFCOMM sécurisé, UUID SPP standard 00001101-0000-1000-8000-00805f9b34fb. Cet UUID est celui du profil public, pas un UUID NexLink découvert. Appairage Android nécessaire ; découverte Classic et BLE au premier plan. Connexion 15 s, réponse 12 s, première interrogation 0100 jusqu’à 30 s. Fermeture du socket lors d’un délai dépassé pour éviter de réutiliser une réponse tardive. Deux nouvelles tentatives initiales, aucune répétition automatique d’effacement.

Initialisation : ATZ, ATE0, ATL0, ATS1, ATH0, ATSP0, ATAT1, ATSTFF. Chaque AT après reset doit recevoir OK. ATI lit la chaîne déclarée par l’adaptateur, qui ne certifie pas son authenticité. 0100 provoque la recherche du protocole ; ATDP/ATDPN rapportent le résultat. Seuls 6, 7, 8, 9 (CAN ISO 15765-4) sont acceptés par cette version.

ELM assure la segmentation et le contrôle de flux ISO-TP via son formatage automatique par défaut après reset. Les en-têtes restent désactivés ; le décodeur accepte les réponses positives formatées et la numérotation ELM des longs messages. Les identifiants CAN sources ne sont donc pas présentés comme connus. Aucun routage vers un calculateur de carrosserie n’est essayé. Une future version devra activer et décoder les en-têtes pour certifier chaque ECU et sa référence logicielle.

PID : 0100 puis blocs 0120/0140/0160 si annoncés. Lectures disponibles si annoncées : 0104 charge, 0105 température, 010C régime, 010D vitesse, 0111 papillon, 0142 tension, 015B charge générique batterie. Formules : A*100/255, A-40, (256A+B)/4, A, A*100/255, (256A+B)/1000, A*100/255. 015B ne vaut pas une validation de l’état de charge Toyota ni des blocs de batterie.

0902 : VIN, 17 caractères, service 49/PID02/compteur01, assemblage ELM. WMI Toyota limité à une liste explicite : l’absence dans cette liste n’est pas une preuve d’une autre marque. Le modèle, l’année et la version ne sont pas déduits du profil demandé par l’utilisateur. Diagnostic réel bloqué sans WMI reconnu ; aucune écriture constructeur dans tous les cas.

03/07/0A : DTC mémorisés/en attente/permanents, réponses 43/47/4A, compteur puis paires de codes. Pas de descriptions propriétaires ni de diagnostic de cause présumé. 04 : effacement standard confirmé, WMI Toyota reconnu, vitesse et régime lus à zéro immédiatement avant l’envoi, réponse 44 requise puis relecture 03. L’utilisateur confirme le contact mis, mode READY désactivé et frein de stationnement ; le logiciel ne prétend pas mesurer ces trois conditions. Aucune tentative de forcer l’effacement de DTC permanents.

Sécurité : liste fermée, mutex, aucune console arbitraire. Services UDS 10/27/2E/31, routines, codages, fonctions ABS/airbag/ceinture non exposés. La personnalisation réelle est désactivée structurellement : aucun appel transport pour un réglage. Le simulateur ne contacte aucun Bluetooth et marque ses données comme fictives.
