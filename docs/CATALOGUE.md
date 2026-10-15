# Contrat du catalogue

Une fiche représente une piste de fonction, sans déclaration de disponibilité. `compatibleModels` et `compatibleYears` sont des compatibilités prouvées, jamais le profil souhaité. Les listes sont vides pour les 13 fiches initiales. Les champs de protocole, ECU, commande de lecture/écriture, valeurs autorisées, valeur par défaut et conditions restent nuls ou vides tant qu’ils ne sont pas établis.

`validation` accepte les états représentés dans le noyau Kotlin : DOCUMENTED (Documenté), TO_VERIFY (À vérifier), READ_VALIDATED (Lecture validée), WRITE_VALIDATED (Écriture validée), INCOMPATIBLE (Non compatible). Une déclaration documentaire n’est pas un résultat d’essai sur véhicule. La compatibilité doit en outre être portée par une preuve spécifique au modèle, au marché, à la référence matérielle/logicielle du calculateur et au firmware d’adaptateur.

Le JSON d’origine est stocké dans Room, avec sa provenance ; le modèle Kotlin peut évoluer indépendamment de l’interface. Cette version lit des fiches sans commandes et ne propose aucun import externe ni moteur d’écriture constructeur. Modifier un statut dans un JSON ne peut pas autoriser un envoi : la liste fermée de `CommandPolicy` ne contient aucune commande OEM.

Pour une contribution future : fournir une source publique ou une observation autorisée, des requêtes/réponses anonymisées, le calculateur exact, l’adressage CAN/ISO-TP, le service et l’identifiant, les octets et masques, le domaine de valeurs, les conditions, la mesure avant/après et la procédure de retour. Commencer par la lecture. Une écriture doit préserver les bits non concernés et sauvegarder la valeur originale durablement avant l’envoi. Aucune fonction de ceinture, freinage, airbags ou stabilité n’entre dans ce catalogue.
