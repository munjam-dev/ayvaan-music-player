package com.example.ui.viewmodel

data class DiagnosticsStats(
    val postersLoaded: Int = 0,
    val categories: Int = 0,
    val songsAssigned: Int = 0,
    val albumsAssigned: Int = 0,
    val artistsAssigned: Int = 0,
    val playlistsAssigned: Int = 0,
    val unassignedSongs: Int = 0,
    val cacheSize: String = "0 MB",
    val lastAssignmentRun: String = "Never"
)
