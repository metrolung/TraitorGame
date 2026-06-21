package io.github.metrolung.traitorgame

import java.util.UUID


sealed interface Vote {
    @JvmRecord
    data class PlayerVote(val player: SessionPlayer) : Vote
    object Skip : Vote
    object EndGame : Vote
}