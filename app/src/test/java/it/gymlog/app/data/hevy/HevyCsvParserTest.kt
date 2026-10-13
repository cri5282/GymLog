package it.gymlog.app.data.hevy

import it.gymlog.app.data.db.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.StringReader
import java.time.LocalDateTime
import java.time.ZoneId

class HevyCsvParserTest {
    private val header =
        "\"title\",\"start_time\",\"end_time\",\"description\",\"exercise_title\",\"superset_id\",\"exercise_notes\"," +
            "\"set_index\",\"set_type\",\"weight_kg\",\"reps\",\"distance_km\",\"duration_seconds\",\"rpe\"\n"

    private fun parse(vararg rows: String) = HevyCsvParser.parse(StringReader(header + rows.joinToString("\n")))

    private fun millis(y: Int, m: Int, d: Int, h: Int, min: Int) =
        LocalDateTime.of(y, m, d, h, min).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun parsesStandardRow() {
        val r = parse("\"Push\",\"12 ott 2026, 07:57\",\"12 ott 2026, 09:22\",\"\",\"Chest Fly (Machine)\",,\"\",0,\"normal\",25,5,,,8").rows.single()
        assertEquals("Push", r.title)
        assertEquals(millis(2026, 10, 12, 7, 57), r.startTime)
        assertEquals(millis(2026, 10, 12, 9, 22), r.endTime)
        assertEquals("Chest Fly (Machine)", r.exerciseTitle)
        assertEquals(SetType.WORKING, r.setType)
        assertEquals(25.0, r.weightKg, 1e-9)
        assertEquals(5, r.reps)
        assertEquals(2, r.rir)
        assertEquals(2, r.line)
    }

    @Test fun handlesQuotedCommas() {
        val r = parse("\"Pull\",\"10 ott 2026, 08:05\",\"\",\"\",\"Row\",,\"lento, controllato \"\"bene\"\"\",0,\"normal\",50,8,,,").rows.single()
        assertEquals("lento, controllato \"bene\"", r.notes)
        assertNull(r.endTime)
    }

    @Test fun mapsSetTypes() {
        val types = listOf("normal", "warmup", "dropset", "failure").map {
            parse("\"P\",\"1 gen 2026, 10:00\",\"\",\"\",\"E\",,\"\",0,\"$it\",10,5,,,").rows.single().setType
        }
        assertEquals(listOf(SetType.WORKING, SetType.WARMUP, SetType.DROP, SetType.FAILURE), types)
    }

    @Test fun rpeToRir_clampedAndNullable() {
        fun rir(rpe: String) = parse("\"P\",\"1 gen 2026, 10:00\",\"\",\"\",\"E\",,\"\",0,\"normal\",10,5,,,$rpe").rows.single().rir
        assertEquals(0, rir("10"))
        assertEquals(5, rir("4"))
        assertNull(rir(""))
        assertEquals(2, rir("8.5"))
        assertEquals(1, rir("9"))
    }

    @Test fun parsesItalianLocaleDecimalsAndDates() {
        val it = parse("\"P\",\"12 ott 2026, 18:30\",\"\",\"\",\"E\",,\"\",0,\"normal\",\"82,5\",5,,,").rows.single()
        val en = parse("\"P\",\"12 Oct 2026, 18:30\",\"\",\"\",\"E\",,\"\",0,\"normal\",82.5,5,,,").rows.single()
        val iso = parse("\"P\",\"2026-10-12T18:30:00\",\"\",\"\",\"E\",,\"\",0,\"normal\",82.5,5,,,").rows.single()
        assertEquals(82.5, it.weightKg, 1e-9)
        assertEquals(millis(2026, 10, 12, 18, 30), it.startTime)
        assertEquals(it.startTime, en.startTime)
        assertEquals(it.startTime, iso.startTime)
    }

    @Test fun missingWeightWithReps_isZeroKg() {
        val r = parse("\"P\",\"1 gen 2026, 10:00\",\"\",\"\",\"Crunch (Machine)\",,\"\",0,\"normal\",,12,,,").rows.single()
        assertEquals(0.0, r.weightKg, 1e-9)
        assertEquals(12, r.reps)
    }

    @Test fun skipsRowsWithoutWeightAndReps_andReportsInvalidLines() {
        val res = parse(
            "\"P\",\"1 gen 2026, 10:00\",\"\",\"\",\"Corsa\",,\"\",0,\"normal\",,,5.0,1800,",
            "\"P\",\"1 gen 2026, 10:00\",\"\",\"\",\"E\",,\"\",0,\"normal\",10,abc,,,",
            "\"P\",\"1 gen 2026, 10:00\",\"\",\"\",\"E\",,\"\",1,\"normal\",10,5,,,",
        )
        assertEquals(1, res.rows.size)
        assertEquals(listOf(3), res.invalidLines)
    }

    @Test(expected = HevyFormatException::class)
    fun missingColumns_throwsHevyFormatException() {
        HevyCsvParser.parse(StringReader("\"title\",\"reps\"\n\"P\",5"))
    }

    @Test fun parsesSampleFixture() {
        val res = HevyCsvParser.parse(javaClass.getResourceAsStream("/hevy_sample.csv")!!.reader())
        assertEquals(7, res.rows.size)
        assertEquals(emptyList<Int>(), res.invalidLines)
    }
}
