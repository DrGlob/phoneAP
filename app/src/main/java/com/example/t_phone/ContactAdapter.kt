package com.example.t_phone

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

class ContactAdapter(context: Context, contacts: List<Contact>) :
    ArrayAdapter<Contact>(context, 0, contacts) {

    fun getItems(): List<Contact> = (0 until count).map { getItem(it)!! }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false)
        val contact = getItem(position)!!

        val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
        val phoneTextView = view.findViewById<TextView>(R.id.phoneTextView)
        val callButton = view.findViewById<ImageButton>(R.id.callButton)

        nameTextView.text = contact.name
        phoneTextView.text = contact.phone

        phoneTextView.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Phone", contact.phone)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Номер скопирован", Toast.LENGTH_SHORT).show()
        }

        callButton.setOnClickListener {
            (context as MainActivity).makePhoneCall(contact.phone)
        }
        return view
    }
}