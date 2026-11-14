# NeverForget — Recurring Todo & Pill Reminder Android App

A clean, modern Android productivity and medication reminder application built with **Kotlin**, **Jetpack Compose**, **Material 3**, **Room Database (v5)**, **Exact `AlarmManager`**, and **AndroidX WorkManager**.

**NeverForget** separates everyday to-dos from medication schedules into two strictly isolated workspaces (**Tasks** and **Pills**) with separate notification channels, icons, and quick actions so your medication alerts never get mixed up with general task reminders.

---

## Key Features

### 1. Tasks Workspace
- **Rich Recurrence Patterns**:
  - **One-time (`NONE`)**: Standard tasks completed once.
  - **Daily (`DAILY`)**: Automatically resets to pending every calendar day.
  - **Weekdays (`WEEKDAYS`)**: Active Monday through Friday, skipping weekends.
  - **Weekly (`WEEKLY`)**: Custom days of the week (e.g., *Mon, Wed, Fri*).
  - **Monthly (`MONTHLY`)**: Specific day of the month (clamped automatically for shorter/leap months such as Feb 28/29 or Apr 30).
  - **Quarterly (`QUARTERLY`)** & **Yearly (`YEARLY`)**: Long-interval recurring obligations.
- **Smart Auto-Reset & Overdue Tracking**: Checking off a late recurring task clears its overdue status until the next scheduled occurrence without false overdue alerts on subsequent days.
- **Swipeable Filter Views (`HorizontalPager`)**: Switch smoothly across *Pending Today*, *Overdue*, *Next 7 Days*, *All Tasks*, *Done*, and *Recurring*.
- **Exact Task Alarms & Daily Summary**:
  - Per-task exact time alerts plus a configurable daily summary reminder.
  - Expandable notifications and detail dialog with snooze intervals (`1h`, `3h`, `6h`, `1d`).

### 2. Isolated Pills Workspace
- **Separate Notification Channel & Icon**: Delivered on `NeverForget Pill Reminders` (`pill_reminders_channel`) with a dedicated capsule status-bar icon (`ic_stat_pill.xml`) and deep link directly to the **Pills** tab.
- **Multiple Daily Doses**: Add multiple daily dose times (e.g., `08:00`, `14:00`, `20:00`) per medication; each slot is tracked independently per calendar day.
- **Dosage & Meal Context Badges**: Customize dosage text (e.g., `1 pill`, `500mg`) and meal instructions (`Anytime`, `Before meal`, `With food`, `After meal`, `Empty stomach`).
- **Course Duration Limits**: Configure **Ongoing** regimens or fixed **$N$-day courses** with automatic `"Day X of N"` progress tracking and automatic alarm cancellation once finished.
- **Lock-Screen & Dialog Quick Actions**: Mark individual doses **`✓ Taken`** directly from the notification shade or snooze for **`15m`**, **`30m`**, or **`1h`**.

### 3. Backup, Restore & Personalization
- **Non-Destructive Database Migrations**: Explicit Room migrations (`v1` $\rightarrow$ `v5`) preserve existing data across app upgrades.
- **JSON Backup & Restore**: Export and import both **Tasks** and **Pills** in portable JSON format (`version: 2`, backward-compatible with `v1` task-only backups).
- **Theme Switcher**: Cycle between **System Default**, **Light Theme**, and **Dark Theme** with dynamic Material 3 theming.
- **Reboot Resilient**: `BootReceiver` re-arms daily summaries, task alarms, and active medication dose alarms after device reboot or app update.

---

## Architecture & Project Structure

