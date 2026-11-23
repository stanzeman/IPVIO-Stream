package com.ipvio.stream

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** The read guide as the app uses it: shows per guide id, name matches, icons (T-053, T-055). */
class EpgData(
    val progs: Map<String, List<EpgEntry>>,
    val byTitle: Map<String, String>,
    val icons: Map<String, String>,
    val iconByTitle: Map<String, String>
) {
    companion object { val EMPTY = EpgData(emptyMap(), emptyMap(), emptyMap(), emptyMap()) }
}

/**
 * Stored copy of the read guide (T-059). Without Android, testable.
 *
 * The guide is read once and kept on the device with the time it was read and a signature of what it was read for
 * (addresses, channels). After a restart the app shows it at once instead of reading the XMLTV file again; it is
 * replaced only when a new read is complete, so shows and logos never disappear in between.
 */
object EpgSnapshot {
    private const val MAGIC = 0x45504753   // "EPGS"
    private const val VERSION = 1

    /** A stored guide is used for at most this long (the shows reach 36 hours ahead). */
    const val MAX_AGE_MS = 36 * 3_600_000L

    /** The guide is read again (in the background) when it is this old. */
    const val REFRESH_MS = 12 * 3_600_000L

    /** While the app runs, it checks this often whether the guide is due. */
    const val CHECK_EVERY_MS = 30 * 60_000L

    /** File name of the stored copy (in the app's files folder). */
    const val FILE = "epg_snapshot.bin"

    /** What a stored copy is valid for: the guide addresses, the channels by id and by name, catch-up days. */
    fun sig(urls: List<String>, wanted: Set<String>, nameKeys: Set<String>, past: Map<String, Int>): Long {
        var h = -0x340d631b7bdddcdbL   // FNV-1a 64
        fun add(s: String) { for (ch in s) { h = h xor ch.code.toLong(); h *= 0x100000001b3L }; h = h xor 0x1fL; h *= 0x100000001b3L }
        urls.map { it.trim() }.sorted().forEach { add("u$it") }
        wanted.sorted().forEach { add("w$it") }
        nameKeys.sorted().forEach { add("n$it") }
        past.entries.sortedBy { it.key }.forEach { add("p${it.key}=${it.value}") }
        return h
    }

    /** true = a copy read at [savedAt] for [savedSig] may be shown now. */
    fun usable(savedAt: Long, savedSig: Long, sig: Long, now: Long): Boolean = savedSig == sig && (now - savedAt) in 0 until MAX_AGE_MS

    /** true = read the guide again ([loadedAt] = when the shown guide was read, 0 = never). */
    fun refreshDue(loadedAt: Long, now: Long): Boolean = loadedAt <= 0 || (now - loadedAt) !in 0 until REFRESH_MS

    /** The new read replaces the shown guide unless it brought nothing while the shown one has shows (failed download). */
    fun replace(old: EpgData, new: EpgData, sameChannels: Boolean): Boolean = new.progs.isNotEmpty() || old.progs.isEmpty() || !sameChannels

    class Stored(val data: EpgData, val savedAt: Long, val sig: Long)

    fun write(out: OutputStream, d: EpgData, savedAt: Long, sig: Long) {
        DataOutputStream(GZIPOutputStream(out, 1 shl 16)).use { o ->
            o.writeInt(MAGIC); o.writeInt(VERSION); o.writeLong(savedAt); o.writeLong(sig)
            o.writeInt(d.progs.size)
            for ((id, list) in d.progs) {
                o.writeUTF(id); o.writeInt(list.size)
                for (e in list) { o.writeUTF(e.title.take(1000)); o.writeUTF(e.desc.take(1000)); o.writeLong(e.start); o.writeLong(e.end) }
            }
            for (m in listOf(d.byTitle, d.icons, d.iconByTitle)) {
                o.writeInt(m.size)
                for ((k, v) in m) { o.writeUTF(k); o.writeUTF(v) }
            }
        }
    }

    /** null = no valid copy (other format, damaged file). */
    fun read(ins: InputStream): Stored? = runCatching {
        DataInputStream(GZIPInputStream(ins, 1 shl 16)).use { i ->
            if (i.readInt() != MAGIC || i.readInt() != VERSION) return@use null
            val savedAt = i.readLong()
            val sig = i.readLong()
            val pool = StringPool()
            val progs = HashMap<String, List<EpgEntry>>()
            repeat(i.readInt()) {
                val id = i.readUTF()
                val n = i.readInt()
                val l = ArrayList<EpgEntry>(n)
                repeat(n) { l.add(EpgEntry(pool.of(i.readUTF()), i.readUTF(), i.readLong(), i.readLong())) }
                progs[id] = l
            }
            fun map(): Map<String, String> { val m = HashMap<String, String>(); repeat(i.readInt()) { m[i.readUTF()] = i.readUTF() }; return m }
            Stored(EpgData(progs, map(), map(), map()), savedAt, sig)
        }
    }.getOrNull()
}
