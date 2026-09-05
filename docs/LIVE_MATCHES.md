# Matchs et scores — 0.2.0

1. Saisir les joueurs, les terrains et le total de points (32 par défaut).
2. Générer le planning puis choisir **Démarrer les matchs**, ou démarrer un match individuellement.
3. Le score apparaît devant chaque équipe. Utiliser **+ / −** pendant le match ou **Saisir le score final**.
4. Valider un résultat dont la somme atteint le total prévu. Les matchs nuls sont acceptés.
5. Quand tous les matchs de la vague sont validés, la vague suivante devient disponible.
6. Consulter l'onglet **Classement**. **Corriger le score** remplace le résultat précédent et recalcule les points.

Le planning exact, les scores en cours et les résultats sont enregistrés localement à chaque modification. Ils sont restaurés après fermeture de l'application. Un seul tournoi est conservé ; **Nouveau tournoi** demande confirmation avant remplacement. Il n'y a pas encore d'historique des corrections ni d'archive multi-tournois.

Les paramètres ne sont plus modifiables après le premier démarrage pour protéger le planning et les scores.

## Vérification

`LiveTournamentTest` couvre le démarrage obligatoire, le double démarrage, le blocage des rounds futurs, les vagues à un terrain, les limites du score, le total configurable, les corrections et les identifiants de matchs entre rounds.

```sh
./gradlew :core:scheduler:test :core:ranking:test :app:assembleDebug
```

Les modules Kotlin demandent un JDK 17 ; le lanceur Gradle demande un JDK récent compatible avec le wrapper existant.

À vérifier sur téléphone : confort des boutons, saisie clavier, rotation de l'écran et reprise après fermeture complète.
