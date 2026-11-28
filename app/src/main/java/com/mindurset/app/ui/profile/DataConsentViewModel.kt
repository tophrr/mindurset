package com.mindurset.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.repository.LocalDataWiper
import com.mindurset.app.domain.usecase.ExportLocalData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ExportStatus { Saved, Failed }

data class DataConsentUiState(
    val exporting: Boolean = false,
    val wiping: Boolean = false,
    val exportStatus: ExportStatus? = null,
)

/**
 * Coordinates JSON data export and confirmed consent withdrawal / full local wipe
 * on the Data & consent screen.
 */
class DataConsentViewModel(
    private val exporter: ExportLocalData,
    private val dataWiper: LocalDataWiper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope = externalScope ?: viewModelScope
    private val _uiState = MutableStateFlow(DataConsentUiState())
    val uiState: StateFlow<DataConsentUiState> = _uiState.asStateFlow()

    fun export(writeDocument: suspend (String) -> Boolean) {
        val current = _uiState.value
        if (current.exporting || current.wiping) return
        _uiState.update { it.copy(exporting = true, exportStatus = null) }
        scope.launch {
            val saved = withContext(ioDispatcher) {
                runCatching {
                    val json = exporter.exportJson()
                    writeDocument(json)
                }.getOrDefault(false)
            }
            _uiState.update {
                it.copy(
                    exporting = false,
                    exportStatus = if (saved) ExportStatus.Saved else ExportStatus.Failed,
                )
            }
        }
    }

    fun revokeAndWipe(onWithdrawn: () -> Unit) {
        val current = _uiState.value
        if (current.wiping || current.exporting) return
        _uiState.update { it.copy(wiping = true, exportStatus = null) }
        scope.launch {
            dataWiper.wipeAll()
            _uiState.update { it.copy(wiping = false) }
            onWithdrawn()
        }
    }
}