```text
NeverForget/
├── .github/workflows/unit-tests.yml                 # GitHub Actions CI workflow (JVM unit tests)
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/neverforget/app/
│   │   │   │   ├── MainActivity.kt                  # Compose host, runtime notification permission & deep linking
│   │   │   │   ├── TodoApplication.kt               # Application singleton, DB & channels init
│   │   │   │   ├── data/
│   │   │   │   │   ├── AppDatabase.kt               # Room database v5 with MIGRATION_1_2 .. MIGRATION_4_5
│   │   │   │   │   ├── BackupManager.kt             # JSON export/import serialization for Tasks & Pills
│   │   │   │   │   ├── Converters.kt                # Room TypeConverters (RecurrenceType, MealContext)
│   │   │   │   │   ├── MealContext.kt               # Meal instruction enum
│   │   │   │   │   ├── Pill.kt                      # Medication entity, slot-keyed dose log & course logic
│   │   │   │   │   ├── PillDao.kt                   # Room DAO for medications
│   │   │   │   │   ├── PillRepository.kt            # Repository wrapping PillDao
│   │   │   │   │   ├── RecurrenceType.kt            # Task recurrence enum
│   │   │   │   │   ├── Task.kt                      # Task entity, due/overdue/next-due evaluation
│   │   │   │   │   ├── TaskDao.kt                   # Room DAO for tasks
│   │   │   │   │   └── TaskRepository.kt            # Repository wrapping TaskDao
│   │   │   │   ├── reminder/
│   │   │   │   │   ├── AlarmReceiver.kt             # Exact alarm receiver for tasks, pills & snoozes
│   │   │   │   │   ├── BootReceiver.kt              # Re-arms scheduled alarms on boot/upgrade
│   │   │   │   │   ├── DailyReminderWorker.kt       # WorkManager worker for immediate/summary checks
│   │   │   │   │   ├── NotificationHelper.kt        # Separate task & pill notification channels/builders
│   │   │   │   │   ├── PillActionReceiver.kt        # Notification actions ("Taken", "Snooze 15m/30m")
│   │   │   │   │   ├── ReminderScheduler.kt         # AlarmManager & SharedPreferences scheduler
│   │   │   │   │   └── SnoozeReceiver.kt            # Task snooze BroadcastReceiver
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/                  # Task & Pill cards, progress gauges, dialogs & empty states
│   │   │   │   │   ├── screens/HomeScreen.kt        # Workspace navigation bar, filters, pagers & FAB
│   │   │   │   │   └── theme/                       # Material 3 colors, typography & ThemeMode
│   │   │   │   └── viewmodel/TaskViewModel.kt       # StateFlows & actions for Tasks and Pills
│   │   │   └── res/                                 # Drawables, adaptive launcher icons, strings & themes
│   │   └── test/java/com/neverforget/app/
│   │       ├── data/
│   │       │   ├── BackupManagerTest.kt             # v2 round-trip, legacy compatibility & malformed JSON tests
│   │       │   ├── ConvertersTest.kt                # Enum converter & fallback tests
│   │       │   ├── PillTest.kt                      # Multi-dose slots, course limits, snooze & history pruning
│   │       │   ├── TaskOverdueTest.kt               # Overdue detection & late completion tests
│   │       │   └── TaskRecurrenceTest.kt            # All recurrence patterns, clamping, formatting & snooze
│   │       └── ui/theme/ThemeModeTest.kt            # Theme cycle tests
│   └── build.gradle.kts
├── gradle/wrapper/                                  # Gradle 8.13 wrapper
├── build.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── local.properties.example                         # Template for local Android SDK path
├── settings.gradle.kts
├── .gitignore
├── LICENSE
└── README.md
```

---

## How Reminders & Alarms Work

NeverForget separates scheduling, delivery, and recovery into distinct components so alerts fire reliably without mixing everyday tasks with medications:

1. **Exact Alarm Scheduling (`ReminderScheduler`)**:
   - Schedules exact trigger timestamps using `AlarmManager.setExactAndAllowWhileIdle()` for per-task due reminders, medication dose slots, snoozed alerts, and the configurable daily summary.
   - Falls back cleanly and respects Android 14+ exact alarm permission checks while persisting active daily summary settings in `SharedPreferences`.
2. **Strictly Isolated Notification Channels (`NotificationHelper`)**:
   - **Tasks Channel (`todo_reminders_channel`)**: Delivers to-do alerts and snooze actions (`SnoozeReceiver`) and deep-links into the **Tasks** workspace.
   - **Pills Channel (`pill_reminders_channel`)**: Delivers medication alerts with a dedicated capsule status-bar icon (`ic_stat_pill.xml`), lock-screen **`✓ Taken`** and snooze quick actions (`PillActionReceiver`), and deep links directly to the **Pills** tab.
3. **Reboot & Daily Summary Resilience (`BootReceiver`, `DailyReminderWorker`)**:
   - `BootReceiver` listens for `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` to re-arm all pending task alarms, active medication course doses, and the daily summary schedule after device restarts or app updates.
   - `DailyReminderWorker` (AndroidX WorkManager) handles background summary checks cleanly within OS battery policies.

---

## Privacy & Permissions

