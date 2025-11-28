package com.aliumar.fypfinal1

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var textChatTitle: TextView
    private lateinit var buttonBack: ImageButton

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageList: MutableList<ChatMessage>
    private lateinit var dbRef: DatabaseReference

    private var requestId: String? = null
    private var currentUserId: String? = null
    private var otherUserName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Retrieve data passed via Intent
        requestId = intent.getStringExtra("REQUEST_ID")
        currentUserId = intent.getStringExtra("CURRENT_USER_ID")
        otherUserName = intent.getStringExtra("OTHER_USER_NAME")

        if (requestId == null || currentUserId == null) {
            Toast.makeText(this, "Error initializing chat", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupRecyclerView()
        setupFirebase()
        setupListeners()
    }

    private fun initializeViews() {
        recyclerViewChat = findViewById(R.id.recyclerViewChat)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
        textChatTitle = findViewById(R.id.textChatTitle)
        buttonBack = findViewById(R.id.backButton)

        textChatTitle.text = "Chat with ${otherUserName ?: "User"}"
    }

    private fun setupRecyclerView() {
        messageList = mutableListOf()
        // Pass currentUserId to adapter to distinguish sent vs received messages
        chatAdapter = ChatAdapter(messageList, currentUserId!!)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // Start list from bottom
        recyclerViewChat.layoutManager = layoutManager
        recyclerViewChat.adapter = chatAdapter
    }

    private fun setupFirebase() {
        // Chat messages are stored under "chats -> requestId"
        dbRef = FirebaseDatabase.getInstance().getReference("chats").child(requestId!!)

        // Listen for new messages
        dbRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(ChatMessage::class.java)
                if (message != null) {
                    messageList.add(message)
                    chatAdapter.notifyItemInserted(messageList.size - 1)
                    recyclerViewChat.scrollToPosition(messageList.size - 1)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupListeners() {
        buttonBack.setOnClickListener { finish() }

        buttonSend.setOnClickListener {
            val messageText = editTextMessage.text.toString().trim()
            if (TextUtils.isEmpty(messageText)) {
                return@setOnClickListener
            }
            sendMessage(messageText)
        }
    }

    private fun sendMessage(message: String) {
        val messageId = dbRef.push().key ?: return
        val timestamp = System.currentTimeMillis()

        val chatMessage = ChatMessage(
            messageId = messageId,
            senderId = currentUserId!!,
            message = message,
            timestamp = timestamp
        )

        dbRef.child(messageId).setValue(chatMessage)
            .addOnSuccessListener {
                editTextMessage.setText("") // Clear input on success
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }
}