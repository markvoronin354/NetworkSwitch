package com.supernova.networkswitch.presentation.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.presentation.theme.NetworkSwitchTheme
import com.supernova.networkswitch.presentation.ui.composable.WidgetCustomizationCard
import com.supernova.networkswitch.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            NetworkSwitchTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.retryCompatibilityCheck()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val controlMethod by viewModel.controlMethod.collectAsState()
    val widgetCustomization by viewModel.widgetCustomization.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Control Method Selection
            ControlMethodCard(
                selectedMethod = controlMethod,
                onMethodSelected = { viewModel.updateControlMethod(it) },
                rootCompatibility = viewModel.rootCompatibility,
                shizukuCompatibility = viewModel.shizukuCompatibility,
                onRetryClick = { viewModel.retryCompatibilityCheck() }
            )

            // Widget Customization Card
            WidgetCustomizationCard(
                config = widgetCustomization,
                onConfigChanged = { viewModel.updateWidgetCustomization(it) }
            )
            
            // About Section
            AboutCard()
        }
    }
}

@Composable
private fun ControlMethodCard(
    selectedMethod: ControlMethod,
    onMethodSelected: (ControlMethod) -> Unit,
    rootCompatibility: CompatibilityState,
    shizukuCompatibility: CompatibilityState,
    onRetryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Control Method",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                IconButton(onClick = onRetryClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh compatibility",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Select how Network Switch executes changes on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Option 1: Root Method
            ControlMethodOption(
                title = "Root Method",
                subtitle = "Requires root access (Magisk / KernelSU / APatch)",
                isSelected = selectedMethod == ControlMethod.ROOT,
                compatibilityState = rootCompatibility,
                onClick = { onMethodSelected(ControlMethod.ROOT) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Option 2: Shizuku Method
            ControlMethodOption(
                title = "Shizuku Method",
                subtitle = "Works without root via Shizuku ADB service",
                isSelected = selectedMethod == ControlMethod.SHIZUKU,
                compatibilityState = shizukuCompatibility,
                onClick = { onMethodSelected(ControlMethod.SHIZUKU) }
            )
        }
    }
}

@Composable
private fun ControlMethodOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    compatibilityState: CompatibilityState,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            // Status indicator
            when (compatibilityState) {
                is CompatibilityState.Pending -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is CompatibilityState.Compatible -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Compatible",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                is CompatibilityState.PermissionDenied, is CompatibilityState.Incompatible -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Incompatible or denied",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "About & Open Source",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinkItem(
                title = "Source Code on GitHub",
                subtitle = "https://github.com/markvoronin354/NetworkSwitch",
                link = "https://github.com/markvoronin354/NetworkSwitch"
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Text(
                text = "Open Source Licenses",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            LinkItem(
                title = "Shizuku Service API",
                subtitle = "Apache License 2.0",
                link = "https://github.com/RikkaApps/Shizuku"
            )
            
            LinkItem(
                title = "libsu Framework",
                subtitle = "Apache License 2.0",
                link = "https://github.com/topjohnwu/libsu"
            )
            
            LinkItem(
                title = "Android Jetpack & Compose",
                subtitle = "Apache License 2.0",
                link = "https://android.googlesource.com/platform/frameworks/support"
            )
        }
    }
}

@Composable
private fun LinkItem(
    title: String,
    subtitle: String,
    link: String
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open Link",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
