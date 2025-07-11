package com.example.notesfrontend

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject

// PUBLIC_INTERFACE
data class Note(
    val id: Long,
    var title: String,
    var content: String,
    var timestamp: Long
)

// PUBLIC_INTERFACE
class MainActivity : AppCompatActivity() {

    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var adapter: NotesAdapter
    private lateinit var emptyState: TextView
    private var notes: MutableList<Note> = mutableListOf()
    private var filteredNotes: MutableList<Note> = mutableListOf()
    private var prefsName = "NotesApp"
    private var storageKey = "notes"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme_Light) // enforce light theme
        setContentView(R.layout.activity_main)

        notesRecyclerView = findViewById(R.id.notes_recycler_view)
        fab = findViewById(R.id.fab)
        emptyState = findViewById(R.id.empty_text)

        notesRecyclerView.layoutManager = LinearLayoutManager(this)

        loadNotes()
        adapter = NotesAdapter(filteredNotes, onEdit = { note -> openEditor(note) }, onDelete = { note -> confirmDelete(note) })
        notesRecyclerView.adapter = adapter
        showOrHideEmptyState()

        fab.setOnClickListener {
            openEditor(null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.queryHint = "Search Notes..."
        searchView?.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterNotes(newText ?: "")
                return true
            }
        })
        return true
    }

    private fun loadNotes() {
        notes.clear()
        val sp = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val jsonString = sp.getString(storageKey, null) ?: ""
        if (jsonString.isNotBlank()) {
            val arr = JSONArray(jsonString)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                notes.add(
                    Note(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
        }
        notes.sortByDescending { it.timestamp }
        filteredNotes.clear()
        filteredNotes.addAll(notes)
    }

    private fun saveNotes() {
        val sp = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (note in notes) {
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("title", note.title)
            obj.put("content", note.content)
            obj.put("timestamp", note.timestamp)
            arr.put(obj)
        }
        sp.edit().putString(storageKey, arr.toString()).apply()
    }

    private fun openEditor(note: Note?) {
        val isEdit = note != null
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit Note" else "New Note")
            .setView(R.layout.dialog_edit_note)
            .setPositiveButton(if (isEdit) "Save" else "Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val etTitle = dialog.findViewById<EditText>(R.id.et_title)
            val etContent = dialog.findViewById<EditText>(R.id.et_content)
            if (isEdit && note != null) {
                etTitle?.setText(note.title)
                etContent?.setText(note.content)
            }
            val positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveBtn.setOnClickListener {
                val title = etTitle?.text?.toString()?.trim() ?: ""
                val content = etContent?.text?.toString()?.trim() ?: ""
                if (title.isBlank() && content.isBlank()) {
                    etTitle?.error = "Title or content required"
                    return@setOnClickListener
                }
                if (isEdit && note != null) {
                    note.title = title
                    note.content = content
                    note.timestamp = System.currentTimeMillis()
                } else {
                    val new = Note(
                        id = System.currentTimeMillis(), // simple unique id
                        title = title,
                        content = content,
                        timestamp = System.currentTimeMillis()
                    )
                    notes.add(new)
                }
                notes.sortByDescending { it.timestamp }
                saveNotes()
                filterNotes("")
                adapter.notifyDataSetChanged()
                showOrHideEmptyState()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun confirmDelete(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                notes.removeIf { it.id == note.id }
                saveNotes()
                filterNotes("")
                adapter.notifyDataSetChanged()
                showOrHideEmptyState()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun filterNotes(query: String) {
        filteredNotes.clear()
        if (query.isBlank()) {
            filteredNotes.addAll(notes)
        } else {
            filteredNotes.addAll(notes.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
            })
        }
        adapter.notifyDataSetChanged()
        showOrHideEmptyState()
    }

    private fun showOrHideEmptyState() {
        emptyState.visibility = if (filteredNotes.isEmpty()) View.VISIBLE else View.GONE
    }
}

// PUBLIC_INTERFACE
class NotesAdapter(
    private val notes: List<Note>,
    val onEdit: (Note) -> Unit,
    val onDelete: (Note) -> Unit
): RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.note_title)
        val content: TextView = itemView.findViewById(R.id.note_content)
        val timestamp: TextView = itemView.findViewById(R.id.note_timestamp)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) onEdit(notes[pos])
            }
            itemView.setOnLongClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) onDelete(notes[pos])
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(v)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.title.text = note.title.ifBlank { "(Untitled)" }
        holder.content.text = note.content
        holder.timestamp.text = android.text.format.DateFormat.format("MMM dd, yyyy  HH:mm", note.timestamp)
    }

    override fun getItemCount(): Int = notes.size
}
