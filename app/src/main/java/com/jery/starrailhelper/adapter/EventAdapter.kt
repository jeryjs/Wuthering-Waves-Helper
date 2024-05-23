package com.jery.starrailhelper.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jery.starrailhelper.R
import com.jery.starrailhelper.data.EventItem
import com.jery.starrailhelper.databinding.ItemEventBinding
import com.jery.starrailhelper.utils.Utils

class EventAdapter (
    private val events: MutableList<EventItem>
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {
    companion object {
        private const val VIEW_TYPE_CURRENT = 0
        private const val VIEW_TYPE_UPCOMING = 1
        private const val VIEW_TYPE_PERMANENT = 2
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemEventBinding.inflate(inflater, parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val eventItem = getItem(position)
        holder.bind(eventItem)
    }

    override fun getItemCount(): Int {
        return if (events.size>10) 10 else events.size
    }

    override fun getItemViewType(position: Int): Int {
        val eventItem = getItem(position)
        return when (eventItem.status) {
            "Current" -> VIEW_TYPE_CURRENT
            "Upcoming" -> VIEW_TYPE_UPCOMING
            "Permanent" -> VIEW_TYPE_PERMANENT
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    private fun getItem(position: Int): EventItem {
        val currentEvents = events.filter { it.status == "Current" }
        val upcomingEvents = events.filter { it.status == "Upcoming" }
        val permanentEvents = events.filter { it.status == "Permanent" }

        val currentItemCount = currentEvents.size
        val upcomingItemCount = upcomingEvents.size
        val permanentItemCount = permanentEvents.size

        return when (position) {
            in 0 until currentItemCount -> currentEvents[position]
            in currentItemCount until currentItemCount + upcomingItemCount -> upcomingEvents[position - currentItemCount]
            in currentItemCount + upcomingItemCount until currentItemCount + upcomingItemCount + permanentItemCount -> permanentEvents[position - currentItemCount - upcomingItemCount]
            else -> throw IndexOutOfBoundsException("Invalid position $position")
        }
    }

    inner class EventViewHolder(private val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(eventItem: EventItem) {
            val ctx = itemView.context
            with (binding) {
                this.eventItem = eventItem
                executePendingBindings()
                itemView.setOnLongClickListener { eventItem.isSeen = !eventItem.isSeen; notifyItemChanged(layoutPosition); false }
                itemView.setOnClickListener { showEventPage(eventItem.page, ctx) }
            }
            try {
                Glide.with(ctx)
                    .load(eventItem.image)
                    .placeholder(R.drawable.ic_timer)
                    .fitCenter()
                    .transition(
                        DrawableTransitionOptions.withCrossFade(
                            DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
                        )
                    )
                    .into(binding.ivBanner)
            } catch (e: Exception) {
                Utils.showStackTrace(ctx, e)
                e.printStackTrace()
            }
        }

        @SuppressLint("SetJavaScriptEnabled")
        private fun showEventPage(page: String, ctx: Context) {
            val bottomSheetDialog = BottomSheetDialog(ctx)
            val dialogView = LayoutInflater.from(ctx).inflate(R.layout.bottom_sheet_layout, null)
            bottomSheetDialog.setContentView(dialogView)
            val progress = dialogView.findViewById<ProgressBar>(R.id.progress)

            val wv = dialogView.findViewById<WebView>(R.id.webView)
            wv.visibility = View.VISIBLE
            wv.apply {
                background = ContextCompat.getDrawable(ctx, R.color.colorAccent)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        progress.visibility = View.VISIBLE
                    }
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        progress.visibility = View.GONE
                    }
                }
            }
            wv.loadUrl(page)
            bottomSheetDialog.show()
        }
    }
}