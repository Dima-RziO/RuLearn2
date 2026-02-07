package ru.dimarzio.rulearn2.utils

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

// Usable both with nullable and non-nullable arguments.
fun NavController.navigate(route: String, vararg arguments: Pair<String, *>) {
    var result = route
    arguments.forEach { (key, value) ->
        result = result.replace("{$key}", value.toString())
    }
    navigate(result)
}

fun NavController.navigateCleaning(route: String, vararg arguments: Pair<String, *>) {
    var result = route
    arguments.forEach { (key, value) ->
        result = result.replace("{$key}", value.toString())
    }
    navigate(result) {
        popUpTo(currentBackStackEntry?.destination?.id ?: graph.findStartDestination().id) {
            inclusive = true
        }
    }
}