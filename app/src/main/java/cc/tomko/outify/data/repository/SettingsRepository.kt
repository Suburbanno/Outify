package cc.tomko.outify.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cc.tomko.outify.data.setting.GestureAction
import cc.tomko.outify.data.setting.GestureSetting
import cc.tomko.outify.data.setting.GestureTrigger
import cc.tomko.outify.data.setting.Side
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    val dataStore: DataStore<Preferences>,
) {
    object Keys {
        val SHUFFLE = booleanPreferencesKey("shuffle")
        val REPEAT = booleanPreferencesKey("repeat")
        val GAPLESS = booleanPreferencesKey("gapless")
        val NORMALIZE_AUDIO = booleanPreferencesKey("normalized_audio")

        /**
         * aka session resurrection
         */
        val KEEPALIVE = booleanPreferencesKey("keepalive")
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val USER_IMAGE_URL = stringPreferencesKey("user_image_url")

        object Gesture {
            val ENABLED = booleanPreferencesKey("gestures_enabled")
            val GESTURES = stringPreferencesKey("gestures_json")
        }

        object Lyrics {
            /**
             * When false, show on manual trigger
             */
            val SHOW_LYRICS_ALWAYS = booleanPreferencesKey("always_show_lyrics")
        }

        object Interface {
            val DYNAMIC_THEME = booleanPreferencesKey("dynamic_theme")
            val PURE_BLACK = booleanPreferencesKey("pure_black")
            val HIGH_CONTRAST_COMPAT = booleanPreferencesKey("high_contrast_compat")

            val MONOCHROME_IMAGES = booleanPreferencesKey("monochrome_images")
            val MONOCHROME_ALBUMS = booleanPreferencesKey("monochrome_albums")
            val MONOCHROME_ARTISTS = booleanPreferencesKey("monochrome_artists")
            val MONOCHROME_PLAYLISTS = booleanPreferencesKey("monochrome_playlists")
            val MONOCHROME_TRACKS = booleanPreferencesKey("monochrome_tracks")
            val MONOCHROME_PLAYER = booleanPreferencesKey("monochrome_player")
            val MONOCHROME_HEADERS = booleanPreferencesKey("monochrome_headers")
        }

        object Queue {
            val QUEUES = stringPreferencesKey("saved_queues_v1")
            val ACTIVE_ID = stringPreferencesKey("active_queue_id")
        }
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(
        Dispatchers.Main.immediate
    )

    val interfaceSettings: Flow<InterfaceSettings> = dataStore.data.map { prefs ->
        val enabled = prefs[Keys.Gesture.ENABLED] ?: true
        
        val monochrome = prefs[Keys.Interface.MONOCHROME_IMAGES] ?: false
        InterfaceSettings(
            swipeGesturesEnabled = enabled,
            gestureSettings = if (enabled) decodeGestures(prefs[Keys.Gesture.GESTURES]) else emptyList(),

            // Dynamic theme
            dynamicTheme = prefs[Keys.Interface.DYNAMIC_THEME] ?: true,
            pureBlack = prefs[Keys.Interface.PURE_BLACK] ?: false,
            highContrastCompat = prefs[Keys.Interface.HIGH_CONTRAST_COMPAT] ?: false,

            // Monochrome
            monochromeImages = monochrome,
            monochromeAlbums = monochrome && prefs[Keys.Interface.MONOCHROME_ALBUMS] ?: false,
            monochromeArtists = monochrome && prefs[Keys.Interface.MONOCHROME_ARTISTS] ?: false,
            monochromePlaylists = monochrome && prefs[Keys.Interface.MONOCHROME_PLAYLISTS] ?: false,
            monochromeTracks = monochrome && prefs[Keys.Interface.MONOCHROME_TRACKS] ?: false,
            monochromePlayer = monochrome && prefs[Keys.Interface.MONOCHROME_PLAYER] ?: false,
            monochromeHeaders = monochrome && prefs[Keys.Interface.MONOCHROME_HEADERS] ?: false,
        )
    }

    val playbackSettings: Flow<PlaybackSettings> =  dataStore.data.map { prefs ->
        PlaybackSettings(
            gapless = prefs[Keys.GAPLESS] ?: false,
            normalizeAudio = prefs[Keys.NORMALIZE_AUDIO] ?: false,
            keepalive = prefs[Keys.KEEPALIVE] ?: true,
        )
    }

    object Gesture {
        fun defaultGestureList(): List<GestureSetting> = listOf(
            GestureSetting(
                action = GestureAction.ADD_TO_QUEUE,
                side = Side.End,
                thresholdFraction = 0.05f,
                backgroundHex = 0xC43C8C52,
            ),
            GestureSetting(
                action = GestureAction.START_RADIO,
                side = Side.End,
                thresholdFraction = 0.45f,
            ),
            GestureSetting(
                action = GestureAction.ADD_TO_FAVORITE,
                side = Side.Start,
                thresholdFraction = 0.25f,
                backgroundHex = 0xC43C8C52,
            )
        )
    }

    init {
        scope.launch {
            dataStore.edit { prefs ->
                val current = prefs[Keys.Gesture.GESTURES]
                if (current.isNullOrBlank()) {
                    val serializedDefaults = json.encodeToString(Gesture.defaultGestureList())
                    prefs[Keys.Gesture.GESTURES] = serializedDefaults
                }
            }
        }
    }

    val shuffleEnabled = dataStore.data.map {
        it[Keys.SHUFFLE] ?: false
    }

    val repeatEnabled = dataStore.data.map {
        it[Keys.REPEAT] ?: false
    }

    val gaplessPlayback = dataStore.data.map {
        it[Keys.GAPLESS] ?: true
    }

    val normalizePlayback = dataStore.data.map {
        it[Keys.NORMALIZE_AUDIO] ?: false
    }

    val keepalive = dataStore.data.map {
        it[Keys.KEEPALIVE] ?: false
    }

    val showLyricsByDefault = dataStore.data.map {
        it[Keys.Lyrics.SHOW_LYRICS_ALWAYS] ?: true
    }

    val userId = dataStore.data.map { it[Keys.USER_ID] }
    val username = dataStore.data.map { it[Keys.USERNAME] }
    val userImageUrl = dataStore.data.map { it[Keys.USER_IMAGE_URL] }

    suspend fun setShuffle(enabled: Boolean) {
        dataStore.edit { it[Keys.SHUFFLE] = enabled }
    }

    suspend fun setRepeat(enabled: Boolean) {
        dataStore.edit { it[Keys.REPEAT] = enabled }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        dataStore.edit { it[Keys.GAPLESS] = enabled }
    }

    suspend fun setNormalizePlayback(enabled: Boolean) {
        dataStore.edit { it[Keys.NORMALIZE_AUDIO] = enabled }
    }

    suspend fun setKeepalive(enabled: Boolean) {
        dataStore.edit { it[Keys.KEEPALIVE] = enabled }
    }

    suspend fun setGesturesEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.Gesture.ENABLED] = enabled }
    }

    suspend fun setDynamicTheme(enabled: Boolean) {
        dataStore.edit { it[Keys.Interface.DYNAMIC_THEME] = enabled }
    }

    suspend fun setPureBlack(enabled: Boolean) {
        dataStore.edit { it[Keys.Interface.PURE_BLACK] = enabled }
    }

    suspend fun setHighContrastCompat(enabled: Boolean) {
        dataStore.edit { it[Keys.Interface.HIGH_CONTRAST_COMPAT] = enabled }
    }

    suspend fun setMonochromeImages(enabled: Boolean) {
        dataStore.edit { it[Keys.Interface.MONOCHROME_IMAGES] = enabled }
    }

    suspend fun setMonochromeAlbums(enabled: Boolean) {
        dataStore.edit { it[Keys.Interface.MONOCHROME_ALBUMS] = enabled }
    }

    suspend fun setMonochromeArtists(enabled: Boolean) {
        dataStore.edit { it[Keys.Interface.MONOCHROME_ARTISTS] = enabled }
    }

    suspend fun setMonochromePlaylists(enabled: Boolean) {
        dataStore.edit { it[Keys.Interface.MONOCHROME_PLAYLISTS] = enabled }
    }

    suspend fun setMonochromeTracks(enabled: Boolean) {
        dataStore.edit { it[Keys.Interface.MONOCHROME_TRACKS] = enabled }
    }

    suspend fun setMonochromePlayer(enabled: Boolean) {
        dataStore.edit { it[Keys.Interface.MONOCHROME_PLAYER] = enabled }
    }

    suspend fun setMonochromeHeaders(enabled: Boolean) {
        dataStore.edit { it[Keys.Interface.MONOCHROME_HEADERS] = enabled }
    }

    suspend fun removeUserProfile() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USERNAME)
            prefs.remove(Keys.USER_IMAGE_URL)
        }
    }

    suspend fun saveUserProfile(userId: String, username: String?, userImageUrl: String?) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userId
            username?.let { prefs[Keys.USERNAME] = it }
            userImageUrl?.let { prefs[Keys.USER_IMAGE_URL] = it }
        }
    }

    suspend fun saveGestures(gestures: List<GestureSetting>) {
        val serialized = json.encodeToString(gestures)
        dataStore.edit { it[Keys.Gesture.GESTURES] = serialized }
    }

    private fun decodeGestures(serialized: String?): List<GestureSetting> {
        if (serialized.isNullOrBlank()) return Gesture.defaultGestureList()
        return try {
            return json.decodeFromString(serialized)
        } catch (e: Exception) {
            Gesture.defaultGestureList()
        }
    }
}

