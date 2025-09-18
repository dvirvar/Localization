@file:OptIn(ExperimentalSerializationApi::class,FlowPreview::class,ExperimentalMaterial3Api::class)

package com.localization.offline.ui.screen

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
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
import com.localization.offline.store.ProcessingStore
import com.localization.offline.ui.view.ButtonWithLoader
import com.localization.offline.ui.view.SaveableButtonsTextField
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
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
import localization.composeapp.generated.resources.search
import localization.composeapp.generated.resources.yes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.awt.Desktop
import java.io.File

class TranslatorVM(private val filePath: String, val type: Type): ViewModel() {
    @Serializable
    enum class Type {
        Export,
        Import
    }
    val languages: List<ExportToTranslator.Language>
    val searchText = MutableStateFlow("")
    val ett: MutableStateFlow<SnapshotStateList<ExportToTranslator.KeyValues>>
    val translations: Flow<SnapshotStateList<ExportToTranslator.KeyValues>>
    val showImportLoader = combine(ProcessingStore.importTranslations, ProcessingStore.exportAndOverwriteTranslations, ProcessingStore.exportTranslationsAsZip) { i, eao, eaz ->
        i || eao || eaz
    }
    val showSaveLoader = MutableStateFlow(false)
    val showSaveSuccessDialog = MutableStateFlow(false)
    val popScreen = MutableSharedFlow<Boolean>()

    init {
        val exportToTranslator: ExportToTranslator = File(filePath).inputStream().use {
            Json.decodeFromStream(it)
        }
        languages = exportToTranslator.languages
        ett = MutableStateFlow(exportToTranslator.keyValuesAsObservable())
        translations = searchText.debounce{if (it.isBlank()) 0 else 200}.combine(ett) { st, kvs ->
            if (st.isBlank()) {
                kvs
            } else {
                kvs.filterTo(SnapshotStateList()) {
                    it.name.contains(st, true) || it.description.contains(st, true) || it.values.fastAny { it.value.contains(st, true) }
                }
            }
        }
    }

    fun updateTranslation(keyName: String, languageId: Int, value: String) {
        val translationIndex = ett.value.binarySearch { it.name.compareTo(keyName) }
        val valueIndex = ett.value[translationIndex].values.indexOfFirst { it.languageId == languageId }
        if (valueIndex == -1) {
            (ett.value[translationIndex].values as SnapshotStateList<ExportToTranslator.KeyValues.Value>).add(ExportToTranslator.KeyValues.Value(languageId, value))
        } else {
            (ett.value[translationIndex].values as SnapshotStateList<ExportToTranslator.KeyValues.Value>)[valueIndex] = ett.value[translationIndex].values[valueIndex].copy(value = value)
        }
    }

    fun save() {
        showSaveLoader.value = true
        val keyValues = ett.value.fastMap {
            ExportToTranslator.KeyValues(it.id, it.name, it.description, it.values)
        }
        val ett = ExportToTranslator(languages, keyValues)
        File(filePath).outputStream().use {
            Json.encodeToStream(ett, it)
        }
        showSaveLoader.value = false
        showSaveSuccessDialog.value = true
    }

    fun openInFileExplorer() {
        Desktop.getDesktop().tryBrowseAndHighlight(File(filePath))
    }

    fun import() {
        val values = mutableListOf<TranslationValueEntity>()
        ett.value.fastForEach { translation ->
            values.addAll(translation.values.fastMap { TranslationValueEntity(translation.id, it.languageId, it.value) })
        }
        viewModelScope.launch {
            TranslationService().updateTranslations(values)
            popScreen.emit(true)
        }
    }
}

@Composable
fun TranslatorScreen(navController: NavController, filePath: String, type: TranslatorVM.Type) {
    val vm = koinViewModel<TranslatorVM>(parameters = { parametersOf(filePath, type) })
    val searchText by vm.searchText.collectAsStateWithLifecycle()
    val translations by vm.translations.collectAsStateWithLifecycle(emptyList())
    val showImportLoader by vm.showImportLoader.collectAsStateWithLifecycle(false)
    val showSaveLoader by vm.showSaveLoader.collectAsStateWithLifecycle()
    val showSaveSuccessDialog by vm.showSaveSuccessDialog.collectAsStateWithLifecycle()
    val popScreen by vm.popScreen.collectAsStateWithLifecycle(false)

    val lazyColumnState = rememberLazyListState()

    LaunchedEffect(popScreen) {
        if (popScreen) {
            navController.popBackStack()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(3.dp, Alignment.End), Alignment.CenterVertically) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        searchText,
                        {vm.searchText.value = it},
                        {},
                        false,
                        {},
                        Modifier.width(250.dp),
                        placeholder = { Text(stringResource(Res.string.search)) },
                        trailingIcon = { Icon(Icons.Outlined.Search, "search") },
                    )
                },
                false,
                {},
                Modifier.padding(bottom = 8.dp)
            ) {}
        }
        HorizontalDivider()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(Modifier.fillMaxSize(), lazyColumnState) {
                items(translations, { it.id }) { translation ->
                    LocalizationRow({ languageId, value ->
                        vm.updateTranslation(translation.name, languageId, value)
                    }, translation, vm.languages)
                    HorizontalDivider()
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
                ButtonWithLoader(vm::save, true, showSaveLoader, stringResource(Res.string.save))
            } else {
                ButtonWithLoader(vm::import, true, showImportLoader, stringResource(Res.string.import))
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
    translations: ExportToTranslator.KeyValues,
    languages: List<ExportToTranslator.Language>
) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(250.dp).padding(horizontal = 10.dp)) {
            Text(translations.name, style = MaterialTheme.typography.titleMedium)
            Text(translations.description, style = MaterialTheme.typography.bodyMedium)
        }
        VerticalDivider()
        Column(Modifier.weight(1f).padding(4.dp)) {
            languages.fastForEach { language ->
                SaveableButtonsTextField(
                    {onSave(language.id, it)},
                    translations.values.fastFirstOrNull { it.languageId ==  language.id }?.value ?: "",
                    label =  { Text(language.name) },
                    readOnly = language.readOnly
                )
            }
        }
    }
}