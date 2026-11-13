package com.example.patienthealth.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.patienthealth.domain.parseDate
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(title) }, navigationIcon = {
            if (onBack != null) TextButton(onClick = onBack) { Text("Back") }
        }, actions = { actions() })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, keyboard: KeyboardType = KeyboardType.Text, tag: String = label) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth().testTag(tag),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard), singleLine = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Choice(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).testTag(label))
        ExposedDropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onChange(option); expanded = false })
            }
        }
    }
}

@Composable
fun DateField(label: String, value: String, minimum: LocalDate? = null, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val date = parseDate(value) ?: LocalDate.now()
    Button(onClick = {
        DatePickerDialog(context, { _, year, month, day -> onChange(LocalDate.of(year, month + 1, day).toString()) }, date.year, date.monthValue - 1, date.dayOfMonth).apply {
            datePicker.maxDate = System.currentTimeMillis()
            minimum?.let { datePicker.minDate = it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
        }.show()
    }, modifier = Modifier.testTag(label)) { Text("$label: ${value.ifBlank { "Select date" }}") }
}

@Composable
fun SectionTitle(title: String) { Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }

@Composable
fun PrototypeNotice() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Local prototype · synthetic data only", style = MaterialTheme.typography.titleSmall)
            Text("Not connected to a clinical backend. Reference ranges are illustrative adult values, not medical advice.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ValidationErrors(errors: List<String>) {
    if (errors.isNotEmpty()) {
        SectionTitle("Before saving")
        errors.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
