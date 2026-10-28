# Viora Local Development & Run Guide

## Prerequisites
- Android Studio Ladybug / Meerkat or Command Line Tools
- JDK 17+
- Supabase CLI (`npm install -g supabase`)
- Agora Developer Account (App ID & Certificate)

---

## Quick Start (Android)

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-org/viora-mobile.git
   cd viora-mobile
   ```

2. **Configure Environment:**
   Copy `.env.example` to `.env` and fill in your Supabase and Agora test keys:
   ```bash
   cp .env.example .env
   ```

3. **Build and Run Android:**
   ```bash
   # Make Gradle wrapper executable
   chmod +x gradlew

   # Run unit tests
   gradle testDebugUnitTest

   # Build debug APK
   gradle assembleDebug
   ```

4. **Install on Connected Device or Streaming Emulator:**
   The compiled APK is located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Testing User Roles in Sandbox
Viora includes a built-in interactive role switcher in **My Profile > Switch Active Role**:
- **Viewer:** Experience broadcasts, chat, send gifts, tap hearts, and recharge coins.
- **Broadcaster:** Access Broadcast Studio, configure camera/mic, manage active room, view diamond earnings.
- **Moderator:** Pin/delete chat messages, mute viewers, report violators.
- **Admin:** Access the full Admin Dashboard & Safety Center (`AdminPanelSettings` icon on Profile screen). Review pending reports, view platform health, and terminate rule-breaking broadcasts.

---

## Supabase Local Setup
```bash
# Initialize and start Supabase Docker environment
supabase start

# Apply migrations
supabase db reset

# Deploy edge functions locally
supabase functions serve
```
