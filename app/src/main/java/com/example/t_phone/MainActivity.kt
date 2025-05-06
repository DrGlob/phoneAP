package com.example.t_phone

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_READ_CONTACTS = 100
    private val REQUEST_CODE_CALL_PHONE = 101
    private val REQUEST_CODE_WRITE_CONTACTS = 102
    private lateinit var contactList: ListView
    private lateinit var removeDuplicatesButton: Button
    private lateinit var contactsAdapter: ContactAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        contactList = findViewById(R.id.contactList)
        removeDuplicatesButton = findViewById(R.id.button)
        contactsAdapter = ContactAdapter(this, mutableListOf())
        contactList.adapter = contactsAdapter

        removeDuplicatesButton.setOnClickListener {
            checkWriteContactsPermission()
        }

        checkContactsPermission()
    }

    private fun checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQUEST_CODE_READ_CONTACTS
            )
        } else {
            loadContacts()
        }
    }

    private fun checkWriteContactsPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_CONTACTS),
                REQUEST_CODE_WRITE_CONTACTS
            )
        } else {
            removeDuplicateContacts()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadContacts()
                } else {
                    Toast.makeText(this, "Доступ к контактам запрещён", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CODE_CALL_PHONE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Разрешение на звонок получено", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Разрешение на звонок запрещено", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CODE_WRITE_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    removeDuplicateContacts()
                } else {
                    Toast.makeText(this, "Разрешение на изменение контактов запрещено", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadContacts() {
        val contactsList = mutableListOf<Contact>()

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)
                contactsList.add(Contact(id, name, number))
            }
        }

        contactsAdapter.clear()
        contactsAdapter.addAll(contactsList)
        contactsAdapter.notifyDataSetChanged()
    }

    private fun removeDuplicateContacts() {
        val currentContacts = contactsAdapter.getItems()
        val seen = mutableSetOf<String>()
        val uniqueContacts = mutableListOf<Contact>()
        val contactsToDelete = mutableListOf<String>()

        for (contact in currentContacts) {
            val key = contact.name + contact.phone
            if (seen.add(key)) {
                uniqueContacts.add(contact)
            } else {
                contact.id?.let { contactsToDelete.add(it) }
            }
        }

        if (contactsToDelete.isEmpty()) {
            Toast.makeText(this, "Дубликатов не найдено", Toast.LENGTH_SHORT).show()
            return
        }

        val operations = ArrayList<ContentProviderOperation>()
        for (contactId in contactsToDelete) {
            operations.add(
                ContentProviderOperation.newDelete(
                    ContactsContract.RawContacts.CONTENT_URI
                )
                    .withSelection(
                        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                        arrayOf(contactId)
                    )
                    .build()
            )
        }

        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            contactsAdapter.clear()
            contactsAdapter.addAll(uniqueContacts)
            contactsAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Дубликаты удалены", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при удалении дубликатов: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun makePhoneCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CODE_CALL_PHONE
            )
        } else {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
        }
    }
}