# Sources étudiées et licences — consultation du 15/10/2026

L’étude porte sur les README, licences et sources publiques ciblées. Les clones locaux ont échoué (résolution réseau du shell) ; la consultation web a fonctionné. Aucun code, catalogue ou fixture des dépôts suivants n’a été copié.

| Projet | Licence constatée | Approche et décision |
|---|---|---|
| [Toyota Diagnostics](https://github.com/kaikozlov/toyota-diagnostics) | Aucun fichier LICENSE visible à la racine consultée ; aucune licence dans pyproject.toml | Résolution véhicule/ECU et métadonnées issues de GTS+, transport Panda/openpilot. Pas une validation ELM/NexLink/Corolla. Réutilisation de code et de métadonnées différée jusqu’à clarification de licence et provenance. |
| [AndrOBD](https://github.com/fr3ts0n/AndrOBD) | [GPL v3](https://github.com/fr3ts0n/AndrOBD/blob/master/LICENSE) | Diagnostic Android ELM, utile pour comparaison de sessions et essais matériels indépendants. Aucune dépendance liée au projet MIT. |
| [ELM327 Emulator](https://github.com/Ircama/ELM327-emulator) | [CC BY-NC-SA 4.0](https://github.com/Ircama/ELM327-emulator/blob/master/LICENSE) | Simulation multi-ECU. La restriction NC empêche de traiter cette licence comme libre au sens OSI ; pas embarqué ni adapté. Simulateur interne original, pas de prétention de tests contre cet outil. |
| [python-OBD](https://github.com/brendan-w/python-OBD) | [GPL v2, fichiers indiquant v2 ou ultérieure](https://github.com/brendan-w/python-OBD/blob/master/obd/decoders.py) | Initialisation série, PID et séparation CAN/ISO-TP ; contrôle de concordance des formats, sans traduction de son code. |
| [opendbc](https://github.com/commaai/opendbc) | [MIT](https://github.com/commaai/opendbc/blob/master/LICENSE) | CAN et empreintes ECU Toyota ; l’accès Panda/harness diffère d’un ELM327 sur prise OBD. Les messages de contrôle ADAS sont hors périmètre. Aucune donnée importée. |

## Concordances et limites

Le [datasheet ELM327](https://www.elmelectronics.com/wp-content/uploads/2017/01/ELM327DS.pdf) documente ATZ, ATE0, ATL0, ATS1, ATH0, ATSP0, ATAT1, ATST et ATDP/ATDPN. L’initialisation publique de [python-OBD](https://github.com/brendan-w/python-OBD/blob/master/obd/elm327.py) confirme l’approche réinitialisation/écho/protocole et l’attente de l’invite `>`. Cette concordance ne valide pas les clones ELM.

Le format des réponses CAN aux modes DTC inclut un octet de nombre de défauts après le service : ce point est également traité dans [protocol_can.py](https://github.com/brendan-w/python-OBD/blob/master/obd/protocols/protocol_can.py). Les modes 03/07/0A sont décodés avec ce compteur dans notre implémentation originale limitée au CAN.

Les ressources IPA apportent des noms de fonctions mais aucune commande complète à comparer. On ne peut donc annoncer aucune concordance de trame constructeur entre ces IPA et les projets publics. Les références UDS du projet Toyota Diagnostics ne suffisent pas à identifier un DID applicable à la Corolla cible.

La [notice NEXAS distribuée avec le produit](https://m.media-amazon.com/images/I/B1krMcya2yL.pdf) indique pour Android un appairage NEXAS, PIN 1234, et Bluetooth Classic ; le matériel et son firmware doivent confirmer SPP. Les profils BLE ne sont pas déduits du nom commercial.

Les [permissions Android officielles](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions) guident la demande SCAN/CONNECT sur Android 12+, et la localisation pour la découverte au premier plan sur Android 10/11.


## Extension CAN 0.2.0

Le [datasheet ELM327](https://www.elmelectronics.com/wp-content/uploads/2017/01/ELM327DS.pdf), sections CAF0/CAF1, D0/D1 et H0/H1 (pages 14, 17 et 18), précise qu’avec H1 les octets PCI reçus redeviennent visibles, tandis que CAF1 conserve le formatage des requêtes. D0 désactive l’affichage du DLC. La nouvelle implémentation originale reconstruit ces réponses ; le boîtier doit encore prouver ce comportement sur le matériel réel. Aucun UUID NexLink ou DID constructeur nouveau n’est déduit de cette documentation.
