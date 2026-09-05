# Compiler l'APK avec GitHub Actions

Aucune installation Android Studio n'est nécessaire pour cette méthode.

## 1. Créer un dépôt GitHub

1. Connecte-toi à GitHub.
2. Clique sur **New repository**.
3. Donne-lui par exemple le nom `padel-americano`.
4. Choisis **Private** si tu ne veux pas rendre le code public.
5. Crée le dépôt sans ajouter de README, `.gitignore` ou licence.

## 2. Envoyer le projet

Le contenu du dossier `padel-americano` doit se trouver à la racine du dépôt GitHub.

La racine doit notamment contenir :

```text
.github/
app/
core/
gradle/
build.gradle.kts
settings.gradle.kts
gradlew
gradlew.bat
```

Tu peux envoyer les fichiers depuis l'interface GitHub avec **Add file > Upload files**, ou avec Git.

## 3. Lancer la compilation

Une compilation est lancée automatiquement après un push sur `main` ou `master`.

Pour la lancer manuellement :

1. Ouvre le dépôt sur GitHub.
2. Va dans l'onglet **Actions**.
3. Ouvre **Build Android APK**.
4. Clique sur **Run workflow**.
5. Sélectionne la branche.
6. Clique sur **Run workflow**.

## 4. Télécharger l'APK

Quand le job **Build debug APK** est vert :

1. Ouvre le run terminé.
2. Descends jusqu'à la section **Artifacts**.
3. Télécharge `Padel-Americano-debug.apk`.
4. Envoie le fichier sur ton téléphone Android si nécessaire.
5. Ouvre le fichier sur le téléphone et autorise l'installation depuis cette source si Android le demande.

L'APK est un APK **debug signé automatiquement**, suffisant pour tester l'application sur un téléphone.

## 5. À chaque nouvelle version

Après une modification du code :

```text
push GitHub
→ GitHub Actions
→ tests
→ compilation
→ nouvel APK
```

Le workflow conserve l'APK pendant 30 jours.

## Workflow

Le fichier utilisé est :

```text
.github/workflows/build-apk.yml
```

Il :

- installe Java 17 ;
- installe le SDK Android API 37 ;
- exécute les tests unitaires ;
- compile `:app:assembleDebug` ;
- publie directement `Padel-Americano-debug.apk` comme artifact GitHub.
