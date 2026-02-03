package com.aiassistant.domain.model

enum class AppTarget(val packageName: String, val displayName: String) {
    YOUTUBE("com.google.android.youtube", "YouTube"),
    LINKEDIN("com.linkedin.android", "LinkedIn"),
    TINDER("com.tinder", "Tinder"),
    INSTAGRAM("com.instagram.android", "Instagram"),
    WHATSAPP("com.whatsapp", "WhatsApp"),
    SPOTIFY("com.spotify.music", "Spotify"),
    CHROME("com.android.chrome", "Chrome"),
    GMAIL("com.google.android.gm", "Gmail"),
    MAPS("com.google.android.apps.maps", "Google Maps"),
    TWITTER("com.twitter.android", "X / Twitter"),
}