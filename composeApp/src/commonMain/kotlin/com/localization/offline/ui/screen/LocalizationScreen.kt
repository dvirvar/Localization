@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, FlowPreview::class)

package com.localization.offline.ui.screen

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.localization.offline.db.LanguageEntity
import com.localization.offline.db.TranslationKeyEntity
import com.localization.offline.db.TranslationKeyPlatformEntity
import com.localization.offline.db.TranslationValueEntity
import com.localization.offline.extension.moveFocusOnTab
import com.localization.offline.service.LanguageService
import com.localization.offline.service.PlatformService
import com.localization.offline.service.TranslationService
import com.localization.offline.ui.view.AppDialog
import com.localization.offline.ui.view.AppTextField
import com.localization.offline.ui.view.AppTooltip
import com.localization.offline.ui.view.SaveableButtonsTextField
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import localization.composeapp.generated.resources.Res
import localization.composeapp.generated.resources.add
import localization.composeapp.generated.resources.app_format_specifier
import localization.composeapp.generated.resources.app_format_specifier_description
import localization.composeapp.generated.resources.cancel
import localization.composeapp.generated.resources.delete
import localization.composeapp.generated.resources.delete_q
import localization.composeapp.generated.resources.description
import localization.composeapp.generated.resources.edit
import localization.composeapp.generated.resources.java_format_specifier
import localization.composeapp.generated.resources.key
import localization.composeapp.generated.resources.key_already_exist
import localization.composeapp.generated.resources.no
import localization.composeapp.generated.resources.save
import localization.composeapp.generated.resources.search
import localization.composeapp.generated.resources.show_all_keys
import localization.composeapp.generated.resources.show_untranslated_keys
import localization.composeapp.generated.resources.yes
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import java.util.UUID

class LocalizationVM: ViewModel() {
    private val translationService = TranslationService()
    val searchText = MutableStateFlow("")
    val showOnlyUntranslatedKeys = MutableStateFlow(false)
    val languages = LanguageService().getAllLanguages()
    private val searchCombinedWithKeyValues = searchText.debounce{if (it.isBlank()) 0 else 200}.combine(translationService.getAllKeysWithValues()) { st, kvs ->
        if (st.isBlank()) {
            kvs
        } else {
            kvs.fastFilter {
                it.key.key.contains(st, true) || it.key.description.contains(st, true) || it.values.fastAny { it.value.contains(st, true) }
            }
        }
    }
    val translationKeyWithValues = combine(searchCombinedWithKeyValues, showOnlyUntranslatedKeys, languages) { t, o, l ->
        if (o) {
            val lSize = l.size
            t.fastFilter { it.values.size != lSize }
        } else {
            t
        }
    }
    val platforms = PlatformService().getAllPlatforms()
    val showAppFormatSpecifierDescriptionDialog = MutableStateFlow(false)
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
    val searchText by vm.searchText.collectAsStateWithLifecycle()
    val languages by vm.languages.collectAsStateWithLifecycle(listOf())
    val translationKeyWithValues by vm.translationKeyWithValues.collectAsStateWithLifecycle(listOf())
    val showOnlyUntranslatedKeys by vm.showOnlyUntranslatedKeys.collectAsStateWithLifecycle()
    val showAppFormatSpecifierDescriptionDialog by vm.showAppFormatSpecifierDescriptionDialog.collectAsStateWithLifecycle()
    val showAddTranslationDialog by vm.showAddTranslationDialog.collectAsStateWithLifecycle()
    val showEditTranslationKeyDialog by vm.showEditTranslationKeyDialog.collectAsStateWithLifecycle()
    val showDeleteTranslationDialog by vm.showDeleteTranslationDialog.collectAsStateWithLifecycle(false)

    val lazyColumnState = rememberLazyListState()

