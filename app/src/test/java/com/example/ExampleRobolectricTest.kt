package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bridge.ActionBridge
import com.example.data.model.AIModelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Arushi AI", appName)
    }

    @Test
    fun `verify arushi system instruction contains multilingual and tool directives`() {
        val instruction = AIModelCatalog.ARUSHI_SYSTEM_INSTRUCTION
        assertTrue(instruction.contains("Arushi"))
        assertTrue(instruction.contains("Hindi"))
        assertTrue(instruction.contains("Hinglish"))
        assertTrue(instruction.contains("openWhatsApp"))
        assertTrue(instruction.contains("openApp"))
        assertTrue(instruction.contains("makeCall"))
        assertTrue(instruction.contains("callContact"))
        assertTrue(instruction.contains("Rauf"))
    }

    @Test
    fun `verify action bridge handles tool calls gracefully`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bridge = ActionBridge(context)

        val whatsappResult = bridge.executeTool("openWhatsApp", "{}")
        assertNotNull(whatsappResult)
        assertEquals("openWhatsApp", whatsappResult.toolName)

        val urlResult = bridge.executeTool("openUrl", """{"url":"https://example.com"}""")
        assertTrue(urlResult.success)
        assertEquals("openUrl", urlResult.toolName)

        val appResult = bridge.executeTool("openApp", """{"appName":"YouTube"}""")
        assertNotNull(appResult)
        assertEquals("openApp", appResult.toolName)

        val callResult = bridge.executeTool("makeCall", """{"phoneNumber":"9876543210"}""")
        assertNotNull(callResult)
        assertEquals("makeCall", callResult.toolName)
    }
}

