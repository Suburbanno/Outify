package cc.tomko.outify.ui.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.core.spirc.SpircController
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.data.repository.LikedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class SetupStep { CONFIG, LOADING, DONE }

enum class StreamingQuality(val label: String) {
    NORMAL("Normal"),
    HIGH("High"),
    VERY_HIGH("Very High"),
}

enum class TaskState { PENDING, RUNNING, DONE, FAILED }

data class LoadingTask(
    val id: String,
    val label: String,
    val icon: String,
    val state: TaskState = TaskState.PENDING,
)

data class SetupConfig(
    val streamingQuality: StreamingQuality = StreamingQuality.HIGH,
    val enableGaplessPlayback: Boolean = true,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    val spClient: SpClient,
    val metadata: Metadata,
    val spircController: SpircController,
    val spirc: SpircWrapper,
    val likedRepository: LikedRepository,
) : ViewModel() {
    private val _step = MutableStateFlow(SetupStep.CONFIG)
    val step: StateFlow<SetupStep> = _step.asStateFlow()

    private val _config = MutableStateFlow(SetupConfig())
    val config: StateFlow<SetupConfig> = _config.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _tasks = MutableStateFlow(INITIAL_TASKS)
    val tasks: StateFlow<List<LoadingTask>> = _tasks.asStateFlow()

    var onSetupComplete: (() -> Unit)? = null

    fun setQuality(quality: StreamingQuality) =
        _config.update { it.copy(streamingQuality = quality) }

    fun toggleGaplessPlayback() =
        _config.update { it.copy(enableGaplessPlayback = !it.enableGaplessPlayback) }

    /**
     * Called by the "Let's go" button in [SetupStep.CONFIG].
     * Advances to [SetupStep.LOADING] and starts the data pipeline.
     */
    fun startLoading() {
        _step.value = SetupStep.LOADING
        viewModelScope.launch { runLoadingPipeline() }
    }

    /**
     * Called by the "Start listening" button in [SetupStep.DONE].
     * Triggers navigation out of the setup graph.
     */
    fun navigateToMain() {
        onSetupComplete?.invoke()
    }

    private suspend fun runLoadingPipeline(failFast: Boolean = false) {
        _progress.value = 0f

        INITIAL_TASKS.forEachIndexed { index, task ->
            mutateTask(task.id) { it.copy(state = TaskState.RUNNING) }

            updateProgress()

            val success = try {
                executeTask(task.id)
            } catch (t: Throwable) {
                t.printStackTrace()
                false
            }

            if (success) {
                mutateTask(task.id) { it.copy(state = TaskState.DONE) }
            } else {
                mutateTask(task.id) { it.copy(state = TaskState.FAILED) }
                if (failFast) {
                    updateProgress()
                    return@runLoadingPipeline
                }
            }

            updateProgress()
        }

        // Brief pause so the user sees 100% before the DONE screen slides in.
        delay(450)

        if (!_tasks.value.any { it.state == TaskState.FAILED }) {
            _step.value = SetupStep.DONE
        }
    }

    private suspend fun executeTask(id: String): Boolean = when (id) {
        "connect" -> {
            try {
                spircController.start()
                val ok = withTimeoutOrNull(6_000L) {
                    while (!spirc.isUsable) {
                        delay(200)
                    }
                    true
                } ?: false

                ok
            } catch (t: Throwable) {
                t.printStackTrace()
                false
            }
        }
        "profile" -> {
            // TODO: Fetch username, avatar, ..
            true
        }
        "tracks" -> {
            try {
                val deferred = viewModelScope.async(Dispatchers.IO) {
                    withTimeoutOrNull(30_000L) {
                        likedRepository.syncLikedTracks()
                    } ?: true
                }

                val result = deferred.await()
                result
            } catch (t: Throwable) {
                t.printStackTrace()
                false
            }
        }

        "library" -> {
            try {
                withTimeoutOrNull(15_000L) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val uris = metadata.getPlaylistUris()
                        metadata.observePlaylists(uris)
                            .collect { /* ignore results */ }
                    }
                    true
                } ?: true
            } catch (t: Throwable) {
                t.printStackTrace()
                false
            }
        }
        else -> {
            try {
                delay(200)
                true
            } catch (t: Throwable) {
                false
            }
        }
    }

    private fun updateProgress() {
        val total = _tasks.value.size
        val doneOrFailed = _tasks.value.count { it.state == TaskState.DONE || it.state == TaskState.FAILED }
        _progress.value = (doneOrFailed.toFloat() / total.toFloat())
    }

    private fun mutateTask(id: String, transform: (LoadingTask) -> LoadingTask) {
        _tasks.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    companion object {
        private val INITIAL_TASKS = listOf(
            LoadingTask(id = "connect",    label = "Connecting to spotify",       icon = "\uD83D\uDD17"),
            LoadingTask(id = "profile",    label = "Fetching your profile",      icon = "\uD83E\uDDD1\uD83C\uDFFD"),
            LoadingTask(id = "tracks",    label = "Fetching your liked tracks",  icon = "❤"),
            LoadingTask(id = "library",   label = "Fetching your library",       icon = "📚"),
        )
    }
}