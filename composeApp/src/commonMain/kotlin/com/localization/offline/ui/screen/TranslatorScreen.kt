package com.localization.offline.ui.screen

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.localization.offline.db.TranslationValueEntity
import com.localization.offline.extension.tryBrowseAndHighlight
import com.localization.offline.model.ExportToTranslator
import com.localization.offline.service.TranslationService
import com.localization.offline.ui.view.SaveableButtonsTextField
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.back
import localization.composeapp.generated.resources.import
import localization.composeapp.generated.resources.no
import localization.composeapp.generated.resources.open_in_file_explorer_q
import localization.composeapp.generated.resources.save
import localization.composeapp.generated.resources.saved_successfully
import localization.composeapp.generated.resources.yes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.awt.Desktop
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
class TranslatorVM(private val filePath: String, typeName: String): ViewModel() {
    enum class Type {
        Export,
        Import
    }
    data class Key(
        val id: String,
        val name: String,
        val description: String
    )
    val type = Type.valueOf(typeName)
    val languages: List<ExportToTranslator.Language>
    val translationKeys: List<Key>
    val translationValues: SnapshotStateMap<String, SnapshotStateList<ExportToTranslator.KeyValues.Value>>
    val showSaveSuccessDialog = MutableStateFlow(false)
    val popScreen = MutableSharedFlow<Boolean>()

    init {
        val exportToTranslator: ExportToTranslator = File(filePath).inputStream().use {
            Json.decodeFromStream(it)
        }
        languages = exportToTranslator.languages
        val keys = mutableListOf<Key>()
        val values = mutableStateMapOf<String, SnapshotStateList<ExportToTranslator.KeyValues.Value>>()
        exportToTranslator.keyValues.fastForEach { keyValues ->
            keys.add(Key(keyValues.id, keyValues.name, keyValues.description))
            values[keyValues.name] = keyValues.values.toMutableStateList()
        }
        translationKeys = keys
        translationValues = values
    }

    fun updateTranslation(keyName: String, languageId: Int, value: String) {
        val valueIndex = translationValues[keyName]!!.indexOfFirst { it.languageId == languageId }
        if (valueIndex == -1) {
            translationValues[keyName]!!.add(ExportToTranslator.KeyValues.Value(languageId, value))
        } else {
            translationValues[keyName]!![valueIndex] = translationValues[keyName]!![valueIndex].copy(value = value)
        }
    }

    fun save() {
        val keyValues = translationKeys.fastMap {
            ExportToTranslator.KeyValues(it.id, it.name, it.description, translationValues[it.name] ?: mutableListOf())
        }
        val ett = ExportToTranslator(languages, keyValues)
        File(filePath).outputStream().use {
            Json.encodeToStream(ett, it)
        }
        showSaveSuccessDialog.value = true
    }

    fun openInFileExplorer() {
        Desktop.getDesktop().tryBrowseAndHighlight(File(filePath))
    }

    fun import() {
        val values = mutableListOf<TranslationValueEntity>()
        translationKeys.fastForEach { key ->
            values.addAll(translationValues[key.name]!!.fastMap { TranslationValueEntity(key.id, it.languageId, it.value) })
        }
        viewModelScope.launch {
            TranslationService().updateTranslations(values)
            popScreen.emit(true)
        }
    }
}

@Composable
fun TranslatorScreen(navController: NavController, filePath: String, typeName: String) {
    val vm = koinViewModel<TranslatorVM>(parameters = { parametersOf(filePath, typeName) })
    val showSaveSuccessDialog by vm.showSaveSuccessDialog.collectAsStateWithLifecycle()
    val popScreen by vm.popScreen.collectAsStateWithLifecycle(false)

    val lazyColumnState = rememberLazyListState()

    LaunchedEffect(popScreen) {
        if (popScreen) {
            navController.popBackStack()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(Modifier.fillMaxSize(), lazyColumnState) {
                items(vm.translationKeys) {
                    HorizontalDivider()
                    LocalizationRow({ languageId, value ->
                        vm.updateTranslation(it.name, languageId, value)
                    }, it,vm.translationValues[it.name] ?: listOf(), vm.languages)
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(lazyColumnState), Modifier.align(Alignment.CenterEnd))
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 3.dp, start = 6.dp, end = 6.dp), verticalAlignment = Alignment.Bottom) {
            Button({navController.popBackStack()}) {
                Text(stringResource(Res.string.back))
            }
            Spacer(Modifier.weight(1f))
            if (vm.type == TranslatorVM.Type.Export) {
                Button(vm::save) {
                    Text(stringResource(Res.string.save))
                }
            } else {
                Button(vm::import) {
                    Text(stringResource(Res.string.import))
                }
            }
        }
    }

    if (showSaveSuccessDialog) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.saved_successfully))
            },
            text = {
                Text(stringResource(Res.string.open_in_file_explorer_q))
            },
            dismissButton = {
                Button({vm.showSaveSuccessDialog.value = false}) {
                    Text(stringResource(Res.string.no))
                }
            },
            confirmButton = {
                Button({
                    vm.showSaveSuccessDialog.value = false
                    vm.openInFileExplorer()
                }) {
                    Text(stringResource(Res.string.yes))
                }
            })
    }
}

@Composable
private fun LocalizationRow(
    onSave: (languageId: Int, value: String) -> Unit,
    key: TranslatorVM.Key,
    values: List<ExportToTranslator.KeyValues.Value>,
    languages: List<ExportToTranslator.Language>
) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(250.dp).padding(horizontal = 10.dp)) {
            Text(key.name, style = MaterialTheme.typography.titleMedium)
            Text(key.description, style = MaterialTheme.typography.bodyMedium)
        }
        VerticalDivider()
        Column(Modifier.weight(1f).padding(4.dp)) {
            languages.fastForEach { language ->
                SaveableButtonsTextField({onSave(language.id, it)}, values.fastFirstOrNull { it.languageId ==  language.id }?.value ?: "", textFieldModifier = Modifier.fillMaxWidth(), label =  { Text(language.name) }, readOnly = language.readOnly)
            }
        }
    }
}