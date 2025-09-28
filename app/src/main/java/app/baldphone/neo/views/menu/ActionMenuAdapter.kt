package app.baldphone.neo.views.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.bald.uriah.baldphone.R

class ActionMenuAdapter(
    val items: List<ActionMenuItem>, private val onItemClick: (ActionMenuItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ITEM = 1
        private const val TYPE_DIVIDER = 2
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is ActionMenuItem.Separator) TYPE_DIVIDER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_DIVIDER) {
            DividerVH(inflater.inflate(R.layout.item_action_divider, parent, false))
        } else {
            ItemVH(inflater.inflate(R.layout.item_action_popup, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = items[position]
        if (holder is ItemVH) {
            holder.bind(row, onItemClick)
        }
    }

    class ItemVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.icon)
        private val label: TextView = itemView.findViewById(R.id.label)

        fun bind(item: ActionMenuItem, clickListener: (ActionMenuItem) -> Unit) {
            when (item) {
                is ActionMenuItem.Option -> {
                    icon.setImageResource(item.iconRes)
                    label.setText(item.labelRes)
                }

                is ActionMenuItem.Toggle -> {
                    icon.setImageResource(item.currentIconRes)
                    label.setText(item.currentLabelRes)
                }

                else -> {}
            }

            itemView.alpha = if (item.enabled) 1.0f else 0.5f

            itemView.setOnClickListener {
                if (item.enabled) {
                    clickListener.invoke(item)
                }
            }
        }
    }

    class DividerVH(itemView: View) : RecyclerView.ViewHolder(itemView)
}
