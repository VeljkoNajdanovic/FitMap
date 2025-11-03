package veljko.najdanovic19273.fitmap.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import veljko.najdanovic19273.fitmap.data.model.FilterState
import veljko.najdanovic19273.fitmap.data.model.ObjectType
import veljko.najdanovic19273.fitmap.data.model.RadiusOption
import veljko.najdanovic19273.fitmap.util.getObjectTypeName

/**
 * Filter & Search Bottom Sheet
 * SEARCH SA DUGMETOM: Korisnik ukuca tekst, pa klikne "Pretraži"
 * NE PRIMENJUJE SE AUTOMATSKI nakon svakog slova
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilter: FilterState,
    onFilterChanged: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(currentFilter.searchQuery) }
    var selectedType by remember { mutableStateOf(currentFilter.selectedType) }
    var selectedRadius by remember { mutableStateOf(RadiusOption.fromMeters(currentFilter.radiusInMeters)) }
    var minRating by remember { mutableFloatStateOf(currentFilter.minRating) }

    // IZMENJENO: Uklonjen LaunchedEffect - NE primenjuj filter automatski
    // Filter se primenjuje SAMO kada korisnik klikne dugme "Pretraži"

    // Funkcija za primenu filtera
    val applyFilters = {
        val hasSearch = searchQuery.trim().isNotBlank()
        val hasOtherFilters = selectedRadius != RadiusOption.ALL || minRating > 0f

        if (hasSearch || hasOtherFilters) {
            val newFilter = FilterState(
                searchQuery = searchQuery.trim(),
                selectedType = selectedType,
                radiusInMeters = if (selectedRadius == RadiusOption.ALL) null else selectedRadius.meters,
                minRating = minRating
            )
            onFilterChanged(newFilter)
        } else {
            onFilterChanged(FilterState())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetMaxWidth = 400.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pretraga",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                // Dugme za zatvaranje
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Zatvori")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ========================================
            // SEARCH POLJE NA VRHU - PRIORITET!
            // ========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "🔍 Unesite naziv, pa kliknite Pretraži",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search field sa Enter key support
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Pretraži po nazivu") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Obriši")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Npr: Kangoo, Iron Gym, tegovi...") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = {
                        applyFilters()
                    }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // NOVO: Dugme za primenu pretrage
            Button(
                onClick = { applyFilters() },
                modifier = Modifier.fillMaxWidth(),
                enabled = searchQuery.trim().isNotBlank() || selectedRadius != RadiusOption.ALL || minRating > 0f
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pretraži")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ========================================
            // DODATNI FILTERI
            // ========================================
            Text(
                text = "Dodatni filteri",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tip objekta
            Text(
                text = "Tip objekta",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // IZMENJENO: Uklanjeno upozorenje, sada se primenjuje samo na klik
            // Opcija "Svi tipovi"
            FilterChipRow(
                label = "Svi tipovi",
                selected = selectedType == null,
                onClick = { selectedType = null }
            )

            // Sve dostupne tipove objekta
            ObjectType.entries.forEach { type ->
                FilterChipRow(
                    label = getObjectTypeName(type),
                    selected = selectedType == type,
                    onClick = { selectedType = type }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Radijus pretrage
            Text(
                text = "Radijus",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            RadiusOption.entries.forEach { radius ->
                FilterChipRow(
                    label = radius.displayName,
                    selected = selectedRadius == radius,
                    onClick = { selectedRadius = radius }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Minimalna ocena
            Text(
                text = "Minimalna ocena",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = minRating,
                    onValueChange = { minRating = it },
                    valueRange = 0f..5f,
                    steps = 4,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (minRating > 0) "%.1f".format(minRating) else "Sve",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.width(40.dp)
                    )
                    if (minRating > 0) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dugme za reset
            if (searchQuery.isNotBlank() || selectedType != null || selectedRadius != RadiusOption.ALL || minRating > 0f) {
                OutlinedButton(
                    onClick = {
                        searchQuery = ""
                        selectedType = null
                        selectedRadius = RadiusOption.ALL
                        minRating = 0f
                        // Resetuj i filtere
                        onFilterChanged(FilterState())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resetuj sve filtere")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
