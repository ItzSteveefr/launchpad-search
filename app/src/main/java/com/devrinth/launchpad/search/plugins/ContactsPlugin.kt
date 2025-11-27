package com.devrinth.launchpad.search.plugins

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.ContactsContract
import android.util.LruCache
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.devrinth.launchpad.adapters.ResultAdapter
import com.devrinth.launchpad.search.SearchPlugin
import com.devrinth.launchpad.utils.IntentUtils
import com.devrinth.launchpad.utils.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
class ContactsPlugin(mContext: Context) : SearchPlugin(mContext) {

    override var ID = "contacts"

    private lateinit var mContentResolver : ContentResolver

    private val nameProjection = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.PHOTO_URI,
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
    )

    private var searchNumber: Boolean = true
    private var searchEmail: Boolean = true

    private var isProcessing = false

    // Cache for loaded contacts
    private var cachedContacts = mutableMapOf<String, ContactData>()

    //Avatar Cache: Avoid regenerating text avatars
    private val avatarCache = LruCache<String, Drawable>(100)

    //Material Design Color Palette
    private val MATERIAL_COLORS = listOf(
        Color.parseColor("#F44336"), Color.parseColor("#E91E63"), Color.parseColor("#9C27B0"),
        Color.parseColor("#673AB7"), Color.parseColor("#3F51B5"), Color.parseColor("#2196F3"),
        Color.parseColor("#03A9F4"), Color.parseColor("#00BCD4"), Color.parseColor("#009688"),
        Color.parseColor("#4CAF50"), Color.parseColor("#8BC34A"), Color.parseColor("#CDDC39"),
        Color.parseColor("#FFEB3B"), Color.parseColor("#FFC107"), Color.parseColor("#FF9800"),
        Color.parseColor("#FF5722"), Color.parseColor("#795548"), Color.parseColor("#607D8B")
    )

    /* Data classes unique to the plugin */
    private data class ContactData(
        val id: String,
        val displayName: String,
        val photoUri: String?,
        val phoneNumbers: MutableList<String>,
        val emailAddresses: MutableList<String>
    )

    private data class ScoredContact(
        val contact: ContactData,
        val score: Double
    )

    override fun pluginInit() {
        val perms = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!perms) {
            Toast.makeText(mContext, "Contacts permission needed", Toast.LENGTH_SHORT).show()
            return
        }

        mContentResolver = mContext.contentResolver
        loadContacts()

        super.pluginInit()
        searchNumber = getPluginSetting("search_phone", true) as Boolean
        searchEmail = getPluginSetting("search_email", true) as Boolean
    }

    override fun pluginProcess(query: String) {
        if (!INIT || query.isEmpty() || query.length < 2 || isProcessing) {
            pluginResult(emptyList(), "")
            return
        }
        isProcessing = false

        CoroutineScope(Dispatchers.Main).launch {
            pluginResult(filterContacts(query), query)
            isProcessing = false
        }
    }

    @SuppressLint("Range")
    private fun loadContacts() {
        val allContactsCursor = mContentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            nameProjection,
            null,
            null,
            null
        )

        try {
            if (allContactsCursor != null) {
                while (allContactsCursor.moveToNext()) {
                    val contactId = allContactsCursor.getString(allContactsCursor.getColumnIndex(ContactsContract.Contacts._ID))
                    val displayName = allContactsCursor.getString(allContactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY))
                    val photoUri = allContactsCursor.getString(allContactsCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI))

                    cachedContacts[contactId] = ContactData(
                        id = contactId,
                        displayName = displayName ?: "",
                        photoUri = photoUri,
                        phoneNumbers = mutableListOf(),
                        emailAddresses = mutableListOf()
                    )
                }
                allContactsCursor.close()
            }
        } catch (e: Exception) {
            allContactsCursor?.close()
        }

        if (searchNumber) {
            val phoneCursor = mContentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )
            try {
                if (phoneCursor != null) {
                    while (phoneCursor.moveToNext()) {
                        val contactId = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                        val phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                        cachedContacts[contactId]?.phoneNumbers?.add(phoneNumber ?: "")
                    }
                    phoneCursor.close()
                }
            } catch (e: Exception) {
                phoneCursor?.close()
            }
        }

        if (searchEmail) {
            val emailCursor = mContentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Email.ADDRESS
                ),
                null,
                null,
                null
            )

            try {
                if (emailCursor != null) {
                    while (emailCursor.moveToNext()) {
                        val contactId = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID))
                        val emailAddress = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS))

                        cachedContacts[contactId]?.emailAddresses?.add(emailAddress ?: "")
                    }
                    emailCursor.close()
                }
            } catch (e: Exception) {
                emailCursor?.close()
            }
        }
    }

    @SuppressLint("Range")
    private suspend fun filterContacts(query: String): List<ResultAdapter> {
        return withContext(Dispatchers.Default) {
            val queryLower = query.lowercase().trim()
            val allContacts = cachedContacts

            val scoredContacts = cachedContacts.values
                .filter { it.phoneNumbers.isNotEmpty() } //Only contacts with phone numbers
                .mapNotNull { contact ->
                val score = calculateFuzzyScore(queryLower, contact)
                if (score > 0.3) {
                    ScoredContact(contact, score)
                } else null
            }.sortedByDescending { it.score }

            val filteredContacts = scoredContacts.map { scoredContact ->
                val contact = scoredContact.contact

                val contactPhoto: Drawable = try {
                    contact.photoUri?.let {
                        getContactPhotoDrawable(it.toUri()) ?: createTextAvatar(contact.id, contact.displayName)
                    } ?: createTextAvatar(contact.id, contact.displayName)
                } catch (e: Exception) {
                    createTextAvatar(contact.id, contact.displayName)
                }

                val phoneNumber = contact.phoneNumbers.firstOrNull()?.trim()
                val emailAddress = contact.emailAddresses.firstOrNull()?.trim()

                val contactInfo = when {
                    phoneNumber != null && emailAddress != null -> "$phoneNumber • $emailAddress"
                    phoneNumber != null -> phoneNumber
                    emailAddress != null -> emailAddress
                    else -> ""
                }

                val callIntent = if (!phoneNumber.isNullOrEmpty()) {
                    IntentUtils.getCallIntent(phoneNumber)
                } else null

                val emailIntent = if (!emailAddress.isNullOrEmpty()) {
                    IntentUtils.getEmailIntent(emailAddress)
                } else null

                val primaryIntent = callIntent ?: emailIntent
                val secondaryIntent = if (callIntent != null && emailIntent != null) emailIntent else null

                ResultAdapter(
                    contact.displayName,
                    contactInfo,
                    contactPhoto,
                    action1 = if (primaryIntent != null) {
                        { mContext.startActivity(primaryIntent) }
                    } else null,
                    action2 = if (secondaryIntent != null) {
                        { mContext.startActivity(secondaryIntent) }
                    } else null
                )
            }

            filteredContacts
        }
    }

    private fun getContactPhotoDrawable(photoUri: Uri?): Drawable? {
        if (photoUri == null) {
            return null
        }
        val contentResolver = mContext.contentResolver
        return try {
            val inputStream = contentResolver.openInputStream(photoUri)
            Drawable.createFromStream(inputStream, photoUri.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    //Creates a Material-style initials avatar with caching
    private fun createTextAvatar(id: String, name: String): Drawable {
        avatarCache.get(id)?.let { return it }

        val initials = name
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")

        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val colorIndex = name.hashCode().absoluteValue % MATERIAL_COLORS.size
        paint.color = MATERIAL_COLORS[colorIndex]
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.color = Color.WHITE
        paint.textSize = size / 2.5f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val bounds = Rect()
        paint.getTextBounds(initials, 0, initials.length, bounds)
        val textHeight = bounds.height()
        canvas.drawText(initials, size / 2f, size / 2f + textHeight / 2f, paint)

        val drawable = BitmapDrawable(mContext.resources, bitmap)
        avatarCache.put(id, drawable)
        return drawable
    }

    private fun calculateFuzzyScore(query: String, contact: ContactData): Double {
        var maxScore = 0.0
        maxScore = maxOf(maxScore, fuzzyMatch(query, contact.displayName.lowercase()))

        if (searchNumber) {
            contact.phoneNumbers.forEach { phone ->
                val cleanPhone = phone.replace(Regex("[^0-9]"), "")
                val cleanQuery = query.replace(Regex("[^0-9]"), "")
                if (cleanQuery.isNotEmpty()) {
                    maxScore = maxOf(maxScore, fuzzyMatch(cleanQuery, cleanPhone))
                }
                maxScore = maxOf(maxScore, fuzzyMatch(query, phone.lowercase()))
            }
        }

        if (searchEmail) {
            contact.emailAddresses.forEach { email ->
                maxScore = maxOf(maxScore, fuzzyMatch(query, email.lowercase()))
            }
        }

        return maxScore
    }

    /*
        Fuzzy matching algorithm that calculates a similarity score between the query and target strings.
        - Returns a score between 0.0 (no match) and 1.0 (exact match).
        - Uses Levenshtein distance for similarity calculation.
        - Supports whole word matching and substring matching.
     */
    private fun fuzzyMatch(query: String, target: String): Double {
        if (query.isEmpty() || target.isEmpty()) return 0.0

        /*
            Lazy processing beforehand to process simple strings
        */
        if (target.contains(query)) {
            return when {
                target == query -> 1.0
                target.startsWith(query) -> 0.9
                else -> 0.8
            }
        }

        val queryWords = query.split(" ").filter { it.isNotEmpty() }
        val targetWords = target.split(" ").filter { it.isNotEmpty() }

        /*
            Check if there are any whole word matches between the query and target.
            If there are multiple words, we check if any of the target words start with or contain the query words.
        */
        if (queryWords.size > 1 || targetWords.size > 1) {
            var wordMatches = 0
            val totalWords = queryWords.size

            queryWords.forEach { queryWord ->
                for (targetWord in targetWords) {
                    if (targetWord.startsWith(queryWord) || targetWord.contains(queryWord)) {
                        wordMatches++
                        break
                    }
                }
            }

            if (wordMatches > 0) {
                return (wordMatches.toDouble() / totalWords) * 0.7
            }
        }

        /*
            - The similarity score score is calculated by the Levenshtein distance and the total length of the string
            - 0 means no similarity, 1 means exact match
        */
        val distance = StringUtils.levenshteinDistance(query, target)
        val maxLength = maxOf(query.length, target.length)

        val similarity = 1.0 - (distance.toDouble() / maxLength)

        return if (similarity > 0.5) similarity * 0.6 else 0.0
    }

}