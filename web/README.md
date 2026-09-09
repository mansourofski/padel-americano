# Padel Americano — application web

Application statique sans dépendance ni backend. Les parties restent dans le navigateur (`localStorage`). Six formats : Americano, Team Americano, Americano mixte, Mexicano, Team Mexicano et Mixicano. Quatre règles de score, timer par vague de terrains, historique, replay, export/import JSON web, partage texte, corrections tracées, retraits entre manches, thème sombre et grand écran.

## Exécution

Avec Node 24 : `node serve.mjs` (http://localhost:5173), `node --test tests/*.test.mjs`, `node build.mjs`.

Publier le contenu de `dist` sur un hébergement statique HTTPS, par exemple Cloudflare Pages gratuit. Aucune clé ni serveur à configurer. L'adresse localhost est réservée à cet ordinateur et ne suffit pas à installer la PWA sur iPhone.

## iPhone

Ouvrir l'adresse HTTPS dans Safari, Partager → Sur l'écran d'accueil. Ouvrir une première fois en ligne pour mettre le shell en cache. Le bouton Démarrer active le contexte audio ; tester le son dans Outils. Le navigateur peut suspendre l'audio en arrière-plan ou écran verrouillé. L'application recalcule l'échéance à la réouverture, sans garantir un signal sonore en arrière-plan. Le verrouillage d'écran est évité uniquement si l'API Wake Lock est disponible et autorisée.

## Données et limites

- Exporter régulièrement : l'effacement des données du navigateur efface l'historique. Pas de synchronisation entre appareils. Éviter de saisir simultanément dans plusieurs onglets (la dernière sauvegarde gagne).
- Le format d'export web v1 n'est pas l'export Android v4 : pas de migration automatique annoncée.
- Les modes tournants réduisent les répétitions sans garantir un calendrier combinatoire exact pour tout effectif. Les équipes fixes Americano rencontrent chaque adversaire une fois ; les variantes Mexicano calculent la prochaine manche selon le classement.
- Les parties privées hébergées par Sites peuvent demander une connexion au propriétaire. Le mode hors connexion doit encore être vérifié sur un véritable iPhone, notamment en présence de cette protection d'accès.
- Tests de logique exécutés. Pas de validation visuelle ou d'essai matériel iOS effectué dans cette tâche.
- WebMCP est optionnel et détecté à l'exécution. Pas de contexte de validation WebMCP disponible ; non vérifié.

L'application Android existante reste indépendante de ce dossier.
