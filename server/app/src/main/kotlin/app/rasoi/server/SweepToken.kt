package app.rasoi.server

import java.security.MessageDigest

/** The shared secret an external scheduler sends to run the sweeps (ADR 0028). Never printed. */
class SweepToken(val value: String) {
    init {
        require(value.length >= MIN_LENGTH) { "RASOI_SWEEP_TOKEN must be at least $MIN_LENGTH characters" }
    }

    fun matches(candidate: String?): Boolean =
        candidate != null && MessageDigest.isEqual(candidate.toByteArray(), value.toByteArray())

    override fun toString() = "SweepToken(***)"

    companion object {
        const val HEADER = "X-Rasoi-Sweep-Token"
        private const val MIN_LENGTH = 32
    }
}
