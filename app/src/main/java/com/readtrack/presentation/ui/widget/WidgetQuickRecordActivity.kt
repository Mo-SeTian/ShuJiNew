package com.readtrack.presentation.ui.widget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.model.BookSnapshot
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.model.ProgressType
import com.readtrack.domain.repository.BookRepository
import com.readtrack.presentation.ui.theme.ReadTrackTheme
import com.readtrack.widget.WidgetUpdateHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WidgetQuickRecordActivity : ComponentActivity() {

    @Inject
    lateinit var bookRepository: BookRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L)
        if (bookId == -1L) {
            finish()
            return
        }

        setContent {
            ReadTrackTheme {
                Surface {
                    var book by remember { mutableStateOf<BookEntity?>(null) }
                    var inputValue by remember { mutableStateOf("") }
                    var errorText by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        val loaded = bookRepository.getBookById(bookId).firstOrNull()
                        if (loaded == null) {
                            finish()
                            return@LaunchedEffect
                        }
                        if (loaded.status != BookStatus.READING) {
                            Toast.makeText(
                                this@WidgetQuickRecordActivity,
                                "只有阅读中的书籍可以记录进度",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                            return@LaunchedEffect
                        }
                        book = loaded
                    }

                    val currentBook = book
                    if (currentBook != null) {
                        val isChapterBased = currentBook.progressType == ProgressType.CHAPTER
                        val label = if (isChapterBased) "章节" else "页数"
                        val currentValue = if (isChapterBased) currentBook.currentChapter.toString() else currentBook.currentPage.toInt().toString()
                        val maxValue = if (isChapterBased) {
                            currentBook.totalChapters?.toString() ?: "?"
                        } else {
                            currentBook.totalPages.toInt().toString()
                        }

                        AlertDialog(
                            onDismissRequest = { finish() },
                            title = { Text("记录阅读进度") },
                            text = {
                                Column {
                                    Text(
                                        text = currentBook.title,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "当前: $currentValue / $maxValue $label",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = inputValue,
                                        onValueChange = {
                                            inputValue = it
                                            errorText = null
                                        },
                                        label = { Text("新$label") },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = if (isChapterBased) KeyboardType.Number else KeyboardType.Decimal
                                        ),
                                        singleLine = true,
                                        isError = errorText != null,
                                        supportingText = errorText?.let { { Text(it) } }
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        submitRecord(currentBook, inputValue, isChapterBased)
                                    }
                                ) {
                                    Text("确认")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { finish() }) {
                                    Text("取消")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun submitRecord(
        book: BookEntity,
        input: String,
        isChapterBased: Boolean
    ) {
        val newValue = if (isChapterBased) {
            input.toIntOrNull()
        } else {
            input.toDoubleOrNull()
        }

        if (newValue == null) {
            Toast.makeText(this, "请输入有效的数值", Toast.LENGTH_SHORT).show()
            return
        }

        val currentTime = System.currentTimeMillis()
        val record: ReadingRecordEntity
        val updatedBook: BookEntity

        if (isChapterBased) {
            val maxChapter = book.totalChapters ?: 0
            val newChapter = (newValue as Int).coerceIn(0, maxChapter)
            if (newChapter < book.currentChapter) {
                Toast.makeText(this, "新章节不能小于当前章节", Toast.LENGTH_SHORT).show()
                return
            }
            val chaptersRead = newChapter - book.currentChapter
            record = ReadingRecordEntity(
                bookId = book.id,
                bookSnapshot = BookSnapshot.from(book, book.status),
                pagesRead = chaptersRead.toDouble(),
                fromPage = book.currentChapter.toDouble(),
                toPage = newChapter.toDouble(),
                chaptersRead = chaptersRead,
                date = currentTime
            )
            updatedBook = book.copy(
                currentChapter = newChapter,
                lastReadAt = currentTime,
                updatedAt = currentTime
            )
        } else {
            val newPage = (newValue as Double).coerceIn(0.0, book.totalPages)
            if (newPage < book.currentPage) {
                Toast.makeText(this, "新页数不能小于当前页数", Toast.LENGTH_SHORT).show()
                return
            }
            val pagesActuallyRead = newPage - book.currentPage
            record = ReadingRecordEntity(
                bookId = book.id,
                bookSnapshot = BookSnapshot.from(book, book.status),
                pagesRead = pagesActuallyRead,
                fromPage = book.currentPage,
                toPage = newPage,
                date = currentTime
            )
            updatedBook = book.copy(
                currentPage = newPage,
                lastReadAt = currentTime,
                updatedAt = currentTime
            )
        }

        lifecycleScope.launch {
            try {
                bookRepository.insertRecordAndUpdateBook(record, updatedBook)
                WidgetUpdateHelper.triggerUpdate(this@WidgetQuickRecordActivity)
                Toast.makeText(this@WidgetQuickRecordActivity, "记录成功", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@WidgetQuickRecordActivity,
                    "记录失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"

        fun createIntent(context: Context, bookId: Long): Intent {
            return Intent(context, WidgetQuickRecordActivity::class.java).apply {
                putExtra(EXTRA_BOOK_ID, bookId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
    }
}
