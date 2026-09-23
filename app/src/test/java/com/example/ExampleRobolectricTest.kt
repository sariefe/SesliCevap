package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.ConversationHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var database: AppDatabase

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Sesli Cevap AI", appName)
  }

  @Test
  fun `conversation history dao stores user input text, ai response and timestamp`() = runBlocking {
    val convDao = database.conversationDao()
    val convId = convDao.insertConversation(
      ConversationEntity(
        id = 1L,
        title = "Test Conversation"
      )
    )

    val dao = database.conversationHistoryDao()
    val testTimestamp = 1700000000000L
    val historyItem = ConversationHistoryEntity(
      conversationId = convId,
      userInputText = "Yapay zeka nedir?",
      aiResponse = "Yapay zeka, insan benzeri düşünme ve öğrenme kabiliyeti sunan sistemlerdir.",
      timestamp = testTimestamp
    )

    val id = dao.insertHistory(historyItem)
    val fetched = dao.getHistoryByIdOnce(id)

    assertNotNull(fetched)
    assertEquals("Yapay zeka nedir?", fetched?.userInputText)
    assertEquals("Yapay zeka, insan benzeri düşünme ve öğrenme kabiliyeti sunan sistemlerdir.", fetched?.aiResponse)
    assertEquals(testTimestamp, fetched?.timestamp)

    val all = dao.getAllHistory().first()
    assertEquals(1, all.size)
    assertEquals(id, all[0].id)
  }
}
