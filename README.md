# MERCURY
### Music Discovery, Reimagined.

An Android-based music discovery application built using Kotlin. Mercury identifies songs playing around you using the Shazam Music Recognition API through RapidAPI.

## Screenshots

### Home
![Mercury Home](screenshots/home.jpg)

### Library
![Mercury Library](screenshots/library.jpg)

### Saved / Favourites
![Mercury Saved](screenshots/saved.jpg)

### Song Recognition
![Recognition Result](screenshots/recognition.jpg)

### Profile
![Mercury Profile](screenshots/profile.jpg)

## Features

- Identify songs using your device microphone.
- Retrieve song titles, artists and album artwork.
- Maintain a personal discovery library.
- Save and manage favourite songs.
- View discovery statistics through the Profile screen.
- Store song information locally using SQLite.

## Technology Stack

- Kotlin
- Android Studio
- Android SDK
- XML layouts
- Shazam Music Recognition API
- RapidAPI
- SQLite

## Application Structure

- **Home:** Start music recognition and view recent discoveries.
- **Library:** Browse all identified songs.
- **Saved:** Access favourite songs.
- **Profile:** View discovery statistics and recent activity.

## How It Works

1. The user starts listening.
2. Mercury captures a short audio sample through the microphone.
3. The audio is encoded and sent to the Shazam recognition API.
4. The API returns matching song information.
5. Mercury displays the result and saves the song in SQLite.

## Setup

1. Clone or download this repository.
2. Open the project in Android Studio.
3. Configure your RapidAPI credentials locally. Do not commit API keys.
4. Sync the Gradle project.
5. Connect an Android device or start an emulator.
6. Build and run the application.
7. Grant microphone permission when prompted.

## Developer

**Divya Pratap Singh**  
B.Tech, Jaypee Institute of Information Technology

## Project

Developed as part of the Android & iOS Lab.

## Future Improvements

- Cloud synchronisation.
- Song sharing.
- Search and sorting within the library.
- Improved recognition feedback.
- More detailed listening statistics.