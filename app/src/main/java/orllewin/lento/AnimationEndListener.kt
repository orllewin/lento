package oppen.stracka

import android.animation.Animator

class AnimationEndListener(val onFinish: () -> Unit): Animator.AnimatorListener {
    override fun onAnimationEnd(animator: Animator?) = onFinish()
    override fun onAnimationCancel(animator: Animator?) = Unit
    override fun onAnimationStart(animator: Animator?) = Unit
    override fun onAnimationRepeat(animator: Animator?) = Unit
}