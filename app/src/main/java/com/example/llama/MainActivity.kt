package com.example.llama

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.arm.aichat.gguf.GgufMetadata
import com.arm.aichat.gguf.GgufMetadataReader
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
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
    private lateinit var btnThemeToggle: ImageButton
    private lateinit var btnInfo: ImageButton
    private lateinit var btnClear: ImageButton

    private lateinit var btnAttach: ImageButton
    private lateinit var composerModelChip: TextView

    private lateinit var emptyStateView: View
    private lateinit var chip1: TextView
    private lateinit var chip2: TextView
    private lateinit var chip3: TextView
    private lateinit var telemetryPanel: View
    private lateinit var headerTitleContainer: View
    private lateinit var btnExpandTelemetry: ImageView

    private lateinit var engine: InferenceEngine
    private var generationJob: Job? = null

    private var isModelReady = false
    private var isGenerating = false
    private var activeModelName = "Bonsai-Q1_0"
    private val messages = mutableListOf<Message>()
    private val messageAdapter = MessageAdapter(
        messages = messages,
        onRegenerateClicked = { regenerateLastTurn() },
        onShareClicked = { text -> shareText(text) }
    )

    private lateinit var chatSessionManager: ChatSessionManager
    private lateinit var chatSessionAdapter: ChatSessionAdapter
    private val savedSessions = mutableListOf<ChatSession>()
    private var currentSession = ChatSession()

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

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
        btnThemeToggle = findViewById(R.id.btn_theme_toggle)
        btnInfo = findViewById(R.id.btn_info)
        btnClear = findViewById(R.id.btn_clear)

        btnAttach = findViewById(R.id.btn_attach)
        composerModelChip = findViewById(R.id.composer_model_chip)

        emptyStateView = findViewById(R.id.empty_state_view)
        chip1 = findViewById(R.id.chip_1)
        chip2 = findViewById(R.id.chip_2)
        chip3 = findViewById(R.id.chip_3)
        telemetryPanel = findViewById(R.id.telemetry_panel)
        headerTitleContainer = findViewById(R.id.header_title_container)
        btnExpandTelemetry = findViewById(R.id.btn_expand_telemetry)

        updateEmptyStateVisibility()

        chatSessionManager = ChatSessionManager(applicationContext)
        setupHistorySidebar()

        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        btnNewChat.setOnClickListener { startNewChatSession() }

        val toggleTelemetry = {
            if (telemetryPanel.visibility == View.VISIBLE) {
                telemetryPanel.visibility = View.GONE
                btnExpandTelemetry.animate().rotation(0f).setDuration(200).start()
            } else {
                telemetryPanel.visibility = View.VISIBLE
                btnExpandTelemetry.animate().rotation(180f).setDuration(200).start()
            }
        }
        headerTitleContainer.setOnClickListener { toggleTelemetry() }

        btnThemeToggle.setOnClickListener {
            val currentMode = prefs.getBoolean(KEY_DARK_MODE, true)
            val newMode = !currentMode
            prefs.edit().putBoolean(KEY_DARK_MODE, newMode).apply()
            if (newMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        composerModelChip.setOnClickListener { showModelPickerBottomSheet() }
        btnAttach.setOnClickListener { showAttachBottomSheet() }

        chip1.setOnClickListener {
            userInputEt.setText("Explain quantum computing in simple terms")
            userInputEt.requestFocus()
        }
        chip2.setOnClickListener {
            userInputEt.setText("Help me draft a clear project update email")
            userInputEt.requestFocus()
        }
        chip3.setOnClickListener {
            userInputEt.setText("Brainstorm 5 creative app features for on-device AI")
            userInputEt.requestFocus()
        }

        lifecycleScope.launch(Dispatchers.Default) {
            engine = AiChat.getInferenceEngine(applicationContext)
            checkExistingInternalModel()
        }

        btnSwitchModel.setOnClickListener { showModelPickerBottomSheet() }
        btnInfo.setOnClickListener { showModelInfoDialog() }
        btnClear.setOnClickListener { clearChat() }

        userActionFab.setOnClickListener {
            if (isGenerating) {
                stopGeneration()
            } else if (isModelReady) {
                handleUserInput()
            } else {
                showModelPickerBottomSheet()
            }
        }
    }

    private fun updateEmptyStateVisibility() {
        if (messages.isEmpty()) {
            emptyStateView.visibility = View.VISIBLE
            messagesRv.visibility = View.GONE
        } else {
            emptyStateView.visibility = View.GONE
            messagesRv.visibility = View.VISIBLE
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
            },
            onSessionLongClick = { session, position ->
                showSessionOptionsBottomSheet(session, position)
            },
            activeSessionId = currentSession.id
        )
        historyRv.adapter = chatSessionAdapter
    }

    private fun refreshHistorySidebar() {
        savedSessions.clear()
        savedSessions.addAll(chatSessionManager.loadAllSessions())
        chatSessionAdapter.activeSessionId = currentSession.id
        chatSessionAdapter.notifyDataSetChanged()
    }

    private fun startNewChatSession() {
        if (currentSession.messages.isNotEmpty()) {
            chatSessionManager.saveSession(currentSession)
        }
        currentSession = ChatSession()
        messages.clear()
        messageAdapter.notifyDataSetChanged()
        updateEmptyStateVisibility()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (isModelReady) { engine.setSystemPrompt("You are a helpful assistant") }
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting engine for new chat", e)
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
        updateEmptyStateVisibility()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (isModelReady) { engine.setSystemPrompt("You are a helpful assistant") }
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting engine on chat load", e)
            }
        }

        refreshHistorySidebar()
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        Toast.makeText(this, "Loaded chat: ${session.title}", Toast.LENGTH_SHORT).show()
    }

    private fun showSessionOptionsBottomSheet(session: ChatSession, position: Int) {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_session_options, null)
        dialog.setContentView(sheetView)

        sheetView.findViewById<TextView>(R.id.session_options_title).text = session.title

        sheetView.findViewById<View>(R.id.option_share).setOnClickListener {
            dialog.dismiss()
            shareChatHistory(session)
        }

        sheetView.findViewById<View>(R.id.option_rename).setOnClickListener {
            dialog.dismiss()
            showRenameDialog(session)
        }

        sheetView.findViewById<View>(R.id.option_delete).setOnClickListener {
            dialog.dismiss()
            chatSessionManager.deleteSession(session.id)
            chatSessionAdapter.removeAt(position)
            if (currentSession.id == session.id) {
                startNewChatSession()
            }
            Toast.makeText(this, "Deleted chat", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun shareChatHistory(session: ChatSession) {
        val sb = StringBuilder()
        sb.append("Edge AI Chat: ").append(session.title).append("\n\n")
        session.messages.forEach { msg ->
            val sender = if (msg.isUser) "User" else "Assistant"
            sb.append(sender).append(": ").append(msg.content).append("\n\n")
        }
        shareText(sb.toString())
    }

    private fun showRenameDialog(session: ChatSession) {
        val input = EditText(this)
        input.setText(session.title)
        AlertDialog.Builder(this)
            .setTitle("Rename Chat")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    session.title = newTitle
                    chatSessionManager.saveSession(session)
                    refreshHistorySidebar()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareText(text: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share chat")
        startActivity(shareIntent)
    }

    private fun showAttachBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_attach, null)
        dialog.setContentView(sheetView)

        sheetView.findViewById<View>(R.id.btn_import_gguf_attach).setOnClickListener {
            dialog.dismiss()
            getContent.launch(arrayOf("*/*"))
        }

        dialog.show()
    }

    private fun showModelPickerBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_model_picker, null)
        dialog.setContentView(sheetView)

        val appModelsDir = ensureModelsDirectory()
        val internalModels = appModelsDir.listFiles()?.filter { it.name.endsWith(".gguf") } ?: emptyList()

        val modelsRv = sheetView.findViewById<RecyclerView>(R.id.models_rv)
        modelsRv.layoutManager = LinearLayoutManager(this)

        val modelItems = internalModels.map { file ->
            val cleanName = cleanModelDisplayName(file.name)
            val sizeMb = file.length() / (1024.0 * 1024.0)
            val sizeStr = if (sizeMb >= 1024) String.format("%.2f GB", sizeMb / 1024.0) else String.format("%.1f MB", sizeMb)
            val is4B = file.name.contains("4B", ignoreCase = true)
            val desc = if (is4B) "Faster, ultra-low RAM footprint" else "Higher capability & detail"
            ModelItem(
                file = file,
                cleanName = cleanName,
                sizeStr = sizeStr,
                quantBadge = "Q1_0",
                description = desc,
                isActive = cleanName == activeModelName && isModelReady
            )
        }

        val adapter = ModelPickerAdapter(modelItems) { item ->
            dialog.dismiss()
            lifecycleScope.launch(Dispatchers.IO) {
                if (isModelReady) {
                    try { engine.cleanUp() } catch (e: Exception) { Log.e(TAG, "Error cleaning up", e) }
                }
                loadModelFileDirectly(item.cleanName, item.file)
            }
        }
        modelsRv.adapter = adapter

        sheetView.findViewById<ImageButton>(R.id.btn_close_model_picker).setOnClickListener {
            dialog.dismiss()
        }

        sheetView.findViewById<View>(R.id.btn_import_from_sheet).setOnClickListener {
            dialog.dismiss()
            getContent.launch(arrayOf("*/*"))
        }

        dialog.show()
    }

    private suspend fun checkExistingInternalModel() {
        val appModelsDir = ensureModelsDirectory()
        val internalModels = appModelsDir.listFiles()?.filter { it.name.endsWith(".gguf") } ?: emptyList()

        if (internalModels.isNotEmpty()) {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val savedFilename = prefs.getString(KEY_LAST_USED_MODEL, null)

            val selectedModel = internalModels.firstOrNull { it.name.equals(savedFilename, ignoreCase = true) }
                ?: internalModels.firstOrNull { it.name.contains("Bonsai", ignoreCase = true) }
                ?: internalModels.first()

            val cleanName = cleanModelDisplayName(selectedModel.name)
            loadModelFileDirectly(cleanName, selectedModel)
        } else {
            withContext(Dispatchers.Main) {
                ggufTv.text = "⚡ Please select a GGUF model file to start"
                userInputEt.hint = "Pick a GGUF model file..."
                userActionFab.setImageResource(R.drawable.outline_folder_open_24)
            }
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
            isModelReady = false
            userActionFab.isEnabled = false
            userInputEt.hint = "Loading $modelDisplayName..."
            ggufTv.text = "⏳ Loading model: $modelDisplayName..."
        }

        try {
            engine.state.first { it is InferenceEngine.State.Initialized }

            engine.loadModel(modelFile.path)
            try {
                engine.setSystemPrompt("You are a helpful assistant")
            } catch (e: Exception) {
                Log.w(TAG, "Could not set system prompt", e)
            }
            activeModelName = modelDisplayName

            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_USED_MODEL, modelFile.name)
                .apply()

            withContext(Dispatchers.Main) {
                isModelReady = true
                subtitleTv.text = "$activeModelName • ARM Neon KleidiAI"
                composerModelChip.text = "⚡ $activeModelName ▾"
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
        updateEmptyStateVisibility()

        val assistantIndex = messages.size
        val assistantMessageObj = Message(UUID.randomUUID().toString(), "", false)
        messages.add(assistantMessageObj)

        messageAdapter.notifyItemRangeInserted(assistantIndex - 1, 2)
        messagesRv.scrollToPosition(assistantIndex)

        sendPromptToEngine(userMsg, assistantIndex)
    }

    private fun regenerateLastTurn() {
        if (isGenerating || !isModelReady || messages.isEmpty()) return

        val lastUserIndex = messages.indexOfLast { it.isUser }
        if (lastUserIndex == -1) return

        val userMsg = messages[lastUserIndex].content

        while (messages.size > lastUserIndex + 1) {
            messages.removeAt(messages.size - 1)
        }
        while (currentSession.messages.size > lastUserIndex + 1) {
            currentSession.messages.removeAt(currentSession.messages.size - 1)
        }

        val assistantIndex = messages.size
        val assistantMessageObj = Message(UUID.randomUUID().toString(), "", false)
        messages.add(assistantMessageObj)

        messageAdapter.notifyDataSetChanged()
        messagesRv.scrollToPosition(assistantIndex)

        userInputEt.isEnabled = false
        userActionFab.isEnabled = false
        isGenerating = true
        userActionFab.setImageResource(R.drawable.ic_stop)

        sendPromptToEngine(userMsg, assistantIndex)
    }

    private fun sendPromptToEngine(userMsg: String, assistantIndex: Int) {
        val startTime = System.currentTimeMillis()
        var firstTokenTime: Long? = null
        var tokenCount = 0

        val response = StringBuilder()

        generationJob = lifecycleScope.launch(Dispatchers.Default) {
            try {
                engine.sendUserPrompt(userMsg, 512)
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
                withContext(Dispatchers.Main) {
                    if (assistantIndex < messages.size) {
                        val errorMsg = messages[assistantIndex].copy(
                            content = "⚠️ Couldn't generate a response — please try again."
                        )
                        messages[assistantIndex] = errorMsg
                        messageAdapter.notifyItemChanged(assistantIndex)
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isGenerating = false
                    userInputEt.isEnabled = true
                    userActionFab.isEnabled = true
                    userActionFab.setImageResource(R.drawable.outline_send_24)

                    val rawResponse = response.toString()
                    val displayResponse = if (rawResponse.trim().isEmpty()) {
                        if (rawResponse.isNotEmpty()) rawResponse else "⚠️ Couldn't generate a response — please try again."
                    } else {
                        rawResponse
                    }

                    if (assistantIndex < messages.size) {
                        val finalMsg = messages[assistantIndex].copy(content = displayResponse)
                        messages[assistantIndex] = finalMsg
                        messageAdapter.notifyItemChanged(assistantIndex)
                        currentSession.messages.add(finalMsg)
                        chatSessionManager.saveSession(currentSession)
                        refreshHistorySidebar()
                    }

                    val first = firstTokenTime ?: startTime
                    val generationSeconds = (System.currentTimeMillis() - first) / 1000.0
                    val tokPerSec = if (generationSeconds > 0 && tokenCount > 1) {
                        (tokenCount - 1) / generationSeconds
                    } else {
                        0.0
                    }

                    val ttft = firstTokenTime?.let { it - startTime } ?: 0

                    ggufTv.text = "⚡ %.1f tok/s | TTFT: %dms | Ctx: 4096 | Threads: 4"
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
                    engine.setSystemPrompt("You are a helpful assistant")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting engine on clear chat", e)
            }
        }
        messages.clear()
        currentSession = ChatSession()
        messageAdapter.notifyDataSetChanged()
        updateEmptyStateVisibility()
        Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show()
    }

    private fun showModelInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle("Active Model Info")
            .setMessage(
                "• Active Model: $activeModelName\n" +
                "• Quantization: Q1_0 (1-bit PrismML)\n" +
                "• Runtime: llama.cpp native\n" +
                "• Acceleration: ARM Neon + KleidiAI\n" +
                "• Device: Moto G54 5G\n" +
                "• Mode: 100% Offline & Private\n" +
                "• Context Window: 4096 tokens\n" +
                "• CPU Threads: 4 Threads\n\n" +
                "💡 Note: Q1_0 is a 1-bit compressed format designed for ultra-fast, low-RAM edge execution."
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
            return "Bonsai-Q1_0"
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
        private const val PREFS_NAME = "EdgeAiPrefs"
        private const val KEY_LAST_USED_MODEL = "last_used_model_filename"
        private const val KEY_DARK_MODE = "is_dark_mode"
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
