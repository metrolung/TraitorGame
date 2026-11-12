package io.github.metrolung.traitorgame

data class GameResult(
    val winners: Set<SessionPlayer>,
    val losers: Set<SessionPlayer>,
    val reason: EndGameReason,
)