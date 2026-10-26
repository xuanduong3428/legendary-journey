# legendary-journey

## Lc0 Chess Analyzer

A small native Android app for setting up a chess position and asking the
official Lc0 Android engine to analyze it.

## Requirements

- Android Studio with Android SDK 35 and JDK 17.
- An Android phone or emulator with the official
  [Lc0 v0.32.1 Android APK](https://github.com/LeelaChessZero/lc0/releases/download/v0.32.1/lc0-v0.32.1-android.apk)
  installed. The APK provides the engine; this app provides the board and
  analysis interface.

## Build

Open this folder in Android Studio, allow Gradle sync, then run the `app`
configuration on an Android device. The app discovers engines using the
Android Chess Engine Exchange provider interface and sends UCI commands to
Lc0.

## Use

- Tap a board square to cycle it through empty, white pieces, and black pieces.
- Or paste a four- to six-field FEN string and tap **Nạp FEN**.
- Tap **Phân tích** to run Lc0 to depth 12 and display its latest UCI line and
  best move.

This is an analysis prototype, not a complete chess game. The position editor
does not enforce legal chess positions or moves.
