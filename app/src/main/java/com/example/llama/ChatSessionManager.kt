package com.example.llama

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "New Chat",
    val timestamp: Long = System.currentTimeMillis(),
    val messages: MutableList<Message> = mutableListOf()
) {
    fun getFormattedDate(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)

        return when {
            minutes < 2 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}

class ChatSessionManager(private val context: Context) {

    private val sessionsDir: File
        get() {
            val dir = File(context.filesDir, "chat_sessions")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun saveSession(session: ChatSession) {
        if (session.messages.isEmpty()) return

        // Auto-generate title if default
        if (session.title == "New Chat" || session.title.isBlank()) {
            val firstUserMsg = session.messages.firstOrNull { it.isUser }?.content
            if (!firstUserMsg.isNullOrBlank()) {
                val cleanTitle = firstUserMsg.take(30).trim()
                session.title = if (cleanTitle.length < firstUserMsg.length) "$cleanTitle..." else cleanTitle
            }
        }

        try {
            val json = JSONObject()
            json.put("id", session.id)
            json.put("title", session.title)
            json.put("timestamp", session.timestamp)

            val msgsArray = JSONArray()
            session.messages.forEach { msg ->
                val msgObj = JSONObject()
                msgObj.put("id", msg.id)
                msgObj.put("content", msg.content)
                msgObj.put("isUser", msg.isUser)
                msgsArray.put(msgObj)
            }
            json.put("messages", msgsArray)

            val file = File(sessionsDir, "session_${session.id}.json")
            file.writeText(json.toString(2))
            Log.i(TAG, "Saved chat session to JSON: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session ${session.id}", e)
        }
    }

    fun loadAllSessions(): List<ChatSession> {
        val list = mutableListOf<ChatSession>()
        val files = sessionsDir.listFiles()?.filter { it.name.startsWith("session_") && it.name.endsWith(".json") } ?: emptyList()

        files.forEach { file ->
            try {
                val content = file.readText()
                val json = JSONObject(content)
                val id = json.optString("id", UUID.randomUUID().toString())
                val title = json.optString("title", "Saved Chat")
                val timestamp = json.optLong("timestamp", file.lastModified())

                val msgsList = mutableListOf<Message>()
                val msgsArray = json.optJSONArray("messages")
                if (msgsArray != null) {
                    for (i in 0 until msgsArray.length()) {
                        val obj = msgsArray.getJSONObject(i)
                        msgsList.add(
                            Message(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                content = obj.optString("content", ""),
                                isUser = obj.optBoolean("isUser", false)
                            )
                        )
                    }
                }

                list.add(ChatSession(id, title, timestamp, msgsList))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read session file: ${file.name}", e)
            }
        }

        return list.sortedByDescending { it.timestamp }
    }

    fun deleteSession(sessionId: String): Boolean {
        val file = File(sessionsDir, "session_$sessionId.json")
        return if (file.exists()) file.delete() else false
    }

    private fun String?.isNullBlink(): Boolean = this == null || this.trim().isEmpty()

    companion object {
        private const val TAG = "ChatSessionManager"
    }
}
