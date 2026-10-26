package dev.seirei.core.roll.guarantee

import dev.seirei.core.model.ContextSnapshot
import dev.seirei.core.model.Party
import dev.seirei.core.model.PartyMember
import dev.seirei.core.model.TimeBand
import dev.seirei.core.model.WeatherKind
import dev.seirei.core.roll.ExpeditionResult
import dev.seirei.core.roll.RollConstants
import dev.seirei.core.roll.RollEngine
import dev.seirei.core.roll.resolveNamed
import java.io.File
import java.time.LocalDate
import org.junit.Assert.fail
import org.junit.Test

/**
 * Golden snapshots of the full engine output (design 02 §8).
 *
 * Fixed seeds × POC/DEMO constants × representative snapshots and parties are
 * rendered to a stable text form and compared against the files under
 * `src/test/resources/golden/`. Any unintentional change in the pipeline
 * (encounters, quells, awakenings, log, mana) breaks this test.
 *
 * Regeneration: run with the GOLDEN_UPDATE=1 environment variable (see
 * core/roll/AGENTS.md). Normal `check` runs never rewrite the files.
 */
class GoldenTest {

    @Test
    fun `固定シードの探索結果全体がゴールデンスナップショットと一致する`() {
        val rendered = EXPEDITION_CASES.joinToString("\n\n") { case -> render(case) }
        assertGolden("golden/expedition-results.txt", rendered)
    }

    @Test
    fun `固定シードの指名覚醒結果がゴールデンスナップショットと一致する`() {
        val rendered = NAMED_CASES.joinToString("\n\n") { (name, seed, motifId) ->
            val roll = resolveNamed(seed, motifId, RollConstants.POC)
            listOf(
                "## $name",
                "seed=$seed",
                "constants=POC",
                "motifId=$motifId",
                "roll=$roll",
            ).joinToString("\n")
        }
        assertGolden("golden/named-results.txt", rendered)
    }

    private fun render(case: ExpeditionCase): String {
        val result: ExpeditionResult = RollEngine.resolve(case.seed, case.snapshot, case.party, case.constants)
        return buildString {
            appendLine("## ${case.name}")
            appendLine("seed=${case.seed}")
            appendLine("constants=${case.constantsName}")
            appendLine("snapshot=${case.snapshot}")
            appendLine("party=${case.party.members}")
            appendLine("earlyReturn=${result.earlyReturn}")
            appendLine("manaGained=${result.manaGained}")
            appendLine("awakened=${result.awakened}")
            appendLine("log=")
            result.log.forEach { appendLine("  $it") }
        }.trimEnd()
    }

    private fun assertGolden(resourcePath: String, rendered: String) {
        if (System.getenv("GOLDEN_UPDATE") == "1") {
            val target = File(System.getProperty("user.dir"), "src/test/resources/$resourcePath")
            target.parentFile.mkdirs()
            target.writeText(rendered + "\n")
            return
        }
        val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (stream == null) {
            fail("Golden file $resourcePath is missing. Regenerate with GOLDEN_UPDATE=1.")
            return
        }
        val expected = stream.bufferedReader().readText().trimEnd()
        if (expected != rendered) {
            val expectedLines = expected.lines()
            val renderedLines = rendered.lines()
            val firstDiff = (0 until maxOf(expectedLines.size, renderedLines.size))
                .firstOrNull { expectedLines.getOrNull(it) != renderedLines.getOrNull(it) } ?: 0
            fail(
                "Golden snapshot mismatch in $resourcePath at line ${firstDiff + 1}:\n" +
                    "  expected: ${expectedLines.getOrNull(firstDiff)}\n" +
                    "  actual  : ${renderedLines.getOrNull(firstDiff)}\n" +
                    "If this change is intentional, regenerate with GOLDEN_UPDATE=1 and review the diff.",
            )
        }
    }

    private data class ExpeditionCase(
        val name: String,
        val seed: Long,
        val constantsName: String,
        val constants: RollConstants,
        val snapshot: ContextSnapshot,
        val party: Party,
    )

    companion object {
        private fun snapshot(date: String, band: TimeBand, weather: WeatherKind) =
            ContextSnapshot(LocalDate.parse(date), band, weather)

        private fun party(vararg members: Triple<Int, Int, Int>) =
            Party(members.map { (motifId, star, personalityId) -> PartyMember(motifId, star, personalityId) })

        /** One scenario per seed; each runs under both POC and DEMO constants. */
        private val SCENARIOS = listOf(
            Triple(1L, snapshot("2026-10-21", TimeBand.DAY, WeatherKind.SUNNY), party(Triple(1, 3, 0))),
            Triple(
                42L,
                snapshot("2026-10-25", TimeBand.NIGHT, WeatherKind.RAIN),
                party(Triple(4, 5, 3), Triple(3, 2, 7), Triple(7, 4, 11)),
            ),
            Triple(-1L, snapshot("2026-01-01", TimeBand.EVENING, WeatherKind.SNOW), party()),
            Triple(
                20261025L,
                snapshot("2026-08-15", TimeBand.DAY, WeatherKind.UNKNOWN),
                party(Triple(10, 1, 9), Triple(2, 2, 2)),
            ),
            Triple(
                987654321L,
                snapshot("2026-12-31", TimeBand.NIGHT, WeatherKind.CLOUDY),
                party(Triple(6, 4, 5), Triple(9, 3, 1)),
            ),
        )

        private val EXPEDITION_CASES: List<ExpeditionCase> = SCENARIOS.flatMap { (seed, snap, members) ->
            listOf(
                ExpeditionCase("poc_seed$seed", seed, "POC", RollConstants.POC, snap, members),
                ExpeditionCase("demo_seed$seed", seed, "DEMO", RollConstants.DEMO, snap, members),
            )
        }

        private val NAMED_CASES = listOf(
            Triple("named_seed7_motif1", 7L, 1),
            Triple("named_seed123456789_motif5", 123456789L, 5),
            Triple("named_seed-99_motif10", -99L, 10),
        )
    }
}
