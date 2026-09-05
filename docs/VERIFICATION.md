# Verification report — MVP 0.1

Date: 2026-09-03

## What was executed in the build environment

The pure Kotlin domain modules were compiled with `kotlinc`:

- `core:model`
- `core:scheduler`
- `core:ranking`

Result: **OK**.

A property-style smoke harness generated schedules for every player count from 4 through 20 and for seeds 0 through 4.

Result:

```text
Validated 85 schedule cases for N=4..20 and seeds=0..4
```

Validated invariants:

- exactly 4 distinct players per match;
- a player never appears twice in the same round;
- a bye player is never active in the same round;
- games per player are equal with the default schedule length;
- byes per player are equal;
- EXACT schedules have zero repeated partnerships;
- EXACT schedules have 100% partner coverage.

The ranking engine was also compiled and exercised with an 18-14 match.

Result:

```text
Ranking engine validation OK
```

## Android build limitation of this execution environment

This runtime does not contain an Android SDK, so the Android `app` module itself was not assembled here.
The project is prepared for Android Studio with `compileSdk 37`, `targetSdk 36`, and a Gradle bootstrap wrapper.

The next verification on a development machine should run:

```bash
./gradlew :core:scheduler:test :core:ranking:test :app:assembleDebug
```
