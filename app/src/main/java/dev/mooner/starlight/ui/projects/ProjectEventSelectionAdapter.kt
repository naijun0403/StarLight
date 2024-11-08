/*
 * ProjectEventSelectionAdapter.kt created by Minki Moon(mooner1022) on 12/18/23, 9:23 PM
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the GNU General Public License v3.0.
 */

package dev.mooner.starlight.ui.projects

import android.annotation.SuppressLint
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.toSpannable
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.load
import coil.size.Scale
import coil.transform.RoundedCornersTransformation
import dev.mooner.starlight.R
import dev.mooner.starlight.databinding.CardProjectEventSelectionBinding
import dev.mooner.starlight.plugincore.Session
import dev.mooner.starlight.plugincore.project.event.ProjectEvent
import dev.mooner.starlight.plugincore.project.event.ProjectEventManager
import dev.mooner.starlight.plugincore.utils.color
import dev.mooner.starlight.utils.dp
import java.io.File

class ProjectEventSelectionAdapter(
    private val events: List<EventData>
) : RecyclerView.Adapter<ProjectEventSelectionAdapter.EventSelectionViewAdapter>() {

    private val iconCache: MutableMap<String, File> = hashMapOf()
    private var radCache: Float? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventSelectionViewAdapter {
        val binding = CardProjectEventSelectionBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return EventSelectionViewAdapter(binding)
    }

    override fun getItemCount(): Int {
        return events.size
    }

    fun getSelectedEventIds(): Map<String, ProjectEvent> {
        return events
            .filter(EventData::isSelected)
            .associate { it.id to it.instance }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EventSelectionViewAdapter, position: Int) {
        val data = events[position]

        val iconFile = if (data.pluginId == "starlight") {
            R.drawable.ic_logo_transparent
        } else {
            iconCache[data.pluginId]
                ?: Session.pluginManager.getPluginById(data.pluginId)!!.getAsset("icon.png")
                    .let { file ->
                        if (file.exists() && file.isFile) {
                            iconCache[data.pluginId] = file
                            file
                        } else
                            R.drawable.ic_round_plugins_24
                    }
        }

        with(holder.binding) {
            val context = root.context

            cardWrapper.setOnClickListener {
                data.isSelected = !data.isSelected
                notifyItemChanged(position)
            }

            if (data.isSelected) {
                cardWrapper.strokeWidth = dp(2)
                cardWrapper.strokeColor = context.getColor(R.color.main_bright)
                cardWrapper.setCardBackgroundColor(color { "#50AD977A" })
            } else {
                cardWrapper.strokeWidth = 0
                cardWrapper.setCardBackgroundColor(context.getColor(dev.mooner.configdsl.R.color.background_selection_card))
            }

            ivIcon.load(iconFile) {
                scale(Scale.FIT)
                val radius =
                    if (radCache == null)
                        root.context.resources.getDimension(R.dimen.lang_icon_corner_radius)
                            .also { radCache = it }
                    else
                        radCache!!
                transformations(
                    RoundedCornersTransformation(radius)
                )
            }
            tvPluginId.text = data.pluginId
            tvEventId.text = "${data.functionName}(${data.id})".toSpannable().also { span ->
                span.setSpan(
                    ForegroundColorSpan(context.getColor(R.color.text_sub)),
                    data.functionName.length,
                    span.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    data class EventData(
        val pluginId: String,
        val id: String,
        val functionName: String,
        val instance: ProjectEvent,
    ) {
        var isSelected: Boolean = false

        companion object {
            fun fromEvent(actualId: String, event: ProjectEvent): EventData {
                return EventData(
                    pluginId = actualId.split(ProjectEventManager.PATH_DELIMITER)[0],
                    id = actualId,
                    functionName = event.functionName,
                    instance = event,
                )
            }
        }
    }

    inner class EventSelectionViewAdapter(
        internal val binding: CardProjectEventSelectionBinding
    ): ViewHolder(binding.root)
}