- **100% Offline & Local-Only**: All tasks, medication logs, and preferences are stored strictly on-device in the Room SQLite database (`AppDatabase`) and local `SharedPreferences`. NeverForget **does not request `android.permission.INTERNET`**, sends zero analytics or telemetry, requires no account, and contains no ads.
- **Cloud Auto-Backup Disabled**: `AndroidManifest.xml` sets `android:allowBackup="false"` so personal task and medication histories are not automatically uploaded by Android system cloud backup (portable exports remain entirely under user control via manual JSON Backup & Restore).

| Permission (`AndroidManifest.xml`) | Why It Is Needed |
| :--- | :--- |
| `POST_NOTIFICATIONS` | Display task and pill reminder notifications on Android 13+ (API 33+). |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Fire task due reminders, medication dose schedules, and snoozes at the exact configured minute even in Doze mode. |
| `RECEIVE_BOOT_COMPLETED` | Re-arm scheduled alarms and daily summaries automatically via `BootReceiver` after device reboot. |
| `VIBRATE` | Provide tactile alert feedback when reminder notifications are delivered. |

---

## Getting Started

### Prerequisites
- **Android Studio** (Koala, Ladybug, or newer) or **JDK 17 / 21** with the **Android SDK** (`compileSdk = 34`, `minSdk = 26`).

### Setup & Build
1. Clone the repository:
   ```bash
   git clone https://github.com/claudiuroata/NeverForget.git
   cd NeverForget
   ```
2. Configure your local Android SDK path (or open the project in Android Studio to generate `local.properties` automatically):
   ```bash
   cp local.properties.example local.properties
   # Edit local.properties and set sdk.dir to your Android SDK location
   ```
3. Build the debug APK using the Gradle wrapper:
   ```bash
   ./gradlew assembleDebug
   ```
   *(On Windows PowerShell: `.\gradlew.bat assembleDebug`)*

   The compiled APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Running Unit Tests

Run all JVM unit tests across every feature via the Gradle wrapper:

```bash
./gradlew testDebugUnitTest
```

Included test suites:
- `TaskRecurrenceTest` — One-time, daily, weekdays, custom weekly days, monthly day clamping (including leap years), quarterly, yearly, next-7-days lookup, time/snooze formatting.
- `TaskOverdueTest` — Overdue detection and late-completion reset across one-time, daily, weekdays, weekly, monthly, quarterly, and yearly tasks.
- `PillTest` — Independent daily dose slots, ongoing vs. fixed-day treatment course boundaries, slot snoozing, and 120-entry history pruning.
- `BackupManagerTest` — Full Tasks + Pills v2 JSON round-trip serialization, legacy v1 import compatibility, and invalid input handling.
- `ConvertersTest` — Room `TypeConverter` encoding/decoding and unknown-value fallbacks for `RecurrenceType` and `MealContext`.
- `ThemeModeTest` — `System` $\rightarrow$ `Light` $\rightarrow$ `Dark` $\rightarrow$ `System` theme cycling.

---

## Dependencies & Third-Party Licenses

NeverForget links against the following open-source libraries via Gradle (`app/build.gradle.kts`) and includes the generated Gradle Wrapper (Apache 2.0). Compiled release/debug APKs bundle the runtime AndroidX and Jetpack dependencies under the Apache License 2.0.

| Dependency | Scope | Purpose | License |
| :--- | :--- | :--- | :--- |
| **AndroidX Core, Activity & Lifecycle** (`core-ktx:1.13.1`, `activity-compose:1.9.1`, `lifecycle-*:2.8.4`) | Runtime | Core Android, Activity Compose & ViewModel APIs | Apache License 2.0 |
| **Jetpack Compose & Material 3** (`compose-bom:2024.06.00`, `material3`, `material-icons-extended`) | Runtime | Declarative UI & Material Icons | Apache License 2.0 |
| **AndroidX Room** (`room-runtime`, `room-ktx`, `room-compiler:2.6.1`) | Runtime | Local SQLite database & migrations | Apache License 2.0 |
| **AndroidX WorkManager** (`work-runtime-ktx:2.9.1`) | Runtime | Background worker scheduling | Apache License 2.0 |
| **Gradle Wrapper (`8.13`)** (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) | Build Tooling | Cross-platform Gradle build wrapper | Apache License 2.0 |
| **JUnit 4** (`junit:4.13.2`) | Unit Tests Only | Local JVM unit testing framework | Eclipse Public License 1.0 |
| **org.json** (`org.json:json:20240303`) | Unit Tests Only | Pure JVM JSON parsing for unit tests | The JSON License |

---

## License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE) for details.