    val appFormatSpecifierDescription = buildAnnotatedString {
        val text = stringResource(Res.string.app_format_specifier_description)
        append(text)
        val javaFormatSpecifier = stringResource(Res.string.java_format_specifier)
        val startIndex = text.indexOf(javaFormatSpecifier)
        if (startIndex == -1) {
            return@buildAnnotatedString
        }
        val endIndex = startIndex + javaFormatSpecifier.length
        addLink(LinkAnnotation.Url("https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Formatter.html", TextLinkStyles(
            SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
        )), startIndex, endIndex)
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(3.dp, Alignment.End), Alignment.CenterVertically) {
            IconButton({vm.showAppFormatSpecifierDescriptionDialog.value = true}) {
                Icon(Icons.Filled.Info, "add info")
            }
            Button({vm.showAddTranslationDialog.value = true}) {
                Text(stringResource(Res.string.add))
            }
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
            IconToggleButton(showOnlyUntranslatedKeys, {vm.showOnlyUntranslatedKeys.value = it}, colors = IconButtonDefaults.iconToggleButtonColors(contentColor = Color.Gray, checkedContentColor = MaterialTheme.colorScheme.primary)) {
                val text = stringResource(if (showOnlyUntranslatedKeys) Res.string.show_all_keys else Res.string.show_untranslated_keys)
                AppTooltip(text) {
                    Icon(Icons.Outlined.Translate, text)
                }
            }
        }
        HorizontalDivider()
        Box(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), lazyColumnState) {
                items(translationKeyWithValues, key = {it.key.id}) {
                    LocalizationRow({ languageId, value ->
                        vm.updateTranslation(it.key.id, languageId, value)
                    }, vm::setTranslationKeyToEdit, vm::setTranslationToDeletion,
                        it.key, it.values, languages)
                    HorizontalDivider()
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(lazyColumnState), Modifier.align(Alignment.CenterEnd))
        }
    }

    if (showAppFormatSpecifierDescriptionDialog) {
        AppDialog(onDismissRequest = {vm.showAppFormatSpecifierDescriptionDialog.value = false}) {
            Text(stringResource(Res.string.app_format_specifier), style = MaterialTheme.typography.titleMedium)
            Text(appFormatSpecifierDescription, style = MaterialTheme.typography.bodyMedium)
        }
    } else if (showAddTranslationDialog) {
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

        AppDialog {
            AppTextField(key, {
                key = it
                vm.translationKeyError.value = null
            }, label = { Text(stringResource(Res.string.key)) }, error = keyError?.let { stringResource(it) }, singleLine = true)
            OutlinedTextField(description, {description = it}, Modifier.moveFocusOnTab(), label = { Text(stringResource(Res.string.description)) })
            languages.fastForEachIndexed { index, language ->
                OutlinedTextField(translations[index], {translations[index] = it}, Modifier.moveFocusOnTab(), label = { Text(language.name) })
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
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
            Row(Modifier.align(Alignment.CenterHorizontally)) {
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

        AppDialog {
            AppTextField(
                key,
                {
                    key = it
                    vm.translationKeyError.value = null
                },
                Modifier.width(TextFieldDefaults.MinWidth),
                label = { Text(stringResource(Res.string.key)) },
                error = keyError?.let { stringResource(it) },
                singleLine = true
            )
            OutlinedTextField(
                description,
                { description = it },
                label = { Text(stringResource(Res.string.description)) }
            )
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
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
            Row(Modifier.align(Alignment.CenterHorizontally)) {
                Button({vm.setTranslationKeyToEdit(null)}) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(Modifier.width(10.dp))
                Button({ vm.updateTranslationKey(key, description) }, enabled = saveButtonEnabled) {
                    Text(stringResource(Res.string.save))
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
                    AppTooltip(stringResource(Res.string.edit)) {
                        Text(key.key)
                    }
                }
                IconButton({onDelete(key)}, Modifier.size(18.dp).align(Alignment.Bottom)) {
                    AppTooltip(stringResource(Res.string.delete)) {
                        Icon(Icons.Filled.DeleteForever, "delete")
                    }
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