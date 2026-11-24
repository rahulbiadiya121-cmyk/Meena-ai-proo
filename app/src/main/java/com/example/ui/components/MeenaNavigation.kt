package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.MeenaCharacter
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

object Routes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val VOICE = "voice"
    const val HISTORY = "history"
    const val CHARACTERS = "characters"
    const val CONNECTIONS = "connections"
    const val QUICK_ACTIONS = "quick_actions"
    const val IMAGE_GEN = "image_gen"
    const val WRITE = "write"
    const val PHONE_CONTROL = "phone_control"
    const val MEMORY = "memory"
    const val SETTINGS = "settings"
}

data class DrawerDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val AllDrawerDestinations = listOf(
    DrawerDestination(Routes.HOME, "Home", Icons.Outlined.Home),
    DrawerDestination(Routes.CHAT, "AI Chat", Icons.Outlined.ChatBubbleOutline),
    DrawerDestination(Routes.VOICE, "Voice Assistant", Icons.Outlined.RecordVoiceOver),
    DrawerDestination(Routes.HISTORY, "Conversation History", Icons.Outlined.History),
    DrawerDestination(Routes.CHARACTERS, "AI Characters", Icons.Outlined.Face),
    DrawerDestination(Routes.IMAGE_GEN, "Create AI Image", Icons.Outlined.AutoAwesome),
    DrawerDestination(Routes.WRITE, "Write Studio", Icons.Outlined.EditNote),
    DrawerDestination(Routes.QUICK_ACTIONS, "Quick Actions", Icons.Outlined.Bolt),
    DrawerDestination(Routes.PHONE_CONTROL, "Phone Control", Icons.Outlined.PhoneAndroid),
    DrawerDestination(Routes.MEMORY, "AI Memory", Icons.Outlined.Psychology),
    DrawerDestination(Routes.CONNECTIONS, "Connections & Keys", Icons.Outlined.Hub),
    DrawerDestination(Routes.SETTINGS, "Settings Control Center", Icons.Outlined.Settings)
)

@Composable
fun MeenaDrawerContent(
    currentRoute: String?,
    character: MeenaCharacter,
    assistantName: String,
    userName: String,
    onNavigate: (String) -> Unit
) {
    val glow = LocalMeenaGlow.current
    ModalDrawerSheet(
        drawerContainerColor = MidnightVoid,
        drawerContentColor = TextPrimary,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Image(
                    painter = painterResource(id = character.imageRes),
                    contentDescription = character.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, glow.primaryGlow, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$assistantName AI PRO",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Companion for $userName",
                        style = MaterialTheme.typography.bodySmall,
                        color = glow.secondaryGlow
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.White.copy(alpha = 0.08f)
            )
            AllDrawerDestinations.forEach { dest ->
                val selected = currentRoute == dest.route
                NavigationDrawerItem(
                    selected = selected,
                    onClick = { onNavigate(dest.route) },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = {
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = glow.primaryGlow.copy(alpha = 0.22f),
                        selectedIconColor = glow.secondaryGlow,
                        selectedTextColor = TextPrimary,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun MeenaBottomBar(
    currentRoute: String?,
    character: MeenaCharacter,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = GlassSurface.copy(alpha = 0.94f),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .align(Alignment.BottomCenter)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            glow.primaryGlow.copy(alpha = 0.4f),
                            glow.secondaryGlow.copy(alpha = 0.3f),
                            glow.primaryGlow.copy(alpha = 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavTab(
                    label = "Home",
                    selected = currentRoute == Routes.HOME,
                    selectedIcon = Icons.Rounded.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    testTag = "nav_home_button",
                    onClick = { onNavigate(Routes.HOME) },
                    modifier = Modifier.weight(1f)
                )
                BottomNavTab(
                    label = "Chat",
                    selected = currentRoute == Routes.CHAT,
                    selectedIcon = Icons.Rounded.ChatBubble,
                    unselectedIcon = Icons.Outlined.ChatBubbleOutline,
                    testTag = "nav_chat_button",
                    onClick = { onNavigate(Routes.CHAT) },
                    modifier = Modifier.weight(1f)
                )

                // Space reserved for center floating MEENA character button
                Spacer(modifier = Modifier.width(68.dp))

                BottomNavTab(
                    label = "History",
                    selected = currentRoute == Routes.HISTORY,
                    selectedIcon = Icons.Rounded.History,
                    unselectedIcon = Icons.Outlined.History,
                    testTag = "nav_history_button",
                    onClick = { onNavigate(Routes.HISTORY) },
                    modifier = Modifier.weight(1f)
                )
                BottomNavTab(
                    label = "Settings",
                    selected = currentRoute == Routes.SETTINGS,
                    selectedIcon = Icons.Rounded.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    testTag = "nav_settings_button",
                    onClick = { onNavigate(Routes.SETTINGS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Center Floating MEENA Character Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
                .size(66.dp)
                .shadow(16.dp, CircleShape, ambientColor = glow.primaryGlow, spotColor = glow.primaryGlow)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(glow.primaryGlow, glow.secondaryGlow))
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(CosmicBlack)
                .clickable { onNavigate(Routes.VOICE) }
                .testTag("nav_center_meena_button")
        ) {
            Image(
                painter = painterResource(id = character.imageRes),
                contentDescription = "Open Meena Voice Assistant",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun BottomNavTab(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = if (selected) glow.secondaryGlow else TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) TextPrimary else TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
