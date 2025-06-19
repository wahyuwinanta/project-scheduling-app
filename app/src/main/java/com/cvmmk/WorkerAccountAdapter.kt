package com.cvmmk

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cvmmk.databinding.ItemWorkerAccountBinding

class WorkerAccountAdapter(
    private var accounts: List<WorkerAccount>,
    private val onEditClick: (WorkerAccount) -> Unit,
    private val onDeleteClick: (WorkerAccount) -> Unit
) : RecyclerView.Adapter<WorkerAccountAdapter.WorkerAccountViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkerAccountViewHolder {
        val binding = ItemWorkerAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkerAccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkerAccountViewHolder, position: Int) {
        holder.bind(accounts[position])
    }

    override fun getItemCount(): Int = accounts.size

    fun updateData(newAccounts: List<WorkerAccount>) {
        accounts = newAccounts
        notifyDataSetChanged()
    }

    inner class WorkerAccountViewHolder(private val binding: ItemWorkerAccountBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(account: WorkerAccount) {
            with(binding) {
                tvWorkerName.text = account.workerName
                tvWorkerRole.text = account.workerRole
                tvUsername.text = account.username
                btnEdit.setOnClickListener { onEditClick(account) }
                btnDelete.setOnClickListener { onDeleteClick(account) }
            }
        }
    }
}
