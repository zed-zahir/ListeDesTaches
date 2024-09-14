package com.zahirmeddour.listedestaches

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoList()
        }

        // Planifier la réinitialisation quotidienne des tâches
        scheduleDailyTaskReset()
    }

    // Fonction pour planifier la réinitialisation quotidienne des tâches à 6h du matin
    private fun scheduleDailyTaskReset() {
        val workManager = WorkManager.getInstance(this)

        // Calculer le temps restant jusqu'à 6h du matin
        val calendar = Calendar.getInstance().apply {
            if (get(Calendar.HOUR_OF_DAY) >= 6) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        val initialDelay = calendar.timeInMillis - System.currentTimeMillis()

        // Créer une requête de travail périodique
        val workRequest = PeriodicWorkRequestBuilder<ResetTasksWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        // Planifier le travail
        workManager.enqueue(workRequest)
    }
}

class ResetTasksWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Réinitialiser les tâches dans SharedPreferences
        resetTasks()
        return Result.success()
    }

    private fun resetTasks() {
        // Utiliser SharedPreferences pour sauvegarder une nouvelle liste de tâches
        val sharedPrefs =
            applicationContext.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putStringSet(
            "tasks",
            setOf("Brosser les dents", "Faire de l'exercice", "Lire un livre")
        )
        editor.apply()
    }
}

@Composable
fun TodoList() {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
    var tasks by remember {
        mutableStateOf(
            sharedPrefs.getStringSet(
                "tasks",
                setOf("Brosser les dents", "Faire de l'exercice", "Lire un livre")
            )?.toMutableList() ?: mutableListOf()
        )
    }

    // Sauvegarder les modifications des tâches lorsque la liste change
    fun saveTasks(newTasks: List<String>) {
        val editor = sharedPrefs.edit()
        editor.putStringSet("tasks", newTasks.toSet())
        editor.apply()
    }

    // Afficher les tâches dans une liste et permettre de cliquer pour les supprimer
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn {
            items(tasks) { task ->
                ClickableText(
                    text = task,
                    onClick = {
                        tasks = tasks.toMutableList().also { it.remove(task) }
                        saveTasks(tasks) // Sauvegarder après suppression
                    }
                )
            }
        }
    }
}

@Composable
fun ClickableText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 18.sp,
        textDecoration = TextDecoration.None,
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxSize()
    )
}
