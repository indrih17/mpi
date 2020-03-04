@file:Suppress("NOTHING_TO_INLINE")

/** [IntArray.toString] нормального человека. */
inline fun IntArray.stringify(): String = toList().toString()

/** @return Ранк кому доставить сообщение. Может не быть, если отправитель последний (size нечётный). */
inline fun recipientRankOrNull(rank: Int, size: Int): Int? =
    (rank + 1).takeIf { it < size }

/** @return Ранк от кого получить сообщение. Может не быть, если получатель первый. */
inline fun senderRankOrNull(rank: Int): Int? =
    (rank - 1).takeIf { it >= 0 }
