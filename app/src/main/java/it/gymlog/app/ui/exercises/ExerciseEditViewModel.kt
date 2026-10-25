package it.gymlog.app.ui.exercises

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.db.ExercisePhoto
import it.gymlog.app.data.db.LengthBias
import it.gymlog.app.data.db.Muscle
import it.gymlog.app.data.repo.DeleteResult
import it.gymlog.app.data.repo.DuplicateHevyNameException
import it.gymlog.app.data.repo.DuplicateNameException
import it.gymlog.app.data.repo.ExerciseDraft
import it.gymlog.app.data.repo.ExerciseRepository
import it.gymlog.app.data.repo.PhotoStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import it.gymlog.app.ui.common.formatKg
import it.gymlog.app.ui.common.parseDecimal
import java.io.File

data class ExerciseForm(
    val name: String = "",
    val primary: Muscle = Muscle.PETTO,
    val secondary: Set<Muscle> = emptySet(),
    val bias: LengthBias = LengthBias.NONE,
    val notes: String = "",
    val hevyName: String = "",
    val loadStep: String = "",
    val single: Boolean = false,
    val nameError: String? = null,
    val hevyNameError: String? = null,
    val used: Boolean = false,
)

class ExerciseEditViewModel(
    initialId: Long,
    private val repo: ExerciseRepository,
    private val photoStore: PhotoStore,
) : ViewModel() {
    /** 0 finché l'esercizio nuovo non viene salvato la prima volta. */
    private val _id = MutableStateFlow(initialId)
    val id: StateFlow<Long> = _id.asStateFlow()

    private val _form = MutableStateFlow(ExerciseForm())
    val form: StateFlow<ExerciseForm> = _form.asStateFlow()

    val photos: StateFlow<List<ExercisePhoto>> =
        (if (initialId == 0L) flowOf(emptyList()) else repo.observe(initialId).map { it?.sortedPhotos.orEmpty() })
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _newPhotos = MutableStateFlow<List<File>>(emptyList())
    /** Foto scattate prima del primo salvataggio di un esercizio nuovo. */
    val pendingPhotos: StateFlow<List<File>> = _newPhotos.asStateFlow()

    init {
        if (initialId != 0L) viewModelScope.launch {
            repo.get(initialId)?.let { e ->
                _form.value = ExerciseForm(
                    name = e.exercise.name,
                    primary = e.exercise.primaryMuscle,
                    secondary = e.secondary.map { it.muscle }.toSet(),
                    bias = e.exercise.lengthBias,
                    notes = e.exercise.executionNotes,
                    hevyName = e.exercise.hevyName.orEmpty(),
                    loadStep = e.exercise.loadStepKg?.let { kg -> formatKg(kg) }.orEmpty(),
                    single = e.exercise.single,
                )
            }
        }
    }

    fun edit(transform: (ExerciseForm) -> ExerciseForm) = _form.update { transform(it).copy(nameError = null, hevyNameError = null) }

    fun newCameraFile(): File = photoStore.newCameraFile()

    fun onPhotoTaken(file: File, success: Boolean) {
        if (!success || !file.exists() || file.length() == 0L) { file.delete(); return }
        addPhotoFile(file)
    }

    fun onPhotoPicked(uri: Uri, resolver: ContentResolver) = viewModelScope.launch {
        runCatching { photoStore.importFrom(uri, resolver) }.onSuccess(::addPhotoFile)
    }

    private fun addPhotoFile(file: File) {
        val current = _id.value
        if (current == 0L) _newPhotos.update { it + file }
        else viewModelScope.launch { repo.addPhoto(current, file) }
    }

    fun removePhoto(p: ExercisePhoto) = viewModelScope.launch { repo.removePhoto(p) }
    fun movePhoto(p: ExercisePhoto, up: Boolean) = viewModelScope.launch { repo.movePhoto(p, up) }
    fun removePending(f: File) { f.delete(); _newPhotos.update { it - f } }

    /** Salva e chiama [onDone] se va a buon fine. */
    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val f = _form.value
        if (f.name.isBlank()) { _form.update { it.copy(nameError = "Il nome è obbligatorio") }; return@launch }
        try {
            val saved = repo.save(
                ExerciseDraft(
                    id = _id.value, name = f.name, primary = f.primary, secondary = f.secondary.toList(),
                    bias = f.bias, notes = f.notes, hevyName = f.hevyName,
                    loadStepKg = parseDecimal(f.loadStep)?.takeIf { it > 0 },
                    single = f.single,
                )
            )
            _newPhotos.value.forEach { repo.addPhoto(saved, it) }
            _newPhotos.value = emptyList()
            _id.value = saved
            onDone()
        } catch (e: DuplicateNameException) {
            _form.update { it.copy(nameError = e.message) }
        } catch (e: DuplicateHevyNameException) {
            _form.update { it.copy(hevyNameError = e.message) }
        }
    }

    fun delete(onDone: (DeleteResult) -> Unit) = viewModelScope.launch {
        val current = _id.value
        _newPhotos.value.forEach { it.delete() }
        onDone(if (current == 0L) DeleteResult.DELETED else repo.deleteOrArchive(current))
    }

    suspend fun isUsed(): Boolean = _id.value != 0L && repo.isUsed(_id.value)
}
