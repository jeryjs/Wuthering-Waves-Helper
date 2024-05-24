package com.jery.wuwahelper.adapter

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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.jery.wuwahelper.R
import com.jery.wuwahelper.data.CodeItem
import com.jery.wuwahelper.databinding.BottomSheetLayoutBinding
import com.jery.wuwahelper.databinding.ItemCodeBinding
import com.jery.wuwahelper.databinding.ItemRewardBinding
import com.jery.wuwahelper.utils.Utils

class CodeAdapter(
    private val codes: MutableList<CodeItem>
) : RecyclerView.Adapter<CodeAdapter.CodeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCodeBinding.inflate(inflater, parent, false)
        return CodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CodeViewHolder, position: Int) {
        val codeItem = getCodeItem(position)
        holder.bind(codeItem)
    }

    override fun getItemCount(): Int {
        return if (codes.size>10) 10 else codes.size
    }

    private fun getCodeItem(position: Int): CodeItem {
        return codes[position]
    }

    inner class CodeViewHolder(private val binding: ItemCodeBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(codeItem: CodeItem) {
            val ctx = itemView.context
            with (binding) {
                this.codeItem = codeItem
                this.utils = Utils
                executePendingBindings()
                itemView.setOnClickListener { onItemClick(codeItem, ctx) }
                ivIcon.setOnLongClickListener { codeItem.isRedeemed = (!codeItem.isRedeemed); notifyItemChanged(layoutPosition); false }
//                ivRedeem.setOnClickListener { redeem(codeItem.code, ctx); codeItem.isRedeemed = true; notifyItemChanged(layoutPosition) }
                ivRedeem.isEnabled = false
            }
            try {
                Glide.with(ctx)
                    .load(codeItem.rewards.first().imageURL!!)
                    .placeholder(R.drawable.ic_timer)
                    .fitCenter()
                    .transition(withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()))
                    .into(binding.ivIcon)
            } catch (e: Exception) {
//                Utils.showStackTrace(ctx, e)
//                e.printStackTrace()
            }
        }

        @SuppressLint("SetTextI18n")
        private fun onItemClick(codeItem: CodeItem, ctx: Context) {
            val bottomSheetDialog = BottomSheetDialog(ctx)
            val dialogBinding = BottomSheetLayoutBinding.inflate(LayoutInflater.from(ctx), null, false)
            bottomSheetDialog.setContentView(dialogBinding.root)

            dialogBinding.rewards.visibility = View.VISIBLE
            val rewardsLayout = dialogBinding.rewardsLayout

            rewardsLayout.removeAllViews()
            for (reward in codeItem.rewards) {
                val rewardBinding = ItemRewardBinding.inflate(LayoutInflater.from(ctx), rewardsLayout, false)
                rewardsLayout.addView(rewardBinding.root)

                rewardBinding.rewardQuantity.text = "x" + reward.quantity.toString()
                try {
                    Glide.with(ctx)
                        .load(reward.imageURL)
                        .placeholder(R.drawable.ic_timer)
                        .fitCenter()
                        .transition( withCrossFade(DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()) )
                        .into(rewardBinding.rewardImage)
                } catch (e: Exception) {
//                    e.printStackTrace()
                    e.printStackTrace()
                }
            }

            dialogBinding.rCode.text = codeItem.code
            dialogBinding.rCode.setOnClickListener { Utils.copyToClipboard(codeItem.code, itemView.rootView) }
            dialogBinding.rStatus.text = codeItem.duration.first +" / "+ codeItem.duration.second
            dialogBinding.redeemBtn.text = if (codeItem.isRedeemed) "Redeem once again" else "Redeem Code"
//            dialogBinding.redeemBtn.setOnClickListener { redeem(codeItem.code, ctx); codeItem.isRedeemed = true; notifyItemChanged(layoutPosition) }
            dialogBinding.redeemBtn.isEnabled = false
            bottomSheetDialog.show()
        }

        @SuppressLint("InflateParams", "SetJavaScriptEnabled")
        private fun redeem(code: String, ctx: Context) {
            Utils.copyToClipboard(code, itemView.rootView)

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
                        wv.evaluateJavascript(script(code), null)
                        progress.visibility = View.GONE
                    }
                }
            }
            wv.loadUrl("https://hsr.hoyoverse.com/gift?code=$code")
            bottomSheetDialog.show()
        }
        private fun script(code: String) = """
            function pollStart() {
                const element = document.querySelector("span.mihoyo-account-role__nickname");
                if (document.querySelector("span.mihoyo-account-role__nickname")) {
                    const server = document.querySelector("div.web-cdkey-form__select--toggle");
                    server.click();
                    function pollServer() {
                        if (server.textContent.trim() != "Select a server") {
                            const codeInput = document.querySelector("input#web_cdkey_code");
                            function pollCode() {
                                if (!codeInput.disabled) codeInput.value = "$code";
                                else setTimeout(pollCode, 500);
                            }
                            pollCode();
                        } else setTimeout(pollServer, 500)
                    }
                    server.addEventListener("click", pollServer);
                    pollServer();
                } else setTimeout(pollStart, 500);
            }
            pollStart();
        """.trimIndent()
    }
}
