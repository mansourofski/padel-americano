# Padel Americano — MVP 0.1

Premier squelette exécutable de l'application Android Padel Americano.

## Ce qui fonctionne déjà

- saisie libre des joueurs (un nom par ligne) ;
- choix du nombre de terrains ;
- seed reproductible ;
- sélection automatique du moteur :
  - `EXACT` pour `N mod 4 = 0 ou 1` ;
  - `BALANCED` pour `N mod 4 = 2 ou 3` ;
- génération des rounds ;
- gestion des pauses ;
- optimisation des adversaires pour le moteur exact ;
- moteur équilibré multi-restarts pour les configurations imparfaites ;
- métriques de qualité ;
- moteur de classement pur Kotlin ;
- tests unitaires 4 / 5 / 6 / 7 / 8 / 9 / 10 joueurs.

## Stack

- Kotlin 2.4.10
- Android Gradle Plugin 9.4.0
- Gradle 9.6.1
- Jetpack Compose BOM 2026.08.00
- Activity Compose 1.13.0
- Lifecycle 2.11.0
- kotlinx.coroutines 1.11.0
- compileSdk 37
- targetSdk 36
- minSdk 26

## Architecture

```text
app
 ├─ UI Compose
 └─ ViewModel

core:model
 └─ modèles métier purs

core:scheduler
 ├─ ClassicExactScheduler
 ├─ BalancedScheduler
 └─ métriques

core:ranking
 └─ RankingEngine
```

Les modules métier n'ont aucune dépendance Android.

## Ouvrir le projet

1. Décompresser le ZIP.
2. Ouvrir le dossier `padel-americano` dans Android Studio.
3. Laisser Android Studio installer SDK 37 si nécessaire.
4. Sync Gradle.
5. Lancer `app` sur émulateur ou téléphone Android.

## Tests

```bash
./gradlew :core:scheduler:test :core:ranking:test
```

## Étape suivante

Milestone 0.2 :

- Room ;
- Tournament / Round / Match persistés ;
- écran Live ;
- saisie 32 points ;
- correction de score ;
- classement live ;
- reprise du tournoi après fermeture de l'app.

## 📱 Générer l'APK sans Android Studio

Le projet contient maintenant un workflow GitHub Actions :

```text
.github/workflows/build-apk.yml
```

Une fois le projet envoyé sur GitHub, ouvre **Actions → Build Android APK → Run workflow**. Après une compilation réussie, récupère `Padel-Americano-debug.apk` dans la section **Artifacts** du run.

Guide détaillé : [`docs/GITHUB_APK.md`](docs/GITHUB_APK.md)
