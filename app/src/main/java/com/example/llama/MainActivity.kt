package com.example.llama

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.arm.aichat.gguf.GgufMetadata
import com.arm.aichat.gguf.GgufMetadataReader
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageButton
    private lateinit var btnNewChat: MaterialButton
    private lateinit var historyRv: RecyclerView

    private lateinit var ggufTv: TextView
    private lateinit var subtitleTv: TextView
    private lateinit var messagesRv: RecyclerView
    private lateinit var userInputEt: EditText
    private lateinit var userActionFab: FloatingActionButton
    private lateinit var btnSwitchModel: ImageButton
    private lateinit var btnInfo: ImageButton
    private lateinit var btnClear: ImageButton

    private lateinit var engine: InferenceEngine
    private var generationJob: Job? = null

    private var isModelReady = false
    private var isGenerating = false
    private var activeModelName = "Bonsai-8B-Q1_0"
    private val messages = mutableListOf<Message>()
    private val messageAdapter = MessageAdapter(messages)

    private lateinit var chatSessionManager: ChatSessionManager
    private lateinit var chatSessionAdapter: ChatSessionAdapter
    private val savedSessions = mutableListOf<ChatSession>()
    private var currentSession = ChatSession()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        onBackPressedDispatcher.addCallback {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                Log.w(TAG, "Ignore back press for simplicity")
            }
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        btnMenu = findViewById(R.id.btn_menu)
        btnNewChat = findViewById(R.id.btn_new_chat)
        historyRv = findViewById(R.id.history_rv)

        ggufTv = findViewById(R.id.gguf)
        subtitleTv = findViewById(R.id.subtitle_tv)
        messagesRv = findViewById(R.id.messages)
        messagesRv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        messagesRv.adapter = messageAdapter
        userInputEt = findViewById(R.id.user_input)
        userActionFab = findViewById(R.id.fab)
        btnSwitchModel = findViewById(R.id.btn_switch_model)
        btnInfo = findViewById(R.id.btn_info)
        btnClear = findViewById(R.id.btn_clear)

        chatSessionManager = ChatSessionManager(applicationContext)
        setupHistorySidebar()

        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        btnNewChat.setOnClickListener { startNewChatSession() }

        lifecycleScope.launch(Dispatchers.Default) {
            engine = AiChat.getInferenceEngine(applicationContext)
            checkExistingInternalModel()
        }

        btnSwitchModel.setOnClickListener { showModelPicker() }
        subtitleTv.setOnClickListener { showModelPicker() }
        btnInfo.setOnClickListener { showModelInfoDialog() }
        btnClear.setOnClickListener { clearChat() }

        userActionFab.setOnClickListener {
            if (isGenerating) {
                stopGeneration()
            } else if (isModelReady) {
                handleUserInput()
            } else {
                showModelPicker()
            }
        }
    }

    private fun setupHistorySidebar() {
        historyRv.layoutManager = LinearLayoutManager(this)
        savedSessions.clear()
        savedSessions.addAll(chatSessionManager.loadAllSessions())

        chatSessionAdapter = ChatSessionAdapter(
            sessions = savedSessions,
            onSessionClick = { session -> loadChatSession(session) },
            onDeleteClick = { session, position ->
                chatSessionManager.deleteSession(session.id)
                chatSessionAdapter.removeAt(position)
                if (currentSession.id == session.id) {
                    startNewChatSession()
                }
                Toast.makeText(this, "Deleted chat session", Toast.LENGTH_SHORT).show()
            }
        )
        historyRv.adapter = chatSessionAdapter
    }

    private fun refreshHistorySidebar() {
        savedSessions.clear()
        savedSessions.addAll(chatSessionManager.loadAllSessions())
        chatSessionAdapter.notifyDataSetChanged()
    }

    private fun startNewChatSession() {
        if (currentSession.messages.isNotEmpty()) {
            chatSessionManager.saveSession(currentSession)
        }
        currentSession = ChatSession()
        messages.clear()
        messageAdapter.notifyDataSetChanged()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (isModelReady) { engine.cleanUp() }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up engine for new chat", e)
            }
        }

        refreshHistorySidebar()
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        Toast.makeText(this, "Started new chat", Toast.LENGTH_SHORT).show()
    }

    private fun loadChatSession(session: ChatSession) {
        if (currentSession.messages.isNotEmpty() && currentSession.id != session.id) {
            chatSessionManager.saveSession(currentSession)
        }
        currentSession = session
        messages.clear()
        messages.addAll(session.messages)
        messageAdapter.notifyDataSetChanged()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (isModelReady) { engine.cleanUp() }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning engine on chat load", e)
            }
        }

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        Toast.makeText(this, "Loaded chat: ${session.title}", Toast.LENGTH_SHORT).show()
    }

    private suspend fun checkExistingInternalModel() {
        val appModelsDir = ensureModelsDirectory()
        val internalModels = appModelsDir.listFiles()?.filter { it.name.endsWith(".gguf") } ?: emptyList()

        if (internalModels.isNotEmpty()) {
            val defaultModel = internalModels.firstOrNull { it.name.contains("Bonsai", ignoreCase = true) }
                ?: internalModels.first()
            val cleanName = cleanModelDisplayName(defaultModel.name)
            loadModelFileDirectly(cleanName, defaultModel)
        } else {
            withContext(Dispatchers.Main) {
                ggufTv.text = "⚡ Please select a GGUF model file to start"
                userInputEt.hint = "Pick a GGUF model file..."
                userActionFab.setImageResource(R.drawable.outline_folder_open_24)
            }
        }
    }

    private fun showModelPicker() {
        val appModelsDir = ensureModelsDirectory()
        val internalModels = appModelsDir.listFiles()?.filter { it.name.endsWith(".gguf") } ?: emptyList()

        if (internalModels.isNotEmpty()) {
            val optionsList = mutableListOf<String>()
            internalModels.forEach { file ->
                val cleanName = cleanModelDisplayName(file.name)
                val marker = if (cleanName == activeModelName && isModelReady) " (Active)" else ""
                optionsList.add("⚡ $cleanName$marker")
            }
            optionsList.add("📂 Select new GGUF file from storage...")

            AlertDialog.Builder(this)
                .setTitle("Select AI Model")
                .setItems(optionsList.toTypedArray()) { _, which ->
                    if (which < internalModels.size) {
                        val selectedFile = internalModels[which]
                        val cleanName = cleanModelDisplayName(selectedFile.name)
                        lifecycleScope.launch(Dispatchers.IO) {
                            if (isModelReady) {
                                try { engine.cleanUp() } catch (e: Exception) { Log.e(TAG, "Error cleaning up", e) }
                            }
                            loadModelFileDirectly(cleanName, selectedFile)
                        }
                    } else {
                        getContent.launch(arrayOf("*/*"))
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            getContent.launch(arrayOf("*/*"))
        }
    }

    private val getContent = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        Log.i(TAG, "Selected file uri:\n $uri")
        uri?.let { handleSelectedModel(it) }
    }

    private fun handleSelectedModel(uri: Uri) {
        userActionFab.isEnabled = false
        userInputEt.hint = "Parsing GGUF..."
        ggufTv.text = "Parsing metadata from selected file \n$uri"

        lifecycleScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Parsing GGUF metadata...")
            try {
                val uriFileName = getFileNameFromUri(uri)
                var metadataName: String? = null
                contentResolver.openInputStream(uri)?.use { input ->
                    GgufMetadataReader.create().readStructuredMetadata(input)?.let { metadata ->
                        val metaFile = metadata.filename()
                        if (metadata.basic.name != null) {
                            metadataName = metaFile + FILE_EXTENSION_GGUF
                        }
                    }
                }

                val rawFileName = uriFileName ?: metadataName ?: ("model-" + System.currentTimeMillis() + FILE_EXTENSION_GGUF)
                val modelName = if (rawFileName.endsWith(FILE_EXTENSION_GGUF, ignoreCase = true)) rawFileName else "$rawFileName$FILE_EXTENSION_GGUF"
                val cleanName = cleanModelDisplayName(modelName)

                withContext(Dispatchers.Main) {
                    ggufTv.text = "Copying model to app storage..."
                    userInputEt.hint = "Copying model to storage..."
                }

                contentResolver.openInputStream(uri)?.use { input ->
                    ensureModelFile(modelName, input)
                }?.let { modelFile ->
                    if (isModelReady) {
                        try { engine.cleanUp() } catch (e: Exception) { Log.e(TAG, "Error cleaning up", e) }
                    }
                    loadModelFileDirectly(cleanName, modelFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read GGUF file from URI: $uri", e)
                withContext(Dispatchers.Main) {
                    isModelReady = false
                    ggufTv.text = "❌ Error reading file: ${e.localizedMessage}"
                    userInputEt.hint = "Select another model..."
                    userActionFab.setImageResource(R.drawable.outline_folder_open_24)
                    userActionFab.isEnabled = true
                }
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            result = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query Uri display name", e)
            }
        }
        if (result == null) {
            val path = uri.path
            val cut = path?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = path?.substring(cut + 1)
            }
        }
        return result
    }

    private suspend fun loadModelFileDirectly(modelDisplayName: String, modelFile: File) {
        withContext(Dispatchers.Main) {
            userActionFab.isEnabled = false
            userInputEt.hint = "Loading $modelDisplayName..."
            ggufTv.text = "⏳ Loading model: $modelDisplayName..."
        }

        try {
            engine.loadModel(modelFile.path)
            activeModelName = modelDisplayName

            withContext(Dispatchers.Main) {
                isModelReady = true
                subtitleTv.text = "$activeModelName • ARM Neon KleidiAI"
                ggufTv.text = "⚡ $activeModelName Ready (100% Offline)"
                userInputEt.hint = "Ask $activeModelName anything..."
                userInputEt.isEnabled = true
                userActionFab.setImageResource(R.drawable.outline_send_24)
                userActionFab.isEnabled = true
                Toast.makeText(this@MainActivity, "Loaded: $activeModelName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model $modelDisplayName", e)
            withContext(Dispatchers.Main) {
                isModelReady = false
                ggufTv.text = "❌ Failed to load model: ${e.localizedMessage}"
                userInputEt.hint = "Pick another model file..."
                userActionFab.setImageResource(R.drawable.outline_folder_open_24)
                userActionFab.isEnabled = true
            }
        }
    }

    private suspend fun ensureModelFile(modelName: String, input: InputStream) =
        withContext(Dispatchers.IO) {
            File(ensureModelsDirectory(), modelName).also { file ->
                if (!file.exists()) {
                    Log.i(TAG, "Start copying file to $modelName")
                    FileOutputStream(file).use { input.copyTo(it) }
                    Log.i(TAG, "Finished copying file to $modelName")
                }
            }
        }

    private fun handleUserInput() {
        val userMsg = userInputEt.text.toString().trim()

        if (userMsg.isEmpty()) {
            Toast.makeText(this, "Input message is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        userInputEt.text = null
        userInputEt.isEnabled = false
        userActionFab.isEnabled = false
        isGenerating = true

        userActionFab.setImageResource(R.drawable.ic_stop)
        ggufTv.text = "⏳ Generating response..."

        val userMessageObj = Message(UUID.randomUUID().toString(), userMsg, true)
        messages.add(userMessageObj)
        currentSession.messages.add(userMessageObj)

        val assistantIndex = messages.size
        val assistantMessageObj = Message(UUID.randomUUID().toString(), "", false)
        messages.add(assistantMessageObj)

        messageAdapter.notifyItemRangeInserted(assistantIndex - 1, 2)
        messagesRv.scrollToPosition(assistantIndex)

        val startTime = System.currentTimeMillis()
        var firstTokenTime: Long? = null
        var tokenCount = 0

        val response = StringBuilder()

        generationJob = lifecycleScope.launch(Dispatchers.Default) {
            try {
                engine.sendUserPrompt(userMsg, 96)
                    .collect { token ->
                        tokenCount++

                        if (firstTokenTime == null) {
                            firstTokenTime = System.currentTimeMillis()
                        }

                        response.append(token)
                        val cleaned = response.toString()

                        withContext(Dispatchers.Main) {
                            if (assistantIndex < messages.size) {
                                messages[assistantIndex] = messages[assistantIndex].copy(
                                    content = cleaned
                                )
                                messageAdapter.notifyItemChanged(assistantIndex)
                                messagesRv.scrollToPosition(assistantIndex)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error during generation stream", e)
            } finally {
                withContext(Dispatchers.Main) {
                    isGenerating = false
                    userInputEt.isEnabled = true
                    userActionFab.isEnabled = true
                    userActionFab.setImageResource(R.drawable.outline_send_24)

                    val finalResponseText = response.toString().trim()
                    if (finalResponseText.isEmpty()) {
                        if (assistantIndex < messages.size) {
                            messages.removeAt(assistantIndex)
                            messageAdapter.notifyItemRemoved(assistantIndex)
                        }
                    } else {
                        if (assistantIndex < messages.size) {
                            val finalMsg = messages[assistantIndex]
                            currentSession.messages.add(finalMsg)
                            chatSessionManager.saveSession(currentSession)
                            refreshHistorySidebar()
                        }
                    }

                    val first = firstTokenTime ?: startTime
                    val generationSeconds = (System.currentTimeMillis() - first) / 1000.0
                    val tokPerSec = if (generationSeconds > 0 && tokenCount > 1) {
                        (tokenCount - 1) / generationSeconds
                    } else {
                        0.0
                    }

                    val ttft = firstTokenTime?.let { it - startTime } ?: 0

                    ggufTv.text = "⚡ %.1f tok/s | TTFT: %dms | Ctx: 4096 | Threads: 2"
                        .format(tokPerSec, ttft)
                }
            }
        }
    }

    private fun stopGeneration() {
        generationJob?.cancel()
        isGenerating = false
        userInputEt.isEnabled = true
        userActionFab.setImageResource(R.drawable.outline_send_24)
        ggufTv.text = "⏹ Generation stopped"
        Toast.makeText(this, "Generation stopped", Toast.LENGTH_SHORT).show()
    }

    private fun clearChat() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (isModelReady) {
                    engine.cleanUp()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up engine on clear chat", e)
            }
        }
        messages.clear()
        currentSession = ChatSession()
        messageAdapter.notifyDataSetChanged()
        Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show()
    }

    private fun showModelInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("Active Model Info")
            .setMessage(
                "• Active Model: $activeModelName\n" +
                "• Runtime: llama.cpp native\n" +
                "• Acceleration: ARM Neon + KleidiAI\n" +
                "• Device: Moto G54 5G\n" +
                "• Mode: 100% Offline\n" +
                "• Context Window: 4096 tokens\n" +
                "• Thread Count: 1 CPU Thread (Perfect Quality Mode)"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun cleanModelDisplayName(filename: String): String {
        val clean = filename
            .replace(".gguf", "", ignoreCase = true)
            .replace("-Q4_K_M", "", ignoreCase = true)
            .replace("-Q4_0", "", ignoreCase = true)
            .replace("-Q8_0", "", ignoreCase = true)

        if (clean.startsWith("qwen3-", ignoreCase = true) || clean.startsWith("model-", ignoreCase = true)) {
            return "Bonsai-8B-Q1_0"
        }
        return clean
    }

    private fun ensureModelsDirectory() =
        File(filesDir, DIRECTORY_MODELS).also { dir ->
            if (dir.exists() && !dir.isDirectory) { dir.delete() }
            if (!dir.exists()) { dir.mkdir() }
            val files = dir.listFiles()?.filter { it.name.endsWith(".gguf") } ?: emptyList()
            val namedFiles = files.filter { !it.name.startsWith("qwen3-") && !it.name.startsWith("model-") }
            if (namedFiles.isNotEmpty()) {
                files.filter { it.name.startsWith("qwen3-") || it.name.startsWith("model-") }.forEach { orphan ->
                    try { orphan.delete() } catch (e: Exception) { Log.e(TAG, "Failed to delete orphan file", e) }
                }
            }
        }

    override fun onStop() {
        stopGeneration()
        super.onStop()
    }

    override fun onDestroy() {
        engine.destroy()
        super.onDestroy()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val DIRECTORY_MODELS = "models"
        private const val FILE_EXTENSION_GGUF = ".gguf"
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun GgufMetadata.filename() = when {
    basic.name != null -> {
        basic.name?.let { name ->
            basic.sizeLabel?.let { size ->
                "$name-$size"
            } ?: name
        }
    }
    architecture?.architecture != null -> {
        architecture?.architecture?.let { arch ->
            basic.uuid?.let { uuid ->
                "$arch-$uuid"
            } ?: "$arch-${System.currentTimeMillis()}"
        }
    }
    else -> {
        "model-${System.currentTimeMillis().toHexString()}"
    }
}
