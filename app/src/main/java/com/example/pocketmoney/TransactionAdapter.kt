package com.example.pocketmoney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    private var items: List<TransactionListItem>,
    private val currencySign: String, // Added to pass currency sign
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_TRANSACTION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TransactionListItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is TransactionListItem.TransactionItem -> VIEW_TYPE_TRANSACTION
            else -> throw IllegalArgumentException("Unknown TransactionListItem type at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            VIEW_TYPE_TRANSACTION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_transaction, parent, false)
                TransactionViewHolder(view, onItemClick)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TransactionListItem.DateHeader -> {
                (holder as DateHeaderViewHolder).bind(item, currencySign)
            }
            is TransactionListItem.TransactionItem -> {
                (holder as TransactionViewHolder).bind(item.transaction, currencySign)
            }
            else -> {
                throw IllegalArgumentException("Unknown TransactionListItem type at position $position")
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<TransactionListItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)

        fun bind(dateHeader: TransactionListItem.DateHeader, currencySign: String) {
            tvDate.text = dateHeader.date
            tvTotalAmount.text = "$currencySign${String.format("%.2f", dateHeader.totalAmount)}"
            tvTotalAmount.setTextColor(
                if (dateHeader.totalAmount >= 0) {
                    itemView.context.resources.getColor(R.color.green)
                } else {
                    itemView.context.resources.getColor(R.color.red)
                }
            )
        }
    }

    class TransactionViewHolder(
        itemView: View,
        private val onItemClick: (Transaction) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val categoryIcon: ImageView = itemView.findViewById(R.id.categoryIcon)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        fun bind(transaction: Transaction, currencySign: String) {
            categoryIcon.setImageResource(transaction.categoryIcon)
            tvCategory.text = transaction.category

            if (!transaction.note.isNullOrEmpty()) {
                tvNote.text = transaction.note
                tvNote.visibility = View.VISIBLE
            } else {
                tvNote.visibility = View.GONE
            }

            tvTime.text = transaction.time
            tvAmount.text = if (transaction.type == "Expense") {
                "-$currencySign${String.format("%.2f", transaction.amount)}"
            } else {
                "+$currencySign${String.format("%.2f", transaction.amount)}"
            }
            tvAmount.setTextColor(
                if (transaction.type == "Expense") {
                    itemView.context.resources.getColor(R.color.red)
                } else {
                    itemView.context.resources.getColor(R.color.green)
                }
            )

            itemView.setOnClickListener {
                onItemClick(transaction)
            }
        }
    }
}