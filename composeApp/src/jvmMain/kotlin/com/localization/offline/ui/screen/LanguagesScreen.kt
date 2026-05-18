package com.localization.offline.ui.screen

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.localization.offline.db.LanguageEntity
import com.localization.offline.db.LanguageExportSettingsEntity
import com.localization.offline.model.LanguageViewStyle
import com.localization.offline.service.LanguageService
import com.localization.offline.service.LocalDataService
import com.localization.offline.service.PlatformService
import com.localization.offline.ui.view.AppCard
import com.localization.offline.ui.view.AppDialog
import com.localization.offline.ui.view.AppTextField
import com.localization.offline.ui.view.AppTooltip
import com.localization.offline.ui.view.SaveableButtonsTextField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.add
import localization.composeapp.generated.resources.cancel
import localization.composeapp.generated.resources.delete
import localization.composeapp.generated.resources.delete_q
import localization.composeapp.generated.resources.drag_to_reorder
import localization.composeapp.generated.resources.language
import localization.composeapp.generated.resources.language_already_exist
import localization.composeapp.generated.resources.languages
import localization.composeapp.generated.resources.no
import localization.composeapp.generated.resources.ok
import localization.composeapp.generated.resources.yes
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.random.Random

class LanguagesVM: ViewModel() {
    private val languageService = LanguageService()
    val languages = languageService.getAllLanguagesAsFlow()
    val platforms = PlatformService().getAllPlatformsAsFlow()
    val languageViewStyles = LanguageViewStyle.entries
    val currentLanguageViewStyle = MutableStateFlow(LocalDataService.languageViewStyle)
    val showAddLanguageDialog = MutableStateFlow(false)
    val languageNameError = MutableStateFlow<StringResource?>(null)
    val showLanguageNameAlreadyExistDialog = MutableStateFlow(false)
    val languageToDeletion = MutableStateFlow<LanguageEntity?>(null)
    val showDeleteLanguageDialog = languageToDeletion.map {
        it != null
    }

    fun setLanguageViewStyle(style: LanguageViewStyle) {
        currentLanguageViewStyle.value = style
        LocalDataService.languageViewStyle = style
    }

    fun addLanguage(name: String, languageExportSettings: List<LanguageExportSettingsEntity>) {
        viewModelScope.launch {
            if (languageService.doesLanguageExist(name)) {
                languageNameError.value = Res.string.language_already_exist
                return@launch
            }
            val language = LanguageEntity(Random.nextInt(), name, languages.first().maxOf { it.orderPriority } + 1)
            val languageExportSettings = languageExportSettings.fastMap { it.copy(languageId = language.id) }
            languageService.addLanguage(language, languageExportSettings)
            showAddLanguageDialog.value = false
        }
    }

    fun editLanguageName(id: Int, originalName: String, name: String) {
        viewModelScope.launch {
            if (name != originalName) {
                if (languageService.doesLanguageExist(name, id)) {
                    showLanguageNameAlreadyExistDialog.value = true
                    return@launch
                }
            }
            languageService.updateLanguageName(id, name)
        }
    }

    fun editLanguageOrder(from: LanguageEntity, to: LanguageEntity) {
        viewModelScope.launch {
            languageService.updateLanguageOrder(from, to.orderPriority)
        }
    }

    fun setLanguageToDeletion(language: LanguageEntity?) {
        languageToDeletion.value = language
    }

    fun deleteLanguage() {
        viewModelScope.launch {
            languageService.deleteLanguage(languageToDeletion.value!!)
            setLanguageToDeletion(null)
        }
    }
}

