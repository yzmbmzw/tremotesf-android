/*
 * Copyright (C) 2017-2019 Alexey Rochev <equeim@gmail.com>
 *
 * This file is part of Tremotesf.
 *
 * Tremotesf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tremotesf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.equeim.tremotesf.torrentpropertiesfragment

import java.util.Comparator

import android.app.Dialog
import android.os.Bundle

import android.text.InputType
import android.text.format.DateUtils

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.View

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import org.equeim.libtremotesf.Tracker
import org.equeim.tremotesf.IntSelector
import org.equeim.tremotesf.NavigationDialogFragment
import org.equeim.tremotesf.R
import org.equeim.tremotesf.Selector
import org.equeim.tremotesf.utils.AlphanumericComparator
import org.equeim.tremotesf.utils.createTextFieldDialog

import kotlinx.android.synthetic.main.text_field_dialog.*
import kotlinx.android.synthetic.main.tracker_list_item.view.*
import org.equeim.tremotesf.Torrent


class TrackersAdapterItem(rpcTracker: Tracker) {
    val id = rpcTracker.id()
    val announce: String = rpcTracker.announce()

    var status = 0
    lateinit var errorMessage: String
    var peers = 0
    var nextUpdate = 0

    init {
        update(rpcTracker)
    }

    fun update(rpcTracker: Tracker) {
        status = rpcTracker.status()
        errorMessage = rpcTracker.errorMessage()
        peers = rpcTracker.peers()
        nextUpdate = rpcTracker.nextUpdate()
    }
}

class TrackersAdapter(private val torrentPropertiesFragment: TorrentPropertiesFragment) : RecyclerView.Adapter<TrackersAdapter.ViewHolder>() {
    private var torrent: Torrent? = null
    private val trackers = mutableListOf<TrackersAdapterItem>()
    private val comparator = object : Comparator<TrackersAdapterItem> {
        private val stringComparator = AlphanumericComparator()
        override fun compare(o1: TrackersAdapterItem, o2: TrackersAdapterItem) = stringComparator.compare(o1.announce,
                                                                                                          o2.announce)
    }

    private val context = torrentPropertiesFragment.requireContext()

    val selector = IntSelector(torrentPropertiesFragment.requireActivity() as AppCompatActivity,
                               ActionModeCallback(),
                               this,
                               trackers,
                               TrackersAdapterItem::id,
                               R.plurals.trackers_selected)

    override fun getItemCount(): Int {
        return trackers.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(selector,
                          LayoutInflater.from(parent.context).inflate(R.layout.tracker_list_item,
                                                                      parent,
                                                                      false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tracker = trackers[position]

        holder.item = tracker
        holder.nameTextView.text = tracker.announce
        holder.statusTextView.text = when (tracker.status) {
            Tracker.Status.Inactive -> context.getString(R.string.tracker_inactive)
            Tracker.Status.Active -> context.getString(R.string.tracker_active)
            Tracker.Status.Queued -> context.getString(R.string.tracker_queued)
            Tracker.Status.Updating -> context.getString(R.string.tracker_updating)
            else -> {
                if (tracker.errorMessage.isEmpty()) {
                    context.getString(R.string.error)
                } else {
                    context.getString(R.string.tracker_error, tracker.errorMessage)
                }
            }
        }

        if (tracker.status == Tracker.Status.Error) {
            holder.peersTextView.visibility = View.GONE
        } else {
            holder.peersTextView.text = context.resources.getQuantityString(R.plurals.peers_plural,
                                                                             tracker.peers,
                                                                             tracker.peers)
            holder.peersTextView.visibility = View.VISIBLE
        }

        if (tracker.nextUpdate < 0) {
            holder.nextUpdateTextView.visibility = View.GONE
        } else {
            holder.nextUpdateTextView.text = context.getString(R.string.next_update,
                                                                DateUtils.formatElapsedTime(tracker.nextUpdate.toLong()))
            holder.nextUpdateTextView.visibility = View.VISIBLE
        }

        holder.updateSelectedBackground()
    }

    fun update() {
        val torrent = torrentPropertiesFragment.torrent

        if (torrent == null) {
            if (this.torrent == null) {
                return
            }
            this.torrent = null
            val count = itemCount
            trackers.clear()
            notifyItemRangeRemoved(0, count)
            selector.actionMode?.finish()
            return
        }

        this.torrent = torrent

        val rpcTrackers = torrent.trackers
        val newTrackers = mutableListOf<TrackersAdapterItem>()
        for (rpcTracker: Tracker in rpcTrackers) {
            val id = rpcTracker.id()
            var tracker = trackers.find { it.id == id }
            if (tracker == null) {
                tracker = TrackersAdapterItem(rpcTracker)
            } else {
                tracker.update(rpcTracker)
            }
            newTrackers.add(tracker)
        }

        run {
            var i = 0
            while (i < trackers.size) {
                if (newTrackers.contains(trackers[i])) {
                    i++
                } else {
                    trackers.removeAt(i)
                    notifyItemRemoved(i)
                }
            }
        }

        for ((i, tracker) in newTrackers.sortedWith(comparator).withIndex()) {
            if (trackers.getOrNull(i) === tracker) {
                notifyItemChanged(i)
            } else {
                val index = trackers.indexOf(tracker)
                if (index == -1) {
                    trackers.add(i, tracker)
                    notifyItemInserted(i)
                } else {
                    trackers.removeAt(index)
                    trackers.add(i, tracker)
                    notifyItemMoved(index, i)
                    notifyItemChanged(i)
                }
            }
        }
    }

    inner class ViewHolder(selector: Selector<TrackersAdapterItem, Int>,
                           itemView: View) : Selector.ViewHolder<TrackersAdapterItem>(selector, itemView) {
        override lateinit var item: TrackersAdapterItem
        val nameTextView = itemView.name_text_view!!
        val statusTextView = itemView.status_text_view!!
        val peersTextView = itemView.peers_text_view!!
        val nextUpdateTextView = itemView.next_update_text_view!!

        override fun onClick(view: View) {
            if (selector.actionMode == null) {
                torrentPropertiesFragment.navController.navigate(R.id.action_torrentPropertiesFragment_to_editTrackerDialogFragment,
                                                                 bundleOf(EditTrackerDialogFragment.TRACKER_ID to item.id,
                                                                 EditTrackerDialogFragment.ANNOUNCE to item.announce))
            } else {
                super.onClick(view)
            }
        }
    }

    class EditTrackerDialogFragment : NavigationDialogFragment() {
        companion object {
            const val TRACKER_ID = "trackerId"
            const val ANNOUNCE = "announce"
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val trackerId = requireArguments().getInt(TRACKER_ID)

            return createTextFieldDialog(requireContext(),
                                         if (trackerId == -1) R.string.add_tracker else R.string.edit_tracker,
                                         null,
                                         null,
                                         getString(R.string.tracker_announce_url),
                                         InputType.TYPE_TEXT_VARIATION_URI,
                                         requireArguments().getString(ANNOUNCE),
                                         null) {
                val torrent = (parentFragmentManager.primaryNavigationFragment as? TorrentPropertiesFragment)?.torrent
                val textField = requireDialog().text_field!!
                if (trackerId == -1) {
                    torrent?.addTracker(textField.text.toString())
                } else {
                    torrent?.setTracker(trackerId, textField.text.toString())
                }
            }
        }
    }

    private inner class ActionModeCallback : Selector.ActionModeCallback<TrackersAdapterItem>() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.trackers_context_menu, menu)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            if (super.onActionItemClicked(mode, item)) {
                return true
            }

            if (item.itemId == R.id.remove) {
                torrentPropertiesFragment.navController
                        .navigate(R.id.action_torrentPropertiesFragment_to_removeTrackerDialogFragment,
                                  bundleOf(RemoveTrackerDialogFragment.IDS to selector.selectedItems.map(TrackersAdapterItem::id).toIntArray()))
                return true
            }

            return false
        }
    }

    class RemoveTrackerDialogFragment : NavigationDialogFragment() {
        companion object {
            const val IDS = "ids"
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val ids = requireArguments().getIntArray(IDS)!!
            return MaterialAlertDialogBuilder(requireContext())
                    .setMessage(resources.getQuantityString(R.plurals.remove_trackers_message,
                                                            ids.size,
                                                            ids.size))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.remove) { _, _ ->
                        (parentFragmentManager.primaryNavigationFragment as? TorrentPropertiesFragment)?.torrent?.removeTrackers(ids)
                        requiredActivity.actionMode?.finish()
                    }
                    .create()
        }
    }
}
