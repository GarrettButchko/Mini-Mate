# MiniMate

MiniMate is a digital companion for mini golf businesses — designed to modernize the course experience with digital scorekeeping, leaderboards, and customer insights. 

This repository is a **Kotlin Multiplatform (KMP)** project containing four applications (Admin Android, User Android, Admin iOS, and User iOS) and a shared logic module.

## Project Structure

- **`working/shared`**: Kotlin Multiplatform module containing shared business logic, data models, and constants used by all four apps.
- **`working/androidApps/admin-android`**: The Android application for course administrators.
- **`working/androidApps/user-android`**: The Android application for players.
- **`working/iosApps/admin-ios`**: The SwiftUI application for course administrators.
- **`working/iosApps/user-ios`**: The SwiftUI application for players.

## Features

### For Players (User Apps)
- Digital scorecards with automatic scoring.
- Live leaderboards to compete locally or globally.
- Personal game stats and achievements.

### For Businesses (Admin Apps)
- Branded experience with course details and assets.
- Customer insights dashboard (play trends, engagement).
- Promotions and event management.

## Tech Stack

- **Shared Logic**: Kotlin Multiplatform (KMP)
- **Android**: Jetpack Compose, Kotlin
- **iOS**: SwiftUI, SwiftData
- **Backend**: Firebase (Auth, Firestore, Functions)
- **Database**: Firestore (Remote), Room (Local)

## Getting Started

### Android Development
1. Open the root directory in **Android Studio**.
2. Select either `:admin-android` or `:user-android` from the run configurations.
3. Build and run on an emulator or device.

### iOS Development
1. Navigate to `working/iosApps/`.
2. Open `MiniMate.xcworkspace` in **Xcode**.
3. Select either the `admin-ios` or `user-ios` scheme.
4. Ensure `GoogleService-Info.plist` is added to the respective app targets.
5. Build and run on a simulator or device.

## Contributing
- Follow the existing project structure: place shared logic in the `shared` module and platform-specific UI in the respective app folders.
- Ensure all four apps are tested when making changes to shared data models.

## License & Contact
This project is maintained privately. For questions or access, contact the maintainer.
