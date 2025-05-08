package com.droidhen.formalautosim.core.entities.machines

import androidx.compose.runtime.Composable
import com.droidhen.formalautosim.core.entities.states.State
import com.droidhen.formalautosim.core.entities.transitions.Transition

class FiniteMachine(name: String = "Untitled") : Machine(name) {

    override var currentState: Int? = null


    @Composable
    override fun calculateTransition(onAnimationEnd: () -> Unit) {
        if (currentState == null) return
        val startState = getStateByIndex(currentState!!)
        val appropriateTransition = getListOfAppropriateTransitions(startState)
        if (appropriateTransition.isEmpty()) return
        val endState = getStateByIndex(appropriateTransition[0].endState)
        val newInputValue = input.removePrefix(appropriateTransition[0].name).toString()
        input.clear()
        input.append(newInputValue)

        AnimationOfTransition(
            start = startState.position,
            end = endState.position,
            radius = startState.radius,
            duration = 3000,
            onAnimationEnd = {
                startState.isCurrent = false
                endState.isCurrent = true
                currentState = endState.index
                onAnimationEnd()
            }
        )
    }


    override fun convertMachineToKeyValue(): List<Pair<String, String>> {
        TODO("not yet")
    }

    override fun addNewState(state: State) {
        if (state.initial && currentState == null) {
            currentState = state.index
            state.isCurrent = true
        }
        states.add(state)
    }

    /**
     * Creates list of map that represents tree
     * Each map it's a level of the tree, Key - name of transition, Float - number of leaves under this state
     *
     */
    override fun getDerivationTreeElements(): List<Map<String?, Float>> {
        val result = mutableListOf<MutableMap<String?, Float>>()

        data class Path(
            val history: List<String?>,
            val currentState: State?,
            val inputIndex: Int,
            val alive: Boolean
        )

        var paths = mutableListOf<Path>()
        val startState = states.firstOrNull { it.isCurrent }
        if (startState != null) {
            paths.add(Path(listOf(null), startState, 0, true))
        }

        val finishedPaths = mutableListOf<List<String?>>()

        while (paths.any { it.alive }) {
            val nextPaths = mutableListOf<Path>()

            paths.forEach { path ->
                if (!path.alive) {
                    // тянем мёртвый путь дальше
                    nextPaths.add(Path(path.history + null, null, path.inputIndex, false))
                    return@forEach
                }

                if (path.inputIndex == input.length) {
                    finishedPaths.add(path.history + path.currentState?.name)
                    nextPaths.add(Path(path.history + null, null, path.inputIndex, false))
                    return@forEach
                }

                val currentChar = input[path.inputIndex]
                val possibleTransitions = transitions.filter { it.startState == path.currentState?.index && it.name.first() == currentChar }

                if (possibleTransitions.isEmpty()) {
                    finishedPaths.add(path.history + path.currentState?.name)
                    nextPaths.add(Path(path.history + null, null, path.inputIndex, false))
                    return@forEach
                }

                for (transition in possibleTransitions) {
                    val nextState = states.first { it.index == transition.endState }
                    nextPaths.add(Path(path.history + path.currentState?.name, nextState, path.inputIndex + 1, true))
                }
            }

            paths = nextPaths
        }

        // теперь строим уровни
        var maxDepth = 0
        finishedPaths.forEach { maxDepth = maxOf(maxDepth, it.size) }

        val normalizedPaths = finishedPaths.map { path ->
            val mutablePath = path.toMutableList()
            while (mutablePath.size < maxDepth) {
                mutablePath.add(null)
            }
            mutablePath.toList()
        }

        for (level in 1 until maxDepth) {
            val levelMap = mutableMapOf<String?, Float>()
            for (path in normalizedPaths) {
                if (path.size > level) {
                    val stateName = path[level]
                    levelMap[stateName] = (levelMap[stateName] ?: 0f) + 1f
                }
            }
            result.add(levelMap)
        }

        return result
    }








    private fun getListOfAppropriateTransitions(startState: State): List<Transition> {
        return transitions.filter {
            it.startState == startState.index && input.startsWith(it.name)
        }
    }



}