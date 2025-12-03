# Cricket Watchdog - Android TV App Walkthrough

I have generated the complete Android Studio project for "Cricket Watchdog". This app is designed to run on Android TV, monitoring live cricket matches and providing alerts even when the TV is muted.

## Project Structure

The project is located in `c:/Users/navee/OneDrive/Pictures/app/CricketWatchdog`.

### Core Components

1.  **[MatchMonitorService.kt](file:///c:/Users/navee/OneDrive/Pictures/app/CricketWatchdog/app/src/main/java/com/example/cricketwatchdog/MatchMonitorService.kt)**
    *   The heart of the app. It's a Foreground Service that keeps running in the background.
    *   **Monitoring Loop:** Polls the API every 5 seconds for score updates.
    *   **Sync Loop:** Periodically triggers the OCR sync process.
    *   **Alert Logic:** Triggers `VolumeManager` and `OverlayManager` when events (4, 6, Wicket) are detected.

2.  **[AutoSyncEngine.kt](file:///c:/Users/navee/OneDrive/Pictures/app/CricketWatchdog/app/src/main/java/com/example/cricketwatchdog/AutoSyncEngine.kt)**
    *   Handles the "Magic" of syncing TV stream with API.
    *   **Capture:** Uses `MediaProjection` to take a screenshot.
    *   **OCR:** Uses ML Kit to read the over number (e.g., "14.2") from the bottom 20% of the screen.
    *   **Calibration:** Calculates the delay between the live TV stream and the API data.
    *   **Ad Detection:** If it can't find the score (e.g., during an Ad), it reports `AD_DETECTED` so the service can retry.

3.  **[VolumeManager.kt](file:///c:/Users/navee/OneDrive/Pictures/app/CricketWatchdog/app/src/main/java/com/example/cricketwatchdog/VolumeManager.kt)**
    *   Ensures you never miss a moment.
    *   **Force Attention:** Unmutes the device and lowers music volume if needed.
    *   **AI Voice:** Can speak events like "It's a huge Six!" using Text-To-Speech.

4.  **[OverlayManager.kt](file:///c:/Users/navee/OneDrive/Pictures/app/CricketWatchdog/app/src/main/java/com/example/cricketwatchdog/OverlayManager.kt)**
    *   **Visual Flash:** Shows a full-screen flash (Green/Red/Orange) with big text.
    *   **Smart Logic:** Only flashes if the **System Volume is Muted**, respecting your viewing experience.

5.  **[MainActivity.kt](file:///c:/Users/navee/OneDrive/Pictures/app/CricketWatchdog/app/src/main/java/com/example/cricketwatchdog/MainActivity.kt)**
    *   The setup screen.
    *   **Inputs:** API Key, Sync Interval, Manual Delay.
    *   **Sound Pack:** Choose between "Standard SFX" and "AI Voice".
    *   **Resync:** Manual "Force Resync Now" button.

## Getting a Free API Key

To use the app, you need a free API key from **CricketData.org** (formerly CricAPI).

1.  Go to [https://cricketdata.org/](https://cricketdata.org/).
2.  Click **Sign Up** (it's free).
3.  Once logged in, copy your **API Key** from the dashboard.
4.  Enter this key in the "API Key" field in the Cricket Watchdog app.

## How to Build & Run

> [!SUCCESS]
> **Code Pushed to GitHub!**
> The code has been successfully uploaded to your repository:
> [https://github.com/naveeneppalapally/CricketWatchdog-](https://github.com/naveeneppalapally/CricketWatchdog-)
>
> **Note:** The code is currently in the state where it uses API for match data but has the *Auto-Match Detection* logic. The OCR-only mode was reverted as requested.
>
> **Documentation:** I have also added a comprehensive `README.md` file to the repository, explaining the app's features and development history.

1.  **Locate the APK:**
    The APK file is located at:
    `c:/Users/navee/OneDrive/Pictures/app/CricketWatchdog/app/build/outputs/apk/debug/app-debug.apk`

2.  **Install on TV:**
    *   Transfer this `app-debug.apk` file to your Android TV (via USB, "Send Files to TV" app, or ADB).
    *   Open the file on your TV to install it.

3.  **Permissions:**
    *   On the TV, open the app.
    *   Click "Start Monitoring".
    *   **Grant Permissions:** You will be prompted to allow "Display over other apps" and "Screen Capture". These are required for the overlay and OCR to work.

4.  **Add Assets (Optional):**
    *   The app currently uses silent placeholders for sounds.
    *   To add real sounds, you would need to replace the files in `app/src/main/res/raw/` and rebuild.
    *   Since I built this from the terminal, if you want to change sounds, you can replace the files and run:
        ```powershell
        C:\Gradle\gradle-9.2.1\bin\gradle.bat assembleDebug
        ```

## Key Features Implemented
*   [x] **Live Score Monitoring** (Retrofit API)
*   [x] **OCR Auto-Sync** (ML Kit + MediaProjection)
*   [x] **Smart Alerts** (Flash only on Mute)
*   [x] **Ad Detection** (Retry logic)
*   [x] **AI Voice Options** (TTS integration)
