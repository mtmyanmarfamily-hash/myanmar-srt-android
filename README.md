# မြန်မာဘာသာ SRT Translator — Android App

Kotlin WebView app that wraps the SRT translator. Build the APK via GitHub Actions — no PC needed.

---

## 📱 Build APK from Termux (step-by-step)

### 1. Install git in Termux (if not done)
```bash
pkg install git
```

### 2. Create a GitHub repo
Go to https://github.com/new on your phone browser, create a **public** or private repo named `myanmar-srt-android`, leave it empty (no README).

### 3. Push this project
```bash
cd ~
# Copy the project folder to Termux home if needed
ls myanmar-srt-android/   # confirm files are here

cd myanmar-srt-android
git init
git add .
git commit -m "Initial commit: Myanmar SRT Android app"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/myanmar-srt-android.git
git push -u origin main
```
> Replace `YOUR_USERNAME` with your GitHub username.
> When prompted for password, use a **Personal Access Token** (not your password):
> GitHub → Settings → Developer settings → Personal access tokens → Generate new token (classic) → check `repo` scope

### 4. Watch the build
- Go to your repo on GitHub
- Click the **Actions** tab
- You'll see "Build APK" running automatically
- Wait ~5 minutes for it to finish

### 5. Download the APK
- Click the completed workflow run
- Scroll down to **Artifacts**
- Download `myanmar-srt-debug-1` (a ZIP containing the APK)
- Extract and install `app-debug.apk` on your phone

> ⚠️ Enable **"Install from unknown sources"** in Android Settings → Security before installing.

---

## 🔑 Gemini API Key
Get a free key at: https://aistudio.google.com/app/apikey  
Enter it in the app's API Key field. It's saved to app storage for future use.

---

## 🔄 Updating the app
After any edits:
```bash
cd ~/myanmar-srt-android
git add .
git commit -m "Update"
git push
```
GitHub Actions will auto-rebuild. Download the new artifact.

---

## 🔏 Optional: Signed Release APK
For a properly signed release (required for Google Play), add these to your repo's
**Settings → Secrets and variables → Actions**:

| Secret name | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 your-keystore.jks` output |
| `KEYSTORE_PASSWORD` | your keystore password |
| `KEY_ALIAS` | your key alias |
| `KEY_PASSWORD` | your key password |

Generate a keystore in Termux:
```bash
pkg install openjdk-17
keytool -genkeypair -v -keystore myanmarsrt.jks -alias myanmarsrt \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 myanmarsrt.jks   # copy this output as KEYSTORE_BASE64 secret
```

---

## 📁 Project Structure
```
myanmar-srt-android/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml       # App permissions & config
│       ├── assets/www/index.html     # The entire web UI
│       ├── java/com/myanmarsrt/
│       │   └── MainActivity.kt       # WebView + Android bridge
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/themes.xml
│           └── xml/network_security_config.xml
├── .github/workflows/build.yml       # GitHub Actions CI
├── build.gradle                      # Root Gradle config
├── app/build.gradle                  # App Gradle config
└── settings.gradle
```

---

## ⚙️ AndroidBridge API (JS ↔ Kotlin)
The app exposes these methods to JavaScript via `window.AndroidBridge`:

| Method | Description |
|---|---|
| `isAndroid()` | Returns `"true"` when running in the app |
| `readFileAsBase64(uri)` | Reads a content:// URI and returns base64 |
| `getMimeType(uri)` | Returns MIME type of a file URI |
| `getFileName(uri)` | Returns display name of a file URI |
| `getFileSize(uri)` | Returns file size in bytes |
| `saveSRT(content, filename)` | Opens native Save dialog |
| `showToast(message)` | Shows Android toast notification |

---

## Requirements
- Android 7.0+ (API 24)  
- Internet access for Gemini API calls
