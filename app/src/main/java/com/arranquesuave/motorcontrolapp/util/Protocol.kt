// app/src/main/java/com/arranquesuave/motorcontrolapp/util/Protocol.kt
package com.arranquesuave.motorcontrolapp.util

object Protocol {
    private const val ARR6P   = 0xA0.toByte()
    private const val PARO    = 0xC0.toByte()
    private const val CONT_B  = 0xB0.toByte()
    private const val VEL_B   = 0xD0.toByte()

    // Mantén antiguos comandos si los necesitas
    fun encodeArranque6P(): ByteArray = byteArrayOf(ARR6P)
    /** Codifica un paso: valor + letra a-f */
    fun encodeStep(index: Int, v: Int): ByteArray = "${v.coerceIn(0,254)}${('a'+index)},".toByteArray(Charsets.UTF_8)
    /** Codifica paro en binario: PARO */
    /** Codifica paro ASCII: "0p" */
    fun encodeParo(): ByteArray = "0p".toByteArray(Charsets.UTF_8)


    /** Codifica arranque suave ASCII: "v0a,v1b,...,v5f" */
    fun encodeArranqueSuave(values: List<Int>): ByteArray {
        // Mapea cada valor con su sufijo a-f
        val parts = values.take(6).mapIndexed { index, v ->
            // 'a'+index genera 'a','b',... 'f'
            "${v.coerceIn(0,254)}${('a' + index)}"
        }
        return parts.joinToString(",").toByteArray(Charsets.UTF_8)
    }

    /** Codifica arranque continuo ASCII: "0i," */
    fun encodeStartRamp(): ByteArray = "0i".toByteArray(Charsets.UTF_8)

    fun decodeSpeed(data: ByteArray): Int? {
        // Decode binary speed packet: first byte = VEL_B + value
        val b = data.firstOrNull() ?: return null
        val diff = (b - VEL_B).toInt()
        return diff.takeIf { it in 0..254 }
    }

}
