# QuickNote for Obsidian 🗒️  
*A lightweight Android app for creating Markdown notes directly in your Obsidian vault.*

---

## Overview

**QuickNote** is a minimalist Android app built with **Jetpack Compose** and **Material 3**, designed to quickly capture notes straight into an Obsidian vault.  
Perfect for mobile workflows — simple, fast, and private.

---

## Features

- 🧱 Create Markdown notes with front-matter and templates  
- 💾 Save directly into your configured Obsidian vault (via Storage Access Framework URI)  
- 🎨 Modern Material 3 theme with light/dark/system toggle  
- ⚙️ Persistent settings (DataStore) for vault location, file naming, and front-matter  
- 🪄 Clean Jetpack Compose UI with About screen and dynamic version info  

---

## 🛠️ Tech Stack

| Component | Usage |
|------------|--------|
| **Jetpack Compose** | UI |
| **Material 3** | Design system |
| **DataStore** | Persistent user preferences |
| **Kotlin** | Core language |
| **Android 14 (API 34)** | Target SDK |

---

## 📂 Project Structure

```
ui/
  screens/
    HomeScreen.kt
    SettingsScreen.kt
    AboutScreen.kt
  theme/
    Theme.kt, Typography.kt, ThemePreference.kt
data/
  VaultPreferences.kt
utils/
  NoteHelpers.kt
```

---

## ⚙️ Planned Features

- Overwrite-existing toggle  
- Note presets/templates system  
- Optional “Share to QuickNote” intent  
- Home screen widget  

---

## 🚀 Building

1. Clone the repo  
   ```bash
   git clone https://github.com/symph0nic/QuickNoteApp.git
   ```
2. Open in **Android Studio Ladybug (or newer)**
3. Build & run on a device or emulator  
   *(Target SDK 34, min SDK 26)*

---

## 📜 License
Released under the [MIT License](LICENSE)

---

**Author:** [@symph0nic](https://github.com/symph0nic)  
**Version:** v1.0.0-beta1  
**Status:** Public beta – feedback welcome!
