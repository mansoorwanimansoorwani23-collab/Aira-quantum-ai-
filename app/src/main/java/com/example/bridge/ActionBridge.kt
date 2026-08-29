package com.example.bridge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat

data class ContactMatch(val name: String, val phoneNumber: String)

data class ActionResult(
    val success: Boolean,
    val message: String,
    val toolName: String,
    val details: String? = null
)

class ActionBridge(private val context: Context) {

    @JavascriptInterface
    fun openWhatsApp(): String {
        return try {
            val packageManager = context.packageManager
            val whatsappIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                ?: packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")

            if (whatsappIntent != null) {
                whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(whatsappIntent)
                "WhatsApp opened successfully."
            } else {
                // Fallback to web/deep link
                val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (urlIntent.resolveActivity(packageManager) != null) {
                    context.startActivity(urlIntent)
                    "WhatsApp web link opened."
                } else {
                    "WhatsApp is not installed on this device."
                }
            }
        } catch (e: Exception) {
            "Failed to open WhatsApp: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    @JavascriptInterface
    fun openApp(appName: String): String {
        val cleanName = appName.trim().lowercase()
        val pm = context.packageManager

        try {
            when {
                cleanName.contains("whatsapp") -> return openWhatsApp()
                cleanName.contains("setting") -> {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return "Device Settings opened."
                }
                cleanName.contains("youtube") -> {
                    val launchIntent = pm.getLaunchIntentForPackage("com.google.android.youtube")
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return "YouTube opened."
                    }
                    return openUrl("https://www.youtube.com")
                }
                cleanName.contains("instagram") -> {
                    val launchIntent = pm.getLaunchIntentForPackage("com.instagram.android")
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return "Instagram opened."
                    }
                    return openUrl("https://www.instagram.com")
                }
                cleanName.contains("chrome") || cleanName.contains("browser") -> {
                    val launchIntent = pm.getLaunchIntentForPackage("com.android.chrome")
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return "Google Chrome opened."
                    }
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                    return "Web browser opened."
                }
                cleanName.contains("map") -> {
                    val launchIntent = pm.getLaunchIntentForPackage("com.google.android.apps.maps")
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return "Google Maps opened."
                    }
                    return openUrl("https://maps.google.com")
                }
                cleanName.contains("spotify") -> {
                    val launchIntent = pm.getLaunchIntentForPackage("com.spotify.music")
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return "Spotify opened."
                    }
                    return "Spotify is not installed."
                }
                cleanName.contains("camera") -> {
                    val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (cameraIntent.resolveActivity(pm) != null) {
                        context.startActivity(cameraIntent)
                        return "Camera opened."
                    }
                }
                cleanName.contains("calculator") -> {
                    val calcIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_APP_CALCULATOR)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (calcIntent.resolveActivity(pm) != null) {
                        context.startActivity(calcIntent)
                        return "Calculator opened."
                    }
                }
            }

            // Search installed applications by label
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installedApps) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label.contains(cleanName) || cleanName.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return "${pm.getApplicationLabel(app)} opened."
                    }
                }
            }

            return "Application '$appName' is not installed or cannot be opened."
        } catch (e: Exception) {
            return "Could not open $appName: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    @JavascriptInterface
    fun openUrl(url: String): String {
        return try {
            var target = url.trim()
            if (!target.startsWith("http://") && !target.startsWith("https://")) {
                target = "https://$target"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Opened URL: $target"
        } catch (e: Exception) {
            "Failed to open URL $url: ${e.localizedMessage ?: "Invalid URL format"}"
        }
    }

    @JavascriptInterface
    fun makeCall(phoneNumber: String): String {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isBlank()) {
            return "Invalid phone number provided."
        }

        return try {
            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCallPermission) {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                "Calling $cleanNumber directly."
            } else {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                "Opened phone dialer with number $cleanNumber."
            }
        } catch (e: Exception) {
            "Failed to initiate call: ${e.localizedMessage ?: "Call action unavailable"}"
        }
    }

    @JavascriptInterface
    fun callContact(contactName: String): String {
        val cleanName = contactName.trim()
        if (cleanName.isBlank()) {
            return "Please provide a contact name to call."
        }

        val hasContactPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactPermission) {
            return "Contacts permission is required to find contacts. Please grant contacts permission."
        }

        val matches = findContactsByName(cleanName)

        return when {
            matches.isEmpty() -> {
                "I couldn't find any contact matching '$cleanName'."
            }
            matches.size == 1 -> {
                val contact = matches[0]
                val result = makeCall(contact.phoneNumber)
                "Found ${contact.name}. $result"
            }
            else -> {
                val namesList = matches.take(4).joinToString(", ") { "${it.name} (${it.phoneNumber})" }
                "I found ${matches.size} contacts matching '$cleanName': $namesList. Which one would you like to call?"
            }
        }
    }

    private fun getQueryVariations(query: String): List<String> {
        val q = query.trim().lowercase()
        val list = mutableListOf(query.trim())

        when {
            q == "mom" || q == "mummy" || q == "mother" || q == "maa" || q == "mataji" || q == "amma" -> {
                list.addAll(listOf("Mom", "Mummy", "Mother", "Maa", "Amma", "Mataji", "Maa ji"))
            }
            q == "dad" || q == "papa" || q == "father" || q == "pitaji" || q == "daddy" || q == "appa" -> {
                list.addAll(listOf("Dad", "Papa", "Father", "Daddy", "Appa", "Pitaji", "Papa ji"))
            }
            q == "bhai" || q == "brother" || q == "bro" || q == "bhaiya" -> {
                list.addAll(listOf("Bhai", "Brother", "Bro", "Bhaiya"))
            }
            q == "sister" || q == "sis" || q == "didi" || q == "behen" -> {
                list.addAll(listOf("Sister", "Sis", "Didi", "Behen"))
            }
            q == "wife" || q == "patni" || q == "biwi" -> {
                list.addAll(listOf("Wife", "Biwi", "Patni", "Home"))
            }
            q == "husband" || q == "pati" -> {
                list.addAll(listOf("Husband", "Pati"))
            }
        }
        return list.distinct()
    }

    private fun findContactsByName(query: String): List<ContactMatch> {
        val matches = mutableListOf<ContactMatch>()
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val variations = getQueryVariations(query)
        val seenNumbers = mutableSetOf<String>()

        for (variant in variations) {
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$variant%")

            var cursor: Cursor? = null
            try {
                cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    do {
                        val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Unknown"
                        val rawNumber = if (numberIndex >= 0) cursor.getString(numberIndex) else ""
                        val cleanNumber = rawNumber.replace(Regex("[^0-9+]"), "")
                        if (cleanNumber.isNotBlank() && seenNumbers.add(cleanNumber)) {
                            matches.add(ContactMatch(name = name, phoneNumber = cleanNumber))
                        }
                    } while (cursor.moveToNext())
                }
            } catch (_: Exception) {
                // Ignore cursor query errors
            } finally {
                cursor?.close()
            }

            if (matches.isNotEmpty()) break
        }

        return matches
    }

    fun executeTool(toolName: String, argsJson: String): ActionResult {
        return try {
            val json = org.json.JSONObject(if (argsJson.isBlank()) "{}" else argsJson)
            when (toolName) {
                "openWhatsApp" -> {
                    val msg = openWhatsApp()
                    ActionResult(success = true, message = msg, toolName = toolName)
                }
                "openApp" -> {
                    val appName = json.optString("appName", json.optString("name", ""))
                    val msg = openApp(appName)
                    ActionResult(success = true, message = msg, toolName = toolName, details = appName)
                }
                "openUrl" -> {
                    val url = json.optString("url", "")
                    val msg = openUrl(url)
                    ActionResult(success = true, message = msg, toolName = toolName, details = url)
                }
                "makeCall" -> {
                    val phone = json.optString("phoneNumber", json.optString("phone", ""))
                    val msg = makeCall(phone)
                    ActionResult(success = true, message = msg, toolName = toolName, details = phone)
                }
                "callContact" -> {
                    val contact = json.optString("contactName", json.optString("name", ""))
                    val msg = callContact(contact)
                    ActionResult(success = true, message = msg, toolName = toolName, details = contact)
                }
                else -> {
                    ActionResult(success = false, message = "Unsupported tool function: $toolName", toolName = toolName)
                }
            }
        } catch (e: Exception) {
            ActionResult(success = false, message = "Error executing $toolName: ${e.message}", toolName = toolName)
        }
    }
}
