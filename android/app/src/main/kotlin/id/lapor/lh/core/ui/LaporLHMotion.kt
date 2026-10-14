package id.lapor.lh.core.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

object LaporLHMotion {
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.9f, stiffness = 700f)

    fun <T> effects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
}
