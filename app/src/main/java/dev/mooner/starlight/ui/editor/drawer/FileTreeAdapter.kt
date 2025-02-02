package dev.mooner.starlight.ui.editor.drawer

import android.content.Context
import android.widget.Toast
import coil.transform.RoundedCornersTransformation
import dev.mooner.starlight.R
import dev.mooner.starlight.plugincore.translation.Locale
import dev.mooner.starlight.plugincore.translation.translate
import dev.mooner.starlight.ui.editor.DefaultEditorActivity
import dev.mooner.starlight.ui.tree.Node
import dev.mooner.starlight.ui.tree.TreeAdapter
import dev.mooner.starlight.utils.dp
import dev.mooner.starlight.utils.getLanguageByExtension
import dev.mooner.starlight.utils.loadWithTint
import dev.mooner.starlight.utils.showConfirmDialog
import java.io.File

typealias OnFileSelectedListener = (file: File) -> Unit
typealias FileNode = Node<File>

class FileTreeAdapter(
    context: Context,
    val root: File,
    private val lockedFiles: Set<String>,
    val listener: OnFileSelectedListener
): TreeAdapter<File>(context, walkAndMap(root)) {

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val node = displayedNodes[position]
        val file = node.content

        val icon = when(getLanguageByExtension(file.extension)) {
            DefaultEditorActivity.Language.JAVASCRIPT -> R.drawable.ic_js
            DefaultEditorActivity.Language.PYTHON     -> R.drawable.ic_python
            else -> {
                if (file.isDirectory)
                    dev.mooner.configdsl.R.drawable.ic_round_folder_24
                else
                    null
            }
        }

        icon?.let { holder.icon.loadWithTint(it, null) {
            transformations(RoundedCornersTransformation(dp(4).toFloat()))
        } }
            ?: holder.icon.loadWithTint(R.drawable.ic_round_code_24, R.color.main_bright)

        holder.name.text = file.name

        if (node.isLeaf && node.content.isFile)
            holder.root.setOnClickListener {
                listener(node.content)
            }

        holder.root.setOnLongClickListener {
            if (file.name in lockedFiles)
                Toast.makeText(holder.root.context, "삭제할 수 없는 파일이에요.", Toast.LENGTH_SHORT).show()
            else
                showDeleteConfirmDialog(it.context, file)

            true
        }
    }

    fun requestUpdate() {
        super.recreateWith(walkAndMap(root))
    }

    private fun showDeleteConfirmDialog(context: Context, file: File) {
        showConfirmDialog(
            context = context,
            title = translate {
                Locale.ENGLISH { "⚠️ Delete file [${file.name}]?" }
                Locale.KOREAN  { "⚠️ 파일 [${file.name}]을(를) 삭제할까요?" }
            },
            message = translate {
                Locale.ENGLISH { "After deletion, it cannot be reversed.\nAre you sure you want to delete this file?" }
                Locale.KOREAN  { "삭제 후에는 되돌릴 수 없어용.\n정말 이 파일을 삭제할까요?" }
            },
            onDismiss = { confirm ->
                if (confirm)
                    file.deleteRecursively()
                requestUpdate()
            }
        )
    }

    companion object {
        private fun walkAndMap(root: File): List<FileNode> {
            val result: MutableList<FileNode> = arrayListOf()

            root.walk().maxDepth(1).drop(1).forEach {
                val node = FileNode(it)
                //println(it.path)
                if (it.isDirectory)
                    node.addChild(*walkAndMap(it).toTypedArray())
                result += node
            }
            return result
        }
    }
}