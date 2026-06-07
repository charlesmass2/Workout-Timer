# Workout Timer

A native Android workout-timer app built with **Kotlin** and **Jetpack Compose**.
Track a training session with a dedicated timer per exercise, per rest period, and
a global stopwatch — backed by a detailed history. The UI is a faithful
recreation of the supplied Claude Design prototype (technical / data-focused,
fitness-watch aesthetic, dark theme).

## Features

- **Default workout** — "Full Body Calisthenics":
  - Superset 1: Tuck Planche + Abs, 6 alternating sets each
  - Superset 2: Pull-ups + Push-ups, 6 sets each
  - Plank Hold: 90 s, 1 set
  - Superset 3: Chin-ups + Dips, 6 sets each
  - Defaults: 30 s work, 60 s rest between sets, 120 s rest between blocks.
- **Live engine** — 5 s countdown, then auto-advancing work → rest → work timers
  alongside a global stopwatch.
  - *Finish set* ends the current exercise early and jumps to rest.
  - *Skip rest* ends a rest early and starts the next exercise.
  - *Pause/Resume* freezes both the dedicated timer and the global stopwatch.
- **Always-visible context** — current block (superset) name, the exercise just
  done, the one now, and the one next; plus sets done / remaining.
- **Reps** — optionally log reps during a set; entering a value auto-fills the
  following sets of the same exercise (still editable). At the end you can edit
  every set, with a "Set all" shortcut per exercise.
- **Post-workout review** — rate satisfaction and breathlessness (4 levels each).
- **Detailed history** — every session is saved automatically: date/time, real
  per-set durations vs planned, reps, ratings, planned-vs-actual delta.
- **Sound & vibration** — toggled live from always-visible buttons on the active
  screen; each timer end fires a ~2 s beep / vibration pattern when enabled.
- **Workout library** — create, edit (including the name), and duplicate workouts.
  A workout is one or more *blocks*; a block with several exercises is a superset
  (alternating each round). Per-exercise duration, rest-between-sets, and
  rest-after-block are all configurable. The library shows the estimated session
  duration if all timings are respected.
- **Keeps the screen awake** while a workout is running.

## Project layout

```
app/src/main/java/io/shizen/workouttimer/
├── data/            Models, schedule builder, formatting, SharedPreferences repo
├── timer/           Feedback (sound/vibration) + ActiveController (timer engine)
├── ui/
│   ├── theme/       Colours (oklch→sRGB), fonts, Material theme
│   ├── components/  Icons, buttons, stepper, sheet, dialog, pill
│   └── screens/     Home, History, History detail, Editor, Active, Summary
├── AppViewModel.kt  State, navigation, persistence
└── MainActivity.kt
```

## Build

Requires the Android SDK (compileSdk 35) and a JDK 17+. The first build downloads
the Android Gradle Plugin and Compose dependencies from Google's Maven and Maven
Central, so a network connection is needed.

If the Gradle wrapper jar (`gradle/wrapper/gradle-wrapper.jar`) is not present,
generate it once with a locally installed Gradle (8.x):

```bash
gradle wrapper --gradle-version 8.11.1
```

Then build:

```bash
./gradlew assembleDebug
```

Opening the project in Android Studio also regenerates the wrapper automatically.

## Notes

- **Persistence** uses `SharedPreferences` with JSON payloads
  (`kotlinx.serialization`), mirroring the prototype's `localStorage` model.
- **Fonts**: the design specifies Manrope + JetBrains Mono. To stay asset- and
  network-free, the app maps these to the platform's geometric sans and monospace
  families. Drop the real font files into `res/font` and point `WtFonts` at them
  to match pixel-for-pixel.
- **Colours** were converted from the design's oklch tokens to sRGB in
  `ui/theme/Color.kt`.
