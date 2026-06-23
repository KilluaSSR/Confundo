package killua.dev.confundo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import killua.dev.confundo.R
import killua.dev.confundo.data.FieldInputType
import killua.dev.confundo.data.FieldSpec

/**
 * 根据 [FieldSpec.inputType] 渲染对应输入控件的对话框：
 * - [FieldInputType.Number]：仅接受数字（可选小数点），trailing 处提供单位下拉。
 * - [FieldInputType.Enum]：下拉单选。
 * - [FieldInputType.Boolean]：开关。
 * - [FieldInputType.Text]：文本。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldInputDialog(
    spec: FieldSpec?,
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (spec == null) return

    when (val type = spec.inputType) {
        is FieldInputType.Bool -> BooleanDialog(title, initialValue, onConfirm, onDismiss)
        is FieldInputType.Enum -> EnumDialog(title, type.options, initialValue, onConfirm, onDismiss)
        is FieldInputType.Number -> NumberDialog(title, type, initialValue, onConfirm, onDismiss)
        FieldInputType.Text -> TextDialog(title, initialValue, onConfirm, onDismiss)
    }
}

@Composable
private fun TextDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember(title) { mutableStateOf(initialValue) }
    BaseDialog(title, onDismiss, { onConfirm(input.trim()) }) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun NumberDialog(
    title: String,
    type: FieldInputType.Number,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val (initNumber, initUnit) = remember(title) { splitNumberUnit(initialValue, type.units) }
    var number by remember(title) { mutableStateOf(initNumber) }
    var unit by remember(title) {
        mutableStateOf(initUnit ?: type.units.firstOrNull() ?: "")
    }
    var unitMenu by remember { mutableStateOf(false) }

    val isValid = number.isEmpty() || number.toDoubleOrNull() != null

    BaseDialog(
        title = title,
        onDismiss = onDismiss,
        onConfirm = {
            val n = number.trim()
            val composed = when {
                n.isEmpty() -> ""
                type.units.isEmpty() -> n
                else -> "$n $unit"
            }
            onConfirm(composed)
        },
        confirmEnabled = isValid,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = number,
                onValueChange = { new ->
                    number = new.filter { it.isDigit() || (type.decimal && it == '.') }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = !isValid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (type.decimal) KeyboardType.Decimal else KeyboardType.Number
                ),
                supportingText = if (!isValid) {
                    { Text(stringResource(R.string.input_invalid_number)) }
                } else null,
            )
            if (type.units.isNotEmpty()) {
                Box {
                    OutlinedButton(onClick = { unitMenu = true }) {
                        Text(unit.ifEmpty { stringResource(R.string.input_select_unit) })
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = unitMenu, onDismissRequest = { unitMenu = false }) {
                        type.units.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u) },
                                onClick = { unit = u; unitMenu = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnumDialog(
    title: String,
    options: List<String>,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(title) { mutableStateOf(initialValue) }
    var menu by remember { mutableStateOf(false) }

    BaseDialog(title, onDismiss, { onConfirm(selected) }) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected.ifEmpty { stringResource(R.string.input_select_option) })
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("—") }, onClick = { selected = ""; menu = false })
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = { selected = opt; menu = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun BooleanDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var checked by remember(title) { mutableStateOf(initialValue.toBooleanStrictOrNull() ?: false) }
    BaseDialog(title, onDismiss, { onConfirm(checked.toString()) }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (checked) "true" else "false")
            Switch(checked = checked, onCheckedChange = { checked = it })
        }
    }
}

@Composable
private fun BaseDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        text = { Column { content() } },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/** 将 "16 GB" 拆为 ("16", "GB")；无单位时返回 (纯数字提取, null)。 */
private fun splitNumberUnit(value: String, units: List<String>): Pair<String, String?> {
    if (value.isBlank()) return "" to null
    val matchedUnit = units.firstOrNull { value.trim().endsWith(it, ignoreCase = true) }
    val number = value.replace(Regex("[^0-9.]"), "")
    return number to matchedUnit
}
