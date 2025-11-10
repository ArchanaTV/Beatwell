package com.beatwell.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beatwell.app.R
import com.beatwell.app.models.MealOption
import com.bumptech.glide.Glide

class MealOptionAdapter(
    private val mealOptions: List<MealOption>,
    private val onItemClick: (MealOption) -> Unit
) : RecyclerView.Adapter<MealOptionAdapter.MealOptionViewHolder>() {
    
    private var selectedPosition = -1
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealOptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_option, parent, false)
        return MealOptionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MealOptionViewHolder, position: Int) {
        val mealOption = mealOptions[position]
        holder.bind(mealOption, position == selectedPosition)
        
        holder.itemView.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                val previousSelected = selectedPosition
                selectedPosition = adapterPosition
                if (previousSelected != -1) notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onItemClick(mealOptions[adapterPosition])
            }
        }
    }
    
    override fun getItemCount(): Int = mealOptions.size
    
    class MealOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMealImage: ImageView = itemView.findViewById(R.id.ivMealImage)
        private val tvMealName: TextView = itemView.findViewById(R.id.tvMealName)
        private val tvMealDescription: TextView = itemView.findViewById(R.id.tvMealDescription)
        private val tvCalories: TextView = itemView.findViewById(R.id.tvCalories)
        
        fun bind(mealOption: MealOption, isSelected: Boolean) {
            // Load meal image using Glide
            Glide.with(itemView.context)
                .load(mealOption.imageUrl)
                .placeholder(R.drawable.ic_meal_placeholder)
                .error(R.drawable.ic_meal_placeholder)
                .into(ivMealImage)
            
            tvMealName.text = mealOption.name
            tvMealDescription.text = mealOption.description
            tvCalories.text = "${mealOption.calories} cal"
            
            // Update selection state
            itemView.isSelected = isSelected
            itemView.setBackgroundResource(
                if (isSelected) R.drawable.meal_option_selected else R.drawable.meal_option_unselected
            )
        }
    }
}
