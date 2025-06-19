package com.cvmmk

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cvmmk.databinding.ItemOwnerAccountBinding

class OwnerAccountAdapter(
    private var accounts: List<User>,
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<OwnerAccountAdapter.OwnerAccountViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OwnerAccountViewHolder {
        val binding = ItemOwnerAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OwnerAccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OwnerAccountViewHolder, position: Int) {
        holder.bind(accounts[position])
    }

    override fun getItemCount(): Int = accounts.size

    fun updateData(newAccounts: List<User>) {
        accounts = newAccounts
        notifyDataSetChanged()
    }

    inner class OwnerAccountViewHolder(private val binding: ItemOwnerAccountBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(account: User) {
            with(binding) {
                tvOwnerName.text = account.username ?: "Tidak tersedia"
                tvOwnerRole.text = "Owner"
                tvUsername.text = account.username ?: "Tidak tersedia"
                btnEdit.setOnClickListener { onEditClick(account) }
                btnDelete.setOnClickListener { onDeleteClick(account) }
            }
        }
    }
}