@Composable
fun LanguagesScreen() {
    val vm = koinViewModel<LanguagesVM>()
    val languages by vm.languages.collectAsStateWithLifecycle(listOf())
    val currentLanguageViewStyle by vm.currentLanguageViewStyle.collectAsStateWithLifecycle()
    val showAddLanguageDialog by vm.showAddLanguageDialog.collectAsStateWithLifecycle()
    val showLanguageNameAlreadyExistDialog by vm.showLanguageNameAlreadyExistDialog.collectAsStateWithLifecycle()
    val showDeleteLanguageDialog by vm.showDeleteLanguageDialog.collectAsStateWithLifecycle(false)

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        vm.editLanguageOrder(languages[from.index], languages[to.index])
    }
    val lazyGridState = rememberLazyGridState()
    val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        vm.editLanguageOrder(languages[from.index], languages[to.index])
    }

    Column(Modifier.fillMaxSize().padding(36.dp)) {
        AppCard(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.languages), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow {
                    vm.languageViewStyles.fastForEachIndexed { index, style ->
                        SegmentedButton(
                            currentLanguageViewStyle == style,
                            {vm.setLanguageViewStyle(style)},
                            SegmentedButtonDefaults.itemShape(index, vm.languageViewStyles.size),
                        ) {
                            Text(stringResource(style.stringResource))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when(currentLanguageViewStyle) {
                    LanguageViewStyle.List -> {
                        LazyColumn(Modifier.matchParentSize(), lazyListState) {
                            items(languages, {it.id}) { language ->
                                ReorderableItem(reorderableLazyListState, language.id) {
                                    LanguageRow(this, language.name, languages.size > 1,
                                        {
                                            vm.editLanguageName(language.id, language.name, it)
                                        },
                                        {
                                            vm.setLanguageToDeletion(language)
                                        }
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(rememberScrollbarAdapter(lazyListState), Modifier.align(Alignment.CenterEnd))
                    }
                    LanguageViewStyle.Grid -> {
                        LazyVerticalGrid(
                            GridCells.Adaptive(250.dp),
                            Modifier.matchParentSize(),
                            lazyGridState
                        ) {
                            items(languages, {it.id}) { language ->
                                ReorderableItem(reorderableLazyGridState, language.id) {
                                    LanguageRow(this, language.name, languages.size > 1,
                                        {
                                            vm.editLanguageName(language.id, language.name, it)
                                        },
                                        {
                                            vm.setLanguageToDeletion(language)
                                        }
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(rememberScrollbarAdapter(lazyGridState), Modifier.align(Alignment.CenterEnd))
                    }
                }
            }
            Row {
                Button({
                    vm.showAddLanguageDialog.value = true
                }) {
                    Text(stringResource(Res.string.add))
                }
            }
        }
    }

    if (showAddLanguageDialog) {
        var language by remember { mutableStateOf("") }
        val languageError by vm.languageNameError.collectAsStateWithLifecycle()
        val platforms by vm.platforms.collectAsStateWithLifecycle(listOf())
        val languageExportSettings = remember(platforms) {
            mutableStateOf<List<LanguageExportSettingsEntity>>(listOf()).apply {
                value = platforms.fastMap { LanguageExportSettingsEntity(0, it.id, "", "") }
            }
        }

        val addButtonEnabled = remember(language, languageExportSettings.value) {
            var platformsAreValid = true
            for (platformIndex in platforms.indices) {
                val platform = platforms[platformIndex]
                if (platform.exportPrefix.isEmpty() && languageExportSettings.value[platformIndex].folderSuffix.isEmpty()) {
                    platformsAreValid = false
                    break
                }
            }
            platformsAreValid && language.isNotBlank() && languageExportSettings.value.all { it.fileName.isNotBlank() }
        }

        DisposableEffect(Unit) {
            onDispose {
                vm.languageNameError.value = null
            }
        }

        AppDialog {
            AppTextField(language, {
                language = it
                vm.languageNameError.value = null
            }, Modifier.width(TextFieldDefaults.MinWidth), label = { Text(stringResource(Res.string.language)) }, error = languageError?.let { stringResource(it) }, singleLine = true)
            Spacer(Modifier.height(6.dp))
            platforms.fastForEachIndexed { index, platform ->
                Text(platform.name, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(languageExportSettings.value[index].folderSuffix, {
                        languageExportSettings.value = languageExportSettings.value.toMutableList().apply {
                            this[index] = this[index].copy(folderSuffix = it)
                        }
                    }, Modifier.width(120.dp), singleLine = true, label = { Text(language, maxLines = 1) })
                    Text("/", Modifier.align(Alignment.CenterVertically), fontSize = 36.sp)
                    OutlinedTextField(languageExportSettings.value[index].fileName, {
                        languageExportSettings.value = languageExportSettings.value.toMutableList().apply {
                            this[index] = this[index].copy(fileName = it)
                        }
                    }, Modifier.width(120.dp), singleLine = true)
                    Spacer(Modifier.width(10.dp))
                    if (platform.exportPrefix.isNotEmpty() || languageExportSettings.value[index].folderSuffix.isNotEmpty() || languageExportSettings.value[index].fileName.isNotEmpty()) {
                        Text("${platform.exportPrefix}${languageExportSettings.value[index].folderSuffix}/${languageExportSettings.value[index].fileName}${platform.fileStructure.fileExtension}")
                    }
                }
            }
            Row(Modifier.align(Alignment.CenterHorizontally)) {
                Button({vm.showAddLanguageDialog.value = false}) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(10.dp))
                Button({
                    vm.addLanguage(language, languageExportSettings.value)
                }, enabled = addButtonEnabled) {
                    Text(stringResource(Res.string.add))
                }
            }
        }
    } else if (showLanguageNameAlreadyExistDialog) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.language_already_exist))
            },
            confirmButton = {
                Button({vm.showLanguageNameAlreadyExistDialog.value = false}) {
                    Text(stringResource(Res.string.ok))
                }
            })
    } else if (showDeleteLanguageDialog) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.delete_q, vm.languageToDeletion.value!!.name))
            },
            dismissButton = {
                Button({vm.setLanguageToDeletion(null)}) {
                    Text(stringResource(Res.string.no))
                }
            },
            confirmButton = {
                Button({vm.deleteLanguage()}) {
                    Text(stringResource(Res.string.yes))
                }
            }
        )
    }
}

@Composable
private fun LanguageRow(scope: ReorderableCollectionItemScope, language: String, showDelete: Boolean, onSave: (String) -> Unit, onDelete: () -> Unit) {
    SaveableButtonsTextField(
        onSave,
        language,
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showDelete) {
                    IconButton(onDelete) {
                        AppTooltip(stringResource(Res.string.delete)) {
                            Icon(Icons.Filled.DeleteForever, "delete language")
                        }
                    }
                }
                AppTooltip(stringResource(Res.string.drag_to_reorder)) {
                    Icon(Icons.Filled.DragHandle, "drag", with(scope) { Modifier.draggableHandle()})
                }
            }
        },
        singleLine = true
    )
}