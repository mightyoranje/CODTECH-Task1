package com.example.fitnesstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitnesstracker.ui.theme.FitnessTrackerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FitnessViewModel : ViewModel() {
    private val _entries = MutableStateFlow<List<FitnessEntry>>(emptyList())
    val groupedEntries: StateFlow<Map<String, List<FitnessEntry>>> = _entries.map { entries ->
        entries.groupBy { it.date.split(" ")[0] } // Group by date, excluding time
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun addEntry(type: String, value: Int) {
        viewModelScope.launch {
            val newEntry = FitnessEntry(
                id = _entries.value.size + 1,
                type = type,
                value = value,
                date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
            _entries.value += newEntry
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitnessTrackerTheme {
                Main()
            }
        }
    }
}

@Composable
fun Main() {
    val navController = rememberNavController()
    val showDialog = remember { mutableStateOf(false) }
    val currentExercise = remember { mutableStateOf("") }
    val viewModel = remember { FitnessViewModel() }

    Scaffold(
        bottomBar = {
            Column {
                CalorieIntakeButton(
                    onClick = {
                        currentExercise.value = "Calorie Intake"
                        showDialog.value = true
                    }
                )
                BottomNavBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(viewModel) }
            composable("history") { HistoryScreen(viewModel) }
            composable("goals") { GoalsScreen() }
        }
    }

    if (showDialog.value) {
        ExerciseInputDialog(
            exercise = currentExercise.value,
            onDismiss = { showDialog.value = false },
            onConfirm = { exercise, value ->
                viewModel.addEntry(exercise, value)
                showDialog.value = false
            }
        )
    }
}
@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf("home", "history", "goals")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.Done, Icons.Filled.Menu)

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) },
                selected = currentRoute == item,
                onClick = {
                    navController.navigate(item) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun HomeScreen(viewModel: FitnessViewModel, modifier: Modifier = Modifier) {
    val showDialog = remember { mutableStateOf(false) }
    val currentExercise = remember { mutableStateOf("") }
    val currentDate = remember {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Fitness Tracker for Home",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                currentDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ExerciseGrid(
                onExerciseClick = { exercise ->
                    currentExercise.value = exercise
                    showDialog.value = true
                }
            )
        }
    }

    if (showDialog.value) {
        ExerciseInputDialog(
            exercise = currentExercise.value,
            onDismiss = { showDialog.value = false },
            onConfirm = { exercise, value ->
                viewModel.addEntry(exercise, value)
                showDialog.value = false
            }
        )
    }
}
@Composable
fun HistoryScreen(viewModel: FitnessViewModel) {
    val groupedEntries by viewModel.groupedEntries.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Exercise History",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn {
            groupedEntries.forEach { (date, entries) ->
                item {
                    Text(
                        text = formatDate(date),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(entries) { entry ->
                    HistoryItem(entry)
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryItem(entry: FitnessEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(entry.type, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${entry.value} ${if (entry.type == "Calorie Intake") "calories" else "reps"}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun GoalsScreen() {
    // Placeholder for Goals screen
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Goals Screen")
    }
}


@Composable
fun ExerciseGrid(onExerciseClick: (String) -> Unit) {
    val exercises = listOf("Push-ups", "Squats", "Pull-ups", "Curls", "Lunges", "Plank", "Sit Ups", "Crunches", "Burpees", "Chin Ups")

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(exercises) { exercise ->
            ExerciseCard(
                exercise = exercise,
                onClick = { onExerciseClick(exercise) }
            )
        }
    }
}

@Composable
fun ExerciseCard(exercise: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onClick),
        border = BorderStroke(2.dp, Color.Black),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val imageRes = when (exercise) {
                "Push-ups" -> R.drawable.pushup
                "Squats" -> R.drawable.sqaut
                "Pull-ups" -> R.drawable.pullup
                "Curls" -> R.drawable.curls
                "Lunges" -> R.drawable.lunges
                "Plank" -> R.drawable.plank
                "Sit Ups" -> R.drawable.situps
                "Crunches" -> R.drawable.crunches
                "Chin Ups" -> R.drawable.pullup
                "Burpees" -> R.drawable.burpees
                else -> R.drawable.ic_launcher_background // Default image
            }
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = exercise,
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 8.dp)
            )
            Text(
                text = exercise,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun CalorieIntakeButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Calorie Intake",
                style = MaterialTheme.typography.titleMedium
            )
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Calorie Intake"
            )
        }
    }
}
@Composable
fun ExerciseInputDialog(
    exercise: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var inputValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Enter ${if (exercise == "Calorie Intake") "calorie intake" else "$exercise count"}") },
        text = {
            OutlinedTextField(
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.Black,
                    unfocusedBorderColor = Color.Black,
                    unfocusedLabelColor = Color.Black,
                    focusedTextColor = Color.Black,
                    focusedBorderColor = Color.Black,
                    focusedLabelColor = Color.Black,
                ),
                value = inputValue,
                onValueChange = { inputValue = it },
                label = { Text(if (exercise == "Calorie Intake") "Calories" else "Number of $exercise") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    inputValue.toIntOrNull()?.let { value ->
                        onConfirm(exercise, value)
                    }
                },
                border = BorderStroke(2.dp, Color.Black),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Confirm", color = Color.Black)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                border = BorderStroke(2.dp, Color.Black),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Cancel", color = Color.Black)
            }
        }
    )
}

fun formatDate(dateString: String): String {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    val date = inputFormat.parse(dateString)
    return outputFormat.format(date)
}

data class FitnessEntry(
    val id: Int,
    val type: String,
    val value: Int,
    val date: String
)

