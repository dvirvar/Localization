@file:OptIn(ExperimentalLayoutApi::class)

package com.localization.offline.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.localization.offline.db.LanguageEntity
import com.localization.offline.db.TranslationKeyEntity
import com.localization.offline.db.TranslationKeyPlatformEntity
import com.localization.offline.db.TranslationValueEntity
import com.localization.offline.service.LanguageService
import com.localization.offline.service.PlatformService
import com.localization.offline.service.TranslationService
import com.localization.offline.ui.view.AppTextField
import com.localization.offline.ui.view.SaveableButtonsTextField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.add
import localization.composeapp.generated.resources.cancel
import localization.composeapp.generated.resources.delete_q
import localization.composeapp.generated.resources.description
import localization.composeapp.generated.resources.key
import localization.composeapp.generated.resources.key_already_exist
import localization.composeapp.generated.resources.no
import localization.composeapp.generated.resources.save
import localization.composeapp.generated.resources.yes
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import java.util.UUID

class LocalizationVM: ViewModel() {
    private val translationService = TranslationService()
    val languages = LanguageService().getAllLanguages()
    val translationKeys = translationService.getAllKeys()
    val translationValues = translationService.getAllValues()
    val platforms = PlatformService().getAllPlatforms()
    val showAddTranslationDialog = MutableStateFlow(false)
    var translationKeyError = MutableStateFlow<StringResource?>(null)
    val translationKeyToEdit = MutableStateFlow<TranslationKeyEntity?>(null)
    var keyPlatformEditSelection = MutableStateFlow<List<Boolean>>(listOf())
    val showEditTranslationKeyDialog = MutableStateFlow(false)
    val translationToDeletion = MutableStateFlow<TranslationKeyEntity?>(null)
    val showDeleteTranslationDialog = translationToDeletion.map {
        it != null
    }

    fun addTranslation(key: String, description: String, platformsSelection: List<Boolean>, values: List<String>) {
        viewModelScope.launch {
            if (translationService.isKeyNameExist(key)) {
                translationKeyError.value = Res.string.key_already_exist
                return@launch
            }
            val keyEntity = TranslationKeyEntity(UUID.randomUUID().toString(), key, description)
            val valueEntities = mutableListOf<TranslationValueEntity>()
            languages.first().fastForEachIndexed { index, language ->
                val value = values[index]
                if (value.isNotEmpty()) {
                    valueEntities.add(TranslationValueEntity(keyEntity.id, language.id, value))
                }
            }
            val keyPlatformEntities = mutableListOf<TranslationKeyPlatformEntity>()
            platforms.first().fastForEachIndexed { index, platform ->
                if (platformsSelection[index]) {
                    keyPlatformEntities.add(TranslationKeyPlatformEntity(keyEntity.id, platform.id))
                }
            }
            translationService.addTranslation(keyEntity, valueEntities, keyPlatformEntities)
            showAddTranslationDialog.value = false
        }
    }

    fun updateTranslation(keyId: String, languageId: Int, value: String) {
        viewModelScope.launch {
            translationService.updateTranslation(TranslationValueEntity(keyId, languageId, value))
        }
    }

    fun setTranslationKeyToEdit(key: TranslationKeyEntity?) {
        translationKeyToEdit.value = key
        keyPlatformEditSelection.value = listOf()
        if (key != null) {
            viewModelScope.launch {
                val keyPlatformEntities = translationService.getKeyPlatform(key.id)
                val entities = mutableListOf<Boolean>()
                platforms.first().fastForEach { platform ->
                    entities.add(keyPlatformEntities.fastAny { it.platformId == platform.id})
                }
                keyPlatformEditSelection.value = entities
            }
        }
        showEditTranslationKeyDialog.value = key != null
    }

