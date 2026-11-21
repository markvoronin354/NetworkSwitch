package com.markvoronin.networkswitch.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markvoronin.networkswitch.R
import com.markvoronin.networkswitch.domain.model.NetworkMode
import com.markvoronin.networkswitch.presentation.theme.NetworkSwitchTheme
import com.markvoronin.networkswitch.presentation.ui.composable.NetworkModeSelector
import com.markvoronin.networkswitch.service.AutomationReceiver

/**
 * Settings screen for the Tasker/Locale plugin contract: automation apps (MacroDroid, Tasker) open
 * it from their plugin list so the user can pick which mode to set, then store the result and send
 * it back to [AutomationReceiver] when the action runs.
 */
class PluginEditActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set when the user is editing an action they already configured
        val savedModeName = intent
            .getBundleExtra(AutomationReceiver.EXTRA_BUNDLE)
            ?.getString(AutomationReceiver.EXTRA_MODE)
        val initialMode = NetworkMode.entries.find { it.name == savedModeName } ?: NetworkMode.NR_LTE

        setContent {
            NetworkSwitchTheme {
                PluginEditScreen(
                    initialMode = initialMode,
                    onSave = ::saveAndFinish
                )
            }
        }
    }

    private fun saveAndFinish(mode: NetworkMode) {
        val result = Intent().apply {
            putExtra(
                AutomationReceiver.EXTRA_BUNDLE,
                Bundle().apply { putString(AutomationReceiver.EXTRA_MODE, mode.name) }
            )
            putExtra(EXTRA_BLURB, getString(R.string.plugin_blurb, mode.displayName))
        }
        setResult(RESULT_OK, result)
        finish()
    }

    private companion object {
        const val EXTRA_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PluginEditScreen(
    initialMode: NetworkMode,
    onSave: (NetworkMode) -> Unit
) {
    var mode by remember { mutableStateOf(initialMode) }
    val buttonHeight = ButtonDefaults.MediumContainerHeight

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.plugin_set_mode),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NetworkModeSelector(
                label = stringResource(R.string.network_mode),
                selectedMode = mode,
                onModeSelected = { mode = it },
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1)
            )

            Button(
                onClick = { onSave(mode) },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = buttonHeight),
                contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight)
            ) {
                Text(
                    text = stringResource(R.string.plugin_save),
                    style = ButtonDefaults.textStyleFor(buttonHeight)
                )
            }
        }
    }
}
