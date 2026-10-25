package it.gymlog.app.domain

import it.gymlog.app.data.db.BodyEntry
import it.gymlog.app.data.settings.Sex
import java.time.LocalDate
import kotlin.math.log10
import kotlin.math.pow

/**
 * Media mobile a [window] giorni sul peso: per ogni giorno considera le misurazioni dal giorno
 * precedente a quello indicato. Restituisce solo i giorni con almeno una misurazione.
 */
fun weightMovingAverage(entries: List<BodyEntry>, window: Int = 7): List<Pair<LocalDate, Double>> {
    val weights = entries.mapNotNull { e -> e.weightKg?.let { LocalDate.ofEpochDay(e.epochDay) to it } }
        .sortedBy { it.first }
    return weights.map { (date, _) ->
        val from = date.minusDays((window - 1).toLong())
        val inWindow = weights.filter { !it.first.isBefore(from) && !it.first.isAfter(date) }
        date to inWindow.map { it.second }.average()
    }
}

/**
 * Jackson-Pollock 7 pliche (mm) → percentuale di grasso con formula di Siri.
 * Null se manca anche una plica o l'età.
 */
fun bodyFatJackson7(entry: BodyEntry, age: Int, sex: Sex): Double? {
    val sum = listOf(
        entry.skinChest, entry.skinMidaxillary, entry.skinTriceps, entry.skinSubscapular,
        entry.skinAbdomen, entry.skinSuprailiac, entry.skinThigh,
    ).fold(0.0) { acc, v -> acc + (v ?: return null) }
    val density = when (sex) {
        Sex.MALE -> 1.112 - 0.00043499 * sum + 0.00000055 * sum.pow(2) - 0.00028826 * age
        Sex.FEMALE -> 1.097 - 0.00046971 * sum + 0.00000056 * sum.pow(2) - 0.00012828 * age
    }
    return siri(density)
}

/** Jackson-Pollock 3 pliche: petto-addome-coscia (uomo), tricipite-sovrailiaca-coscia (donna). */
fun bodyFatJackson3(entry: BodyEntry, age: Int, sex: Sex): Double? {
    val sum = when (sex) {
        Sex.MALE -> listOf(entry.skinChest, entry.skinAbdomen, entry.skinThigh)
        Sex.FEMALE -> listOf(entry.skinTriceps, entry.skinSuprailiac, entry.skinThigh)
    }.fold(0.0) { acc, v -> acc + (v ?: return null) }
    val density = when (sex) {
        Sex.MALE -> 1.10938 - 0.0008267 * sum + 0.0000016 * sum.pow(2) - 0.0002574 * age
        Sex.FEMALE -> 1.0994921 - 0.0009929 * sum + 0.0000023 * sum.pow(2) - 0.0001392 * age
    }
    return siri(density)
}

/** US Navy: circonferenze in cm. Null se mancano i dati necessari. */
fun bodyFatNavy(entry: BodyEntry, heightCm: Double?, sex: Sex): Double? {
    val height = heightCm?.takeIf { it > 0 } ?: return null
    val neck = entry.neckCm?.takeIf { it > 0 } ?: return null
    val waist = entry.waistCm?.takeIf { it > 0 } ?: return null
    val value = when (sex) {
        Sex.MALE -> {
            if (waist <= neck) return null
            495.0 / (1.0324 - 0.19077 * log10(waist - neck) + 0.15456 * log10(height)) - 450.0
        }
        Sex.FEMALE -> {
            val hips = entry.hipsCm?.takeIf { it > 0 } ?: return null
            if (waist + hips <= neck) return null
            495.0 / (1.29579 - 0.35004 * log10(waist + hips - neck) + 0.22100 * log10(height)) - 450.0
        }
    }
    return value.takeIf { it in 2.0..70.0 }
}

private fun siri(density: Double): Double? {
    if (density <= 0) return null
    val fat = 495.0 / density - 450.0
    return fat.takeIf { it in 2.0..70.0 }
}

/** FFMI = massa magra / altezza². Null se mancano altezza, peso o percentuale di grasso. */
fun ffmi(weightKg: Double?, heightCm: Double?, bodyFatPct: Double?): Double? {
    val weight = weightKg?.takeIf { it > 0 } ?: return null
    val height = heightCm?.takeIf { it > 0 }?.div(100.0) ?: return null
    val fat = bodyFatPct ?: return null
    return weight * (1.0 - fat / 100.0) / height.pow(2)
}

/** FFMI normalizzato a 1,80 m (utile per confrontare persone di altezze diverse). */
fun ffmiNormalized(weightKg: Double?, heightCm: Double?, bodyFatPct: Double?): Double? {
    val height = heightCm?.takeIf { it > 0 }?.div(100.0) ?: return null
    val base = ffmi(weightKg, heightCm, bodyFatPct) ?: return null
    return base + 6.1 * (1.8 - height)
}

/** Età in anni compiuti a [date] da [birthYear]. */
fun ageAt(birthYear: Int?, date: LocalDate): Int? =
    birthYear?.takeIf { it > 1900 }?.let { date.year - it }?.takeIf { it in 10..120 }

/** Tutte le stime disponibili per una misurazione, in ordine di preferenza. */
fun bodyFatEstimates(entry: BodyEntry, heightCm: Double?, birthYear: Int?, sex: Sex): Map<String, Double> {
    val age = ageAt(birthYear, LocalDate.ofEpochDay(entry.epochDay))
    val out = linkedMapOf<String, Double>()
    age?.let { bodyFatJackson7(entry, it, sex)?.let { v -> out["JP7"] = v } }
    age?.let { bodyFatJackson3(entry, it, sex)?.let { v -> out["JP3"] = v } }
    bodyFatNavy(entry, heightCm, sex)?.let { v -> out["Navy"] = v }
    return out
}
