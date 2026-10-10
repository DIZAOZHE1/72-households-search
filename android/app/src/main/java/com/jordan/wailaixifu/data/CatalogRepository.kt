package com.jordan.wailaixifu.data

import android.content.Context
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the offline catalogues packaged inside the APK.
 *
 * Layout under assets/, one dataset per site:
 *   <dataset>.tsv            section, first, last, label, title, raw
 *   <dataset>.sections.txt   one section heading per line
 *   <dataset>.detail.tsv     index, synopsis, characterIds, tagIds, storyRefs
 *   <dataset>.chars.tsv      index, name, category, aliases, description   (optional)
 *   <dataset>.tags.txt       one tag per line                              (optional)
 *   <dataset>.seasons.txt    one season title per line                     (optional)
 *   <dataset>.lines.tsv      index, section, name, summary, nodes          (optional)
 *
 * Nothing here touches the network. Parsing happens off the main thread and the result is cached.
 */
class CatalogRepository(private val context: Context) {

    private val cache = mutableMapOf<String, Catalog?>()

    /** Loads [dataset], or returns null when the data has not been bundled. */
    suspend fun load(dataset: String): Catalog? {
        cache[dataset]?.let { return it }
        return withContext(Dispatchers.IO) {
            cache[dataset] ?: readCatalog(dataset)?.also { cache[dataset] = it }
        }
    }

    private fun readCatalog(dataset: String): Catalog? {
        val cardLines = readLines("$dataset.tsv") ?: return null
        val sections = readLines("$dataset.sections.txt").orEmpty()
        val tags = readLines("$dataset.tags.txt").orEmpty()
        val characters = parseCharacters(readLines("$dataset.chars.tsv").orEmpty())
        val extras = parseExtras(readLines("$dataset.detail.tsv").orEmpty())
        val storylines = parseStorylines(readLines("$dataset.lines.tsv").orEmpty())

        val episodes = ArrayList<Episode>(cardLines.size)
        for ((row, line) in cardLines.withIndex()) {
            if (line.isBlank()) continue
            val f = line.split('\t')
            if (f.size < FIELD_COUNT) continue
            val start = f[1].toIntOrNull() ?: continue
            val extra = extras[f[0].toIntOrNull() ?: row]
            episodes += Episode(
                index = episodes.size,
                section = f[0].toIntOrNull() ?: 0,
                start = start,
                end = f[2].toIntOrNull() ?: start,
                episodeLabel = f[3],
                title = f[4],
                raw = f.getOrElse(5) { "" },
                synopsis = extra?.synopsis.orEmpty(),
                characterIds = extra?.characterIds.orEmpty(),
                tagIds = extra?.tagIds.orEmpty(),
                storyRefs = extra?.storyRefs.orEmpty(),
            )
        }
        if (episodes.isEmpty()) return null

        return Catalog(
            dataset = dataset,
            episodes = episodes,
            sections = sections,
            tags = tags,
            characters = characters,
            storylines = storylines,
        )
    }

    private class Extra(
        val synopsis: String,
        val characterIds: List<Int>,
        val tagIds: List<Int>,
        val storyRefs: List<StoryRef>,
    )

    private fun parseExtras(lines: List<String>): Map<Int, Extra> {
        val out = HashMap<Int, Extra>(lines.size)
        for (line in lines) {
            if (line.isBlank()) continue
            val f = line.split('\t')
            val index = f.getOrNull(0)?.toIntOrNull() ?: continue
            out[index] = Extra(
                synopsis = f.getOrElse(1) { "" },
                characterIds = intList(f.getOrNull(2)),
                tagIds = intList(f.getOrNull(3)),
                storyRefs = storyRefList(f.getOrNull(4)),
            )
        }
        return out
    }

    private fun parseCharacters(lines: List<String>): List<Character> =
        lines.filter { it.isNotBlank() }.mapIndexed { row, line ->
            val f = line.split('\t')
            Character(
                id = f.getOrNull(0)?.toIntOrNull() ?: row,
                name = f.getOrElse(1) { "" },
                category = f.getOrNull(2)?.toIntOrNull() ?: 0,
                aliases = f.getOrElse(3) { "" }.split('、').map { it.trim() }.filter { it.isNotEmpty() },
                description = f.getOrElse(4) { "" },
            )
        }

    private fun parseStorylines(lines: List<String>): List<Storyline> =
        lines.filter { it.isNotBlank() }.mapIndexed { row, line ->
            val f = line.split('\t')
            val nodes = f.getOrElse(4) { "" }.split(NODE_SEPARATOR).mapNotNull { raw ->
                if (raw.isEmpty()) return@mapNotNull null
                val parts = raw.split(FIELD_SEPARATOR)
                StoryNode(
                    group = parts.getOrElse(0) { "" },
                    text = parts.getOrElse(1) { "" },
                    episodeIndices = intList(parts.getOrElse(2) { "" }),
                )
            }
            Storyline(
                index = f.getOrNull(0)?.toIntOrNull() ?: row,
                section = f.getOrElse(1) { "" },
                name = f.getOrElse(2) { "" },
                summary = f.getOrElse(3) { "" },
                nodes = nodes,
            )
        }

    private fun intList(value: String?): List<Int> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(',').mapNotNull { it.trim().toIntOrNull() }
    }

    private fun storyRefList(value: String?): List<StoryRef> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(',').mapNotNull { part ->
            val bits = part.split(':')
            val line = bits.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val node = bits.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            StoryRef(line, node)
        }
    }

    /** Returns null when the asset is absent, so callers can distinguish "missing" from "empty". */
    private fun readLines(asset: String): List<String>? = try {
        context.assets.open(asset).use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readLines()
        }
    } catch (_: FileNotFoundException) {
        null
    } catch (_: java.io.IOException) {
        null
    }

    private companion object {
        const val FIELD_COUNT = 5
        const val NODE_SEPARATOR = '\u001e'
        const val FIELD_SEPARATOR = '\u001f'
    }
}
