# Viora

> *“Go live. Find your people.”*

Viora is a next-generation social live-streaming mobile application where creators and communities connect in real-time. Built with a signature dark aesthetic featuring deep violet (`#35146F`), electric purple (`#7B35E8`), coral (`#FF5470`), and warm gold (`#FFC857`), Viora delivers broadcast-grade streaming, interactive chat, virtual gifting, and creator monetization.

---

## ✨ Features

- **Live Broadcasts:** Low-latency video and audio powered by Agora RTC architecture with camera flip, mute, and video pause controls.
- **Interactive Live Room:**
  - Full-bleed immersive video surface with dynamic ambient audio visualizer waves.
  - Real-time chat with host/moderator badge identification and pin/delete moderation.
  - Ascending animated floating reaction hearts on tap or double-tap.
  - Celebratory full-room virtual gift banner announcements.
- **Creator Economy & Virtual Gifts:**
  - 8 catalog gifts: Rose 🌹, Coffee ☕, Sparkle Heart 💖, Fire Energy 🔥, Diamond Ring 💍, Sports Car 🏎️, Galactic Rocket 🚀, Viora Crown 👑.
  - Combo multipliers (x1, x5, x10, x99).
  - 70% Creator Revenue Share credited as Diamonds (100 diamonds = $1.00 USD).
  - Full financial ledger and withdrawal request engine.
- **Role-Based Access Control (RBAC):**
  - **Viewer:** Watch, chat, like, follow, send gifts.
  - **Broadcaster:** Studio setup, live camera preview, viewer telemetry, gift earnings, stream termination.
  - **Moderator:** Chat cleanup, comment pinning, user muting, report review.
  - **Admin:** Safety review queue, stream kill switch, platform health analytics.
- **Admin & Safety Center:**
  - Pending safety queue with reason tags (harassment, spam, underage, inappropriate).
  - One-tap enforcement actions (Dismiss, Warn, Kill Room, Ban User).
- **Design System:** Material 3 dark-first aesthetic, adaptive launcher icon with Viora broadcast beam monogram.

---

## 🛠️ Tech Stack & Integrations

- **Android Client:** Kotlin, Jetpack Compose, Material Design 3, Coroutines & StateFlow, Coil image loading.
- **Live Video & Audio:** Agora RTC Engine (token-gated authentication).
- **Cloud Backend:** Supabase PostgreSQL with Row Level Security (RLS) & Realtime.
- **Serverless Logic:** Supabase Edge Functions (Deno / TypeScript).
- **Continuous Integration:** GitHub Actions workflows for Android build, Supabase validation, and security scans.

---

## 📁 Repository Structure

```
├── app/
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt               # App root, edge-to-edge, bottom navigation
│   │   ├── model/                        # Domain models (User, LiveStream, Gift, Wallet, Moderation)
│   │   ├── data/                         # MockDataProvider, VioraRepository state engine
│   │   ├── ui/
│   │   │   ├── components/               # LiveBadge, AvatarView, FloatingReactions, GiftBottomSheet, StreamCard
│   │   │   ├── screens/                  # Auth, Home, LiveRoom, BroadcastSetup, Wallet, Profile, Admin, Settings
│   │   │   └── theme/                    # Color.kt, Theme.kt, Type.kt
│   │   └── res/                          # Adaptive icons (mipmap), drawables, strings
├── docs/                                 # Architecture, local development, Agora, security docs
├── supabase/
│   ├── migrations/                       # PostgreSQL schema & RLS policies
│   ├── functions/                        # Edge functions (agora-token, send-gift)
│   └── seed.sql                          # Categories & gift catalog seed data
├── .github/workflows/                    # CI/CD pipelines
└── .env.example                          # Environment variable configuration template
```
