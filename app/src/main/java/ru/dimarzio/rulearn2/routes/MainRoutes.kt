package ru.dimarzio.rulearn2.routes

sealed class MainRoutes(val route: String) {
    data object Courses : MainRoutes("courses")
    data object Course : MainRoutes("course/{course}")
    data object Level : MainRoutes("level/{level}")
    data object Word : MainRoutes("word/{id}/{level}")
    data object GuessingReview : MainRoutes("guessing_review?level={level}")
    data object TypingReview : MainRoutes("typing_review?level={level}")
    data object LearnNewWords : MainRoutes("learn_new_words?level={level}")
    data object DifficultWords : MainRoutes("difficult_words?level={level}")
    data object Settings : MainRoutes("settings")
}