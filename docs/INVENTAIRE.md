# Inventaire technique — 15 octobre 2026

Recherche effectuée dans Documents/Codex, le chat antérieur « Intégrer les add-ons OEM Toyota » et ses dossiers de travail ; vérification complémentaire des IPA originaux présents dans Downloads. Les anciens travaux ont été lus avant toute création de protocole. Le manifeste `evidence/local-manifest.json` recense 137 fichiers locaux avec tailles et SHA-256. Les chemins y sont relatifs au dossier de travail antérieur, sans nom d’utilisateur. Les empreintes des trois IPA originaux ont aussi été recalculées dans `evidence/original-ipa-hashes.json`. Aucun binaire tiers n’est redistribué.

## Résultats retrouvés

| Source locale (dossier du 14/10/2026/j) | Données effectivement disponibles | Portée |
|---|---|---|
| outputs/analyse-ipa-obdlink.md, preuves-extraction.json ; work/resources | 34 074 ressources décodées, 5 blobs dont Manufacturer, DtcDatabase, VehicleMakes | Ressources binaires présentes ; pas une base Toyota SQLite décodée |
| work/embedded et ressources-extraites.zip | Paquet Manufacturer, 76 fichiers mpid selon le rapport précédent | Format non interprété ; aucune commande de confort exploitable |
| outputs/catalogue-toyota-carista.json et .csv | 803 clés (catégories, réglages, choix, outils) et références natives | Ce ne sont pas 803 fonctions compatibles ; catalogue multi-modèles |
| outputs/symboles-toyota-carista.json ; extraction-toyota-carista.zip | 701 fonctions natives Toyota désassemblées selon le rapport | Présence de logique compilée ; pas de source autorisée à intégrer |
| outputs/verification-donnees-toyota-ios.json et ...ios91.json | Références de 775 identifiants dans les binaires iOS | Confirme les références, pas les valeurs ou les trames |
| outputs/etat-toyota-ios91.json | Définitions complètes extraites : false ; compatibilité Corolla vérifiée : false | Aucune écriture validée |
| work/carista-ios, work/carista-ios91, work/ipa/Payload | Extractions et notes d’analyse encore présentes | Pas de nouvelle extraction systématique des IPA |

IPA originaux retrouvés dans Downloads : `com.obdsol.obdlink-7.4.0-eeveedecrypter.ipa`, `com.prizmos.Carista-10.1.0.ipa`, `Carista OBD2-9.1-decrypted.ipa`. Les variantes modifiées existent aussi mais ne servent pas de référence technique. Les modifications antérieures liées au compte et aux licences ne sont pas réutilisées.

## Informations réutilisables

Les anciennes analyses repèrent les familles verrouillage, confirmations sonores, éclairage, rétroviseurs, vitres, climatisation et données hybrides. Elles motivent 13 fiches rédigées de manière originale dans `toyota-catalog.json`. Les identifiants d’origine y sont uniquement des pointeurs de provenance ; une liste vide signifie qu’aucune clé concordante n’a été identifiée par la recherche textuelle. Cela ne prouve pas la présence de la fonction.

Symboles OBDLink observés dans les notes : ToyotaEnhancedCoding, ToyotaDatabase, GetReadCustomizationMessage, GetWriteCustomizationMessage. Ils établissent l’existence d’une architecture de personnalisation ; ils n’établissent aucun octet de requête.

Les éléments de ceinture, ABS, stabilité et airbags sont exclus du catalogue intégré. Les traductions, bibliothèques natives, désassemblages et blobs propriétaires ne sont pas copiés dans l’application. Les constats d’interopérabilité et les références de provenance sont séparés de l’implémentation originale MIT.

## Données manquantes, sans équivoque

- Aucune table décodée associant Corolla 2019 Europe → référence ECU → fonction → masque de bits → valeurs.
- Aucune commande de lecture ou d’écriture Toyota complète dont la provenance et la compatibilité soient établies par les anciens rapports.
- Aucun en-tête CAN, DID, identifiant de personnalisation, temporisation constructeur ou session UDS réutilisable confirmé dans ces résultats.
- Aucun jeu de données Toyota Enhanced OBDLink téléchargé avec manifeste autorisé ; les blobs mpid ne sont pas interprétés.
- Aucun journal NexLink/Corolla, aucune réponse réelle et aucune référence de calculateur de votre voiture.
- Aucun UUID de service série BLE NexLink confirmé. La connexion Android Classic SPP est l’option prioritaire documentée par la notice.

Statut des 13 fiches : **À vérifier**, commandes nulles, compatibilités vides. Aucun élément n’est marqué « Lecture validée » ou « Écriture validée ». « Non compatible » nécessite une preuve négative propre à une configuration ; une donnée inconnue reste inconnue.

Une recherche ciblée complémentaire dans `work/resources/manifest.json` retrouve des libellés de statuts UDS et OBDonUDS ; il s’agit de noms de ressources, pas de définitions de trames ou de preuve de prise en charge Toyota. Aucune chaîne ATZ/ATE0/ATSP/ATSH/ATST exploitable n’a été retrouvée par cette recherche ciblée dans les fichiers JSON examinés.
