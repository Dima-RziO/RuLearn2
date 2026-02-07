package ru.dimarzio.rulearn2.routes

sealed class SessionRoutes(val route: String) {
    data object NewWord : SessionRoutes("new_word")
    data object GuessingTest : SessionRoutes("guessing_test")
    data object TypingTest : SessionRoutes("typing_test")
    data object Word : SessionRoutes("word/{id}")
}