package com.example.llama

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatSessionAdapter(
    private val sessions: MutableList<ChatSession>,
    private val onSessionClick: (ChatSession) -> Unit,
    private val onDeleteClick: (ChatSession, Int) -> Unit,
    private val onSessionLongClick: ((ChatSession, Int) -> Unit)? = null,
    var activeSessionId: String? = null
) : RecyclerView.Adapter<ChatSessionAdapter.SessionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        holder.titleTv.text = session.title
        holder.timeTv.text = session.getFormattedDate()

        if (session.id == activeSessionId) {
            holder.itemView.setBackgroundColor(Color.parseColor("#1E293B"))
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.itemView.setOnClickListener { onSessionClick(session) }
        holder.itemView.setOnLongClickListener {
            onSessionLongClick?.invoke(session, position)
            true
        }
        holder.deleteBtn.setOnClickListener { onDeleteClick(session, position) }
    }

    override fun getItemCount(): Int = sessions.size

    fun removeAt(position: Int) {
        if (position in 0 until sessions.size) {
            sessions.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateSessions(newList: List<ChatSession>) {
        sessions.clear()
        sessions.addAll(newList)
        notifyDataSetChanged()
    }

    class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTv: TextView = view.findViewById(R.id.session_title)
        val timeTv: TextView = view.findViewById(R.id.session_time)
        val deleteBtn: ImageButton = view.findViewById(R.id.btn_delete_session)
    }
}
