package me.proxer.app.chat.pub.message

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.entity.chat.ChatMessage
import me.proxer.library.enums.ChatMessageAction
import java.util.Collections.emptyList
import java.util.Date
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class ChatViewModel(private val chatRoomId: String) : PagedViewModel<ChatMessage>() {

    override val itemsOnPage = 50

    override val dataSingle: Single<List<ChatMessage>>
        get() = api.chat().messages(chatRoomId)
            .messageId(data.value?.lastOrNull()?.id ?: "0")
            .buildSingle()

    val sendMessageError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    private var pollingDisposable: Disposable? = null

    private val sendMessageQueue: Queue<ChatMessage> = LinkedList()
    private val sentMessageIds = mutableSetOf<String>()
    private var sendMessageDisposable: Disposable? = null

    private var currentFirstId = "0"

    override fun onCleared() {
        pollingDisposable?.dispose()
        sendMessageDisposable?.dispose()

        pollingDisposable = null
        sendMessageDisposable = null

        super.onCleared()
    }

    override fun load() {
        dataDisposable?.dispose()
        dataDisposable = dataSingle
            .doAfterSuccess { newData -> hasReachedEnd = newData.size < itemsOnPage }
            .map { newData -> mergeNewDataWithExistingData(newData, data.value?.lastOrNull()?.id ?: "0") }
            .subscribeOn(Schedulers.io())
            .doAfterTerminate { isRefreshing = false }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                refreshError.value = null
                error.value = null
                isLoading.value = true
            }
            .doOnSuccess { if (pollingDisposable == null) startPolling() }
            .doAfterTerminate { isLoading.value = false }
            .subscribeAndLogErrors({
                currentFirstId = findFirstRemoteId(it) ?: "0"

                refreshError.value = null
                error.value = null
                data.value = it
            }, {
                if (data.value?.size ?: 0 > 0) {
                    refreshError.value = ErrorUtils.handle(it)
                } else {
                    error.value = ErrorUtils.handle(it)
                }
            })
    }

    fun sendMessage(text: String) {
        StorageHelper.user?.let { user ->
            val firstId = data.value?.firstOrNull()?.id?.toLong()
            val nextId = if (firstId == null || firstId >= 0) -1 else firstId - 1
            val message = ChatMessage(nextId.toString(), user.id, user.name, user.image,
                text, ChatMessageAction.NONE, Date())

            data.value = listOf(message).plus(data.value ?: emptyList())
            sendMessageQueue.offer(message)

            if (sendMessageDisposable?.isDisposed != false) {
                doSendMessages()
            }
        }
    }

    private fun mergeNewDataWithExistingData(newData: List<ChatMessage>, currentId: String): List<ChatMessage> {
        val messageIdsToDelete = newData.filter { it.action == ChatMessageAction.REMOVE_MESSAGE }
            .flatMap { listOf(it.id, it.message) }

        val previousSentMessageIdAmount = sentMessageIds.size
        val existingUnsendMessages = data.value?.takeWhile { it.id.toLong() < 0 } ?: emptyList()

        sentMessageIds.removeAll(newData.map { it.id })

        val result = data.value.let { existingData ->
            when (existingData) {
                null -> newData
                else -> when (currentId) {
                    "0" -> newData + existingData.dropWhile { it.id.toLong() < 0 }.filter { oldItem ->
                        newData.none { newItem -> oldItem.id == newItem.id }
                    }
                    else -> existingData.dropWhile { it.id.toLong() < 0 }.filter { oldItem ->
                        newData.none { newItem -> oldItem.id == newItem.id }
                    } + newData
                }
            }
        }

        val mergedResult = existingUnsendMessages.dropLast(previousSentMessageIdAmount - sentMessageIds.size) + result

        return when (messageIdsToDelete.isNotEmpty()) {
            true -> mergedResult.filterNot { it.id in messageIdsToDelete }
            false -> mergedResult
        }
    }

    private fun startPolling(immediate: Boolean = false) {
        pollingDisposable?.dispose()
        pollingDisposable = Single.fromCallable { Validators.validateLogin() }
            .flatMap { api.chat().messages(chatRoomId).messageId("0").buildSingle() }
            .repeatWhen { it.concatMap { Flowable.timer(3, TimeUnit.SECONDS) } }
            .retryWhen { it.concatMap { Flowable.timer(3, TimeUnit.SECONDS) } }
            .map { newData -> mergeNewDataWithExistingData(newData, "0") }
            .let { if (!immediate) it.delaySubscription(3, TimeUnit.SECONDS) else it }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAndLogErrors {
                val firstId = findFirstRemoteId(it)

                if (firstId != null && firstId != currentFirstId) {
                    currentFirstId = firstId

                    data.value = it
                }
            }
    }

    private fun doSendMessages() {
        sendMessageDisposable?.dispose()

        sendMessageQueue.poll()?.let { item ->
            sendMessageDisposable = Single.fromCallable { Validators.validateLogin() }
                .flatMap { api.chat().sendMessage(chatRoomId, item.message).buildOptionalSingle() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeAndLogErrors({
                    it.toNullable()?.let {
                        sentMessageIds.add(it)

                        startPolling(true)
                        doSendMessages()
                    }
                }, {
                    data.value = data.value?.dropWhile { it.id.toLong() < 0 }
                    sendMessageQueue.clear()

                    sendMessageError.value = ErrorUtils.handle(it)
                })
        }
    }

    private fun findFirstRemoteId(data: List<ChatMessage>): String? {
        return data.dropWhile { it.id.toLong() < 0 }.firstOrNull()?.id
    }
}