    fun updateTranslationKey(key: String, description: String) {
        viewModelScope.launch {
            val id = translationKeyToEdit.value!!.id
            if (translationKeyToEdit.value!!.key != key) {
                if (translationService.isKeyNameExist(key, id)) {
                    translationKeyError.value = Res.string.key_already_exist
                    return@launch
                }
            }
            val keyPlatformEntities = translationService.getKeyPlatform(id)
            val insertKeyPlatform = mutableListOf<TranslationKeyPlatformEntity>()
            val deleteKeyPlatform = mutableListOf<TranslationKeyPlatformEntity>()
            val platformsSelection = keyPlatformEditSelection.value
            platforms.first().fastForEachIndexed { index, platform ->
                val wasSelected = keyPlatformEntities.fastAny { it.platformId == platform.id }
                val isSelected = platformsSelection[index]
                if (!wasSelected && isSelected) {
                    insertKeyPlatform.add(TranslationKeyPlatformEntity(id, platform.id))
                } else if (wasSelected && !isSelected) {
                    deleteKeyPlatform.add(TranslationKeyPlatformEntity(id, platform.id))
                }
            }
            translationService.updateTranslationKey(id, key, description, insertKeyPlatform, deleteKeyPlatform)
            setTranslationKeyToEdit(null)
        }
    }

    fun setTranslationToDeletion(key: TranslationKeyEntity?) {
        translationToDeletion.value = key
    }

    fun deleteTranslation() {
        viewModelScope.launch {
            translationService.deleteTranslation(translationToDeletion.value!!.id)
            setTranslationToDeletion(null)
        }
    }
}

