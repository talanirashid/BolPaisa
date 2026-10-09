package com.bolpaisa.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bolpaisa.app.R

data class OnboardingSlide(
    val title: String,
    val subtitle: String,
    val iconResId: Int,
    val actionText: String? = null,
    val onActionClick: (() -> Unit)? = null
)

class OnboardingAdapter(
    private val slides: List<OnboardingSlide>
) : RecyclerView.Adapter<OnboardingAdapter.SlideViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding_slide, parent, false)
        return SlideViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount(): Int = slides.size

    inner class SlideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivSlideIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvSlideTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSlideSubtitle)
        private val btnAction: Button = itemView.findViewById(R.id.btnSlideAction)

        fun bind(slide: OnboardingSlide) {
            ivIcon.setImageResource(slide.iconResId)
            tvTitle.text = slide.title
            tvSubtitle.text = slide.subtitle

            if (slide.actionText != null && slide.onActionClick != null) {
                btnAction.text = slide.actionText
                btnAction.visibility = View.VISIBLE
                btnAction.setOnClickListener {
                    slide.onActionClick.invoke()
                }
            } else {
                btnAction.visibility = View.GONE
            }
        }
    }
}