data class InterfaceSettings(
    val swipeGesturesEnabled: Boolean = true,
    // Default gestures
    val gestureSettings: List<GestureSetting> = listOf(
        GestureSetting(
            action = GestureAction.ADD_TO_QUEUE,
            side = Side.End,
            thresholdFraction = 0.05f,
            backgroundHex = 0xC43C8C52,
        ),
        GestureSetting(
            action = GestureAction.START_RADIO,
            side = Side.End,
            thresholdFraction = 0.45f,
        ),
        GestureSetting(
            action = GestureAction.ADD_TO_FAVORITE,
            side = Side.Start,
            thresholdFraction = 0.25f,
            backgroundHex = 0xC43C8C52,
        ),
        GestureSetting(
            action = GestureAction.SHOW_TRACK_INFO,
            trigger = GestureTrigger.LongPress,
        )
    ),
    // Dynamic theme
    val dynamicTheme: Boolean = true,
    val pureBlack: Boolean = false,
    val highContrastCompat: Boolean = false,

    // Monochrome
    val monochromeImages: Boolean = false,
    val monochromeAlbums: Boolean = false,
    val monochromeArtists: Boolean = false,
    val monochromePlaylists: Boolean = false,
    val monochromeTracks: Boolean = false,
    val monochromePlayer: Boolean = false,
    val monochromeHeaders: Boolean = false,
)

data class PlaybackSettings(
    val gapless: Boolean = false,
    val normalizeAudio: Boolean = false,
    val keepalive: Boolean = true,
)