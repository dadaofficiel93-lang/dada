package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
    assertEquals("Vente", appName)
  }

  @Test
  fun `verify seller contact constants`() {
    assertEquals("07 12 30 85", com.example.ui.FormatUtils.SELLER_PHONE)
    assertEquals("dadaofficiel93@gmail.com", com.example.ui.FormatUtils.SELLER_EMAIL)
  }
}