@Composable
fun LocalizationScreen() {
    val vm = koinViewModel<LocalizationVM>()
    val languages by vm.languages.collectAsStateWithLifecycle(listOf())
    val translationKeys by vm.translationKeys.collectAsStateWithLifecycle(listOf())
    val translationValues by vm.translationValues.collectAsStateWithLifecycle(hashMapOf())
    val showAddTranslationDialog by vm.showAddTranslationDialog.collectAsStateWithLifecycle()
    val showEditTranslationKeyDialog by vm.showEditTranslationKeyDialog.collectAsStateWithLifecycle()
    val showDeleteTranslationDialog by vm.showDeleteTranslationDialog.collectAsStateWithLifecycle(false)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), Arrangement.End) {
            Button({vm.showAddTranslationDialog.value = true}) {
                Text(stringResource(Res.string.add))
            }
        }
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize()) {
            items(translationKeys) {
                LocalizationRow({ languageId, value ->
                    vm.updateTranslation(it.id, languageId, value)
                }, vm::setTranslationKeyToEdit, vm::setTranslationToDeletion,
                    it, translationValues[it.id] ?: listOf(), languages)
                HorizontalDivider()
            }
        }
    }

    if (showAddTranslationDialog) {
        var key by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        val translations = remember { mutableStateListOf<String>().apply {
            addAll(languages.fastMap { "" })
        }}
        val keyError by vm.translationKeyError.collectAsStateWithLifecycle()
        val platforms by vm.platforms.collectAsStateWithLifecycle(listOf())
        //Was with mutableStateListOf but cant listen to changes so using regular mutable state
        val platformsSelection = remember(platforms) { mutableStateOf<List<Boolean>>(listOf()).apply {
            value = platforms.fastMap { true }
        }}

        val addButtonEnabled = remember(key, platformsSelection.value) {
            key.isNotEmpty() && platformsSelection.value.fastAny { it }
        }

        DisposableEffect(Unit) {
            onDispose {
                vm.translationKeyError.value = null
            }
        }

        //TODO: Size of dialog
        Dialog(onDismissRequest = {}) {
            Column(Modifier.wrapContentSize(unbounded = true).background(Color.White).padding(16.dp)) {
                AppTextField(key, {
                    key = it
                    vm.translationKeyError.value = null
                }, label = { Text(stringResource(Res.string.key)) }, error = keyError?.let { stringResource(it) })
                OutlinedTextField(description, {description = it}, label = { Text(stringResource(Res.string.description)) })
                languages.fastForEachIndexed { index, language ->
                    OutlinedTextField(translations[index], {translations[index] = it}, label = { Text(language.name) })
                }
                FlowRow(Modifier.fillMaxWidth()) {
                    platforms.fastForEachIndexed { index, platform ->
                        FilterChip(platformsSelection.value[index],
                            {
                                platformsSelection.value = platformsSelection.value.toMutableList().apply {
                                    this[index] = !platformsSelection.value[index]
                                }
                            },
                            { Text(platform.name) },
                            leadingIcon = if (platformsSelection.value[index]) {{ Icon(Icons.Filled.Done, "remove platform") }} else null)
                    }
                }
                Row(Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)) {
                    Button({vm.showAddTranslationDialog.value = false}) {
                        Text(stringResource(Res.string.cancel))
                    }
                    Spacer(Modifier.width(10.dp))
                    Button({
                        vm.addTranslation(key, description, platformsSelection.value, translations)
                    }, enabled = addButtonEnabled) {
                        Text(stringResource(Res.string.add))
                    }
                }
            }
        }
    } else if (showEditTranslationKeyDialog) {
        var key by remember { mutableStateOf(vm.translationKeyToEdit.value!!.key) }
        var description by remember { mutableStateOf(vm.translationKeyToEdit.value!!.description) }
        val keyError by vm.translationKeyError.collectAsStateWithLifecycle()
        val platforms by vm.platforms.collectAsStateWithLifecycle(listOf())
        val platformsSelection by vm.keyPlatformEditSelection.collectAsStateWithLifecycle()
        val saveButtonEnabled = remember(key, platformsSelection) {
            key.isNotEmpty() && vm.keyPlatformEditSelection.value.fastAny { it }
        }

        DisposableEffect(Unit) {
            onDispose {
                vm.translationKeyError.value = null
            }
        }

        Dialog(onDismissRequest = {}) {
            Column(Modifier.wrapContentSize(unbounded = true).background(Color.White).padding(16.dp)) {
                AppTextField(
                    key,
                    {
                        key = it
                        vm.translationKeyError.value = null
                    },
                    label = { Text(stringResource(Res.string.key)) },
                    error = keyError?.let { stringResource(it) }
                )
                OutlinedTextField(
                    description,
                    { description = it },
                    label = { Text(stringResource(Res.string.description)) }
                )
                FlowRow(Modifier.fillMaxWidth()) {
                    platforms.fastForEachIndexed { index, platform ->
                        FilterChip(platformsSelection[index],
                            {
                                vm.keyPlatformEditSelection.value = platformsSelection.toMutableList().apply {
                                    this[index] = !platformsSelection[index]
                                }
                            },
                            { Text(platform.name) },
                            leadingIcon = if (platformsSelection[index]) {{ Icon(Icons.Filled.Done, "remove platform") }} else null)
                    }
                }
                Row(Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)) {
                    Button({vm.setTranslationKeyToEdit(null)}) {
                        Text(stringResource(Res.string.cancel))
                    }
                    Spacer(Modifier.width(10.dp))
                    Button({ vm.updateTranslationKey(key, description) }, enabled = saveButtonEnabled) {
                        Text(stringResource(Res.string.save))
                    }
                }
            }
        }
    } else if (showDeleteTranslationDialog) {
        AlertDialog({},
            title = {
                Text(stringResource(Res.string.delete_q, vm.translationToDeletion.value!!.key))
            },
            dismissButton = {
                Button({vm.setTranslationToDeletion(null)}) {
                    Text(stringResource(Res.string.no))
                }
            },
            confirmButton = {
                Button({vm.deleteTranslation()}) {
                    Text(stringResource(Res.string.yes))
                }
            },
        )
    }
}

@Composable
private fun LocalizationRow(onSave: (languageId: Int, value: String) -> Unit, onKeyEdit: (TranslationKeyEntity) -> Unit, onDelete: (TranslationKeyEntity) -> Unit, key: TranslationKeyEntity, values: List<TranslationValueEntity>, languages: List<LanguageEntity>) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(250.dp)) {
            FlowRow(Modifier.fillMaxWidth()) {
                TextButton({ onKeyEdit(key) }) {
                    Text(key.key)
                }
                IconButton({onDelete(key)}, Modifier.size(18.dp).align(Alignment.Bottom)) {
                    Icon(Icons.Filled.DeleteForever, "delete")
                }
            }
            Text(key.description, Modifier.padding(horizontal = 10.dp), style = MaterialTheme.typography.bodyMedium)
        }
        VerticalDivider()
        Column(Modifier.weight(1f).padding(4.dp)) {
            languages.fastForEach { language ->
                SaveableButtonsTextField({onSave(language.id, it)}, values.fastFirstOrNull { it.languageId ==  language.id}?.value ?: "", textFieldModifier = Modifier.fillMaxWidth(), label =  { Text(language.name) })
            }
        }
    }
}