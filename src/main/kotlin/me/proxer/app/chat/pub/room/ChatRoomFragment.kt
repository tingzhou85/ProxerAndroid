package me.proxer.app.chat.pub.room

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ContentView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.chat.pub.message.ChatActivity
import me.proxer.app.util.extension.enableFastScroll
import me.proxer.library.entity.chat.ChatRoom
import org.koin.androidx.viewmodel.ext.viewModel
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@ContentView(R.layout.fragment_chat_room)
class ChatRoomFragment : BaseContentFragment<List<ChatRoom>>() {

    companion object {
        fun newInstance() = ChatRoomFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by viewModel<ChatRoomViewModel>()

    private var adapter by Delegates.notNull<ChatRoomAdapter>()

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val recyclerView by bindView<RecyclerView>(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ChatRoomAdapter()

        adapter.clickSubject
            .autoDisposable(this.scope())
            .subscribe { item -> ChatActivity.navigateTo(requireActivity(), item.id, item.name, item.isReadOnly) }

        adapter.linkClickSubject
            .autoDisposable(this.scope())
            .subscribe { showPage(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setHasFixedSize(true)
        recyclerView.enableFastScroll()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun showData(data: List<ChatRoom>) {
        super.showData(data)

        adapter.swapDataAndNotifyWithDiffing(data.toList())
    }

    override fun hideData() {
        adapter.swapDataAndNotifyWithDiffing(emptyList())

        super.hideData()
    }
}
