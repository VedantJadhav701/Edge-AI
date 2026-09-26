package com.example.llama

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

data class Message(
    val id: String,
    val content: String,
    val isUser: Boolean
)

class MessageAdapter(
    private val messages: List<Message>,
    private val onRegenerateClicked: (() -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_ASSISTANT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_ASSISTANT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            val view = layoutInflater.inflate(R.layout.item_message_user, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = layoutInflater.inflate(R.layout.item_message_assistant, parent, false)
            AssistantMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val context = holder.itemView.context

        if (holder is UserMessageViewHolder) {
            val textView = holder.itemView.findViewById<TextView>(R.id.msg_content)
            textView.text = message.content
            setupUserLongClick(textView, context, message.content)
        } else if (holder is AssistantMessageViewHolder) {
            val textView = holder.itemView.findViewById<TextView>(R.id.msg_content)
            textView.text = ResponseCleaner.formatMarkdown(message.content)
            setupAssistantLongClick(textView, context, message.content)
        }
    }

    private fun setupUserLongClick(view: View, context: Context, text: String) {
        view.setOnLongClickListener {
            copyToClipboard(context, text)
            true
        }
    }

    private fun setupAssistantLongClick(view: View, context: Context, text: String) {
        view.setOnLongClickListener {
            val options = arrayOf("📋 Copy Text", "🔄 Regenerate Response")
            AlertDialog.Builder(context)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> copyToClipboard(context, text)
                        1 -> onRegenerateClicked?.invoke()
                    }
                }
                .show()
            true
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message Text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    override fun getItemCount(): Int = messages.size

    class UserMessageViewHolder(view: View) : RecyclerView.ViewHolder(view)
    class AssistantMessageViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
