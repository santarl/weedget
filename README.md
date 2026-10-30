> **AI assistance:** this project is written with the help of Claude (Anthropic). Commits carry a `Co-Authored-By` trailer.

# tmr

Tiny Android timer widgets.

Create timers inside the app, then put them on the home screen as app-icon sized widgets (ring, pie, vertical bar or fill). A timer either counts down from a set duration or counts up forever. Tap an idle widget to start it; tap a running or finished one to get a slide-to-confirm reset. Every timer has its own notification channel, so sound and vibration can be set per timer. A 4x2 list widget shows all timers at once.

No AndroidX, no libraries, no analytics. Permissions: post notifications, exact alarms, and run at boot (to re-arm alarms).

Licensed under GPL-3.0.
