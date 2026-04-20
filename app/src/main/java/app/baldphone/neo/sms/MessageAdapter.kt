package app.baldphone.neo.sms

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.bald.uriah.baldphone.databinding.ItemMessageReceivedBinding
import com.bald.uriah.baldphone.databinding.ItemMessageSentBinding

class MessageAdapter : ListAdapter<SmsMessage, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isSent) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            SentViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
        } else {
            ReceivedViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        val timeText = DateFormat.getTimeFormat(holder.itemView.context).format(msg.date)
        when (holder) {
            is SentViewHolder -> holder.bind(msg, timeText)
            is ReceivedViewHolder -> holder.bind(msg, timeText)
        }
    }

    class SentViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: SmsMessage, time: String) {
            binding.messageBody.text = msg.body
            binding.messageTime.text = time
        }
    }

    class ReceivedViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: SmsMessage, time: String) {
            binding.messageBody.text = msg.body
            binding.messageTime.text = time
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SmsMessage>() {
        override fun areItemsTheSame(oldItem: SmsMessage, newItem: SmsMessage) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SmsMessage, newItem: SmsMessage) =
            oldItem == newItem
    }

    companion object {
        private const val VIEW_TYPE_SENT = 0
        private const val VIEW_TYPE_RECEIVED = 1
    }
}
