package fr.augustine.androgustinecopilote.data

data class CopilotInstructions(
    val pilotPaceInstruction: String,
    val raceStatusInstruction: String,
    val pitStopRequest: Boolean,
    val updatedAtIso: String,
    val updatedBy: String = UPDATED_BY_COPILOT,
) {
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            FIELD_PILOT_PACE_INSTRUCTION to pilotPaceInstruction,
            FIELD_RACE_STATUS_INSTRUCTION to raceStatusInstruction,
            FIELD_PIT_STOP_REQUEST to pitStopRequest,
            FIELD_UPDATED_AT_ISO to updatedAtIso,
            FIELD_UPDATED_BY to updatedBy,
        )
    }

    companion object {
        const val PACE_ACCELERATE = "ACCELERATE"
        const val PACE_MAINTAIN = "MAINTAIN"
        const val PACE_SLOW_DOWN = "SLOW_DOWN"

        const val STATUS_RACE = "RACE"
        const val STATUS_NO_OVERTAKING = "NO_OVERTAKING"
        const val STATUS_STOP = "STOP"

        const val UPDATED_BY_COPILOT = "COPILOT"

        private const val FIELD_PILOT_PACE_INSTRUCTION = "pilotPaceInstruction"
        private const val FIELD_RACE_STATUS_INSTRUCTION = "raceStatusInstruction"
        private const val FIELD_PIT_STOP_REQUEST = "pitStopRequest"
        private const val FIELD_UPDATED_AT_ISO = "updatedAtIso"
        private const val FIELD_UPDATED_BY = "updatedBy"
    }
}
