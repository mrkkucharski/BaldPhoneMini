package app.baldphone.neo.sms

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.bald.uriah.baldphone.databinding.ItemSmsThreadBinding

class ThreadAdapter(
    private val onClick: (SmsThread) -> Unit
) : ListAdapter<SmsThread, ThreadAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSmsThreadBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSmsThreadBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(thread: SmsThread) {
            binding.root.setOnClickListener { onClick(thread) }
            binding.contactName.text = thread.displayName
            binding.snippet.text = thread.snippet
            binding.timestamp.text = DateUtils.getRelativeTimeSpanString(
                thread.date,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
            binding.unreadDot.isVisible = !thread.isRead
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SmsThread>() {
        override fun areItemsTheSame(oldItem: SmsThread, newItem: SmsThread) =
            oldItem.threadId == newItem.threadId

        override fun areContentsTheSame(oldItem: SmsThread, newItem: SmsThread) =
            oldItem == newItem
    }
}
