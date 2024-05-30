package com.mapbox.vision.teaser.view

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.mapbox.vision.teaser.R
import com.mapbox.vision.teaser.databinding.ScoreProgressViewBinding
import com.mapbox.vision.teaser.utils.dpToPx

class ScoreProgressView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    // Add a property to hold the generated view binding object
    private val binding: ScoreProgressViewBinding =
        ScoreProgressViewBinding.inflate(LayoutInflater.from(context),this,true)

    private val initScore = 75

    private var currentScore = initScore
        set(value) {
            if (value !in SCORE_MIN..SCORE_MAX) return
            field = value
            redrawProgress()
            redrawScore()
            redrawScoreColor()
        }

    private val gradientColors = intArrayOf(
        Color.parseColor("#57ff3f"), // green
        Color.parseColor("#e9f40e"), // yellow
        Color.parseColor("#ff3636") // red
    )

    private val argbEvaluator = ArgbEvaluator()

    private val warningAndScoreShowDuration = 2000L
    private val animationAppearDuration = 350L
    private val animationDisappearDuration = 350L
    private val betweenNewScoreDuration = 300L
    private var animationWillEndAt = 0L

    private var handlerHideWarning: Handler = Handler(Looper.getMainLooper())
    private var handlerHideScore: Handler = Handler(Looper.getMainLooper())

    private val handlerScore = Handler(Looper.getMainLooper())

    @get:ColorInt
    private val currentColor: Int
        get() =
            when (currentScore) {
                0 -> gradientColors[2]
                50 -> gradientColors[1]
                100 -> gradientColors[0]
                in SCORE_MIN..(SCORE_MAX / 2) -> {
                    argbEvaluator.evaluate(
                        (currentScore.toFloat() / (SCORE_MAX / 2)),
                        gradientColors[2],
                        gradientColors[1]
                    ) as Int
                }
                in (SCORE_MAX / 2)..SCORE_MAX -> {
                    argbEvaluator.evaluate(
                        ((currentScore - 50).toFloat() / (SCORE_MAX / 2)),
                        gradientColors[1],
                        gradientColors[0]
                    ) as Int
                }
                else -> gradientColors[1]
            }

    private val progressGradientWidth by lazy(LazyThreadSafetyMode.NONE) { binding.progressGradient.width }

    companion object {
        private const val SCORE_MIN = 0
        private const val SCORE_MAX = 100
    }

    init {
        View.inflate(context, R.layout.score_progress_view, this)
        binding.progressViewContainer.bringToFront()
        binding.progressScoreContainer.bringToFront()

        val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
            .apply {
                cornerRadius = context.dpToPx(3f)
            }
        binding.progressGradient.background = gradientDrawable

        setupInitScoreMark()
    }

    private fun setupInitScoreMark() {
        val markFactor = 1 - initScore.toFloat() / SCORE_MAX
        binding.initScoreMark.layoutParams =
            (binding.initScoreMark.layoutParams as ConstraintLayout.LayoutParams).apply { horizontalBias = markFactor }
    }

    fun plusScore(amount: Int) = onNewScore(
        DriverScore.Positive(
            amount
        )
    )

    fun minusScore(amount: Int, reason: String) = onNewScore(
        DriverScore.Negative(
            amount,
            reason
        )
    )

    private fun onNewScore(nextScore: DriverScore) {
        val delta = (animationWillEndAt - System.currentTimeMillis()).let { if (it < 0) 0 else it }

        val delayMills: Long = delta + betweenNewScoreDuration

        animationWillEndAt = when (nextScore) {
            is DriverScore.Positive -> 0
            is DriverScore.Negative -> animationAppearDuration + animationDisappearDuration + warningAndScoreShowDuration
        } + delta + System.currentTimeMillis()

        handlerScore.postDelayed({

            when (nextScore) {
                is DriverScore.Positive -> {
                    currentScore += nextScore.score
                }
                is DriverScore.Negative -> {
                    currentScore -= nextScore.score
                    showWarning(nextScore.reason)
                    showWarningScore(nextScore.score)
                }
            }
        }, delayMills)
    }

    private fun redrawProgress() {
        val newWidth = (1f - currentScore.toFloat() / SCORE_MAX).let { it * progressGradientWidth }.toInt()
        binding.progressGray.layoutParams.width = newWidth
        binding.progressGray.requestLayout()
    }

    private fun redrawScore() {
        binding.scoreAmount.text = "$currentScore"
    }

    private fun redrawScoreColor() {
        currentColor.let { color ->
            binding.scoreAmount.setTextColor(color)
            binding.scoreAmountCircle.circleColor = color
        }
    }

    private fun showWarning(message: String) {
        startTimerHideWarningTextAnimation()

        TransitionManager.beginDelayedTransition(
            binding.warningTextContainer,
            Slide(Gravity.TOP).apply { duration = animationAppearDuration })
        if (binding.warningTextContainer.visibility == View.GONE) {
            binding.warningTextContainer.show()
        }
        binding.warningText.text = message
    }

    private fun showWarningScore(score: Int) {
        startTimerHideScore()
        binding.scoreWarningContainer.show()
        binding.scoreWarningText.text = "-$score"
    }

    private fun startTimerHideWarningTextAnimation() {
        handlerHideWarning.removeCallbacksAndMessages(null)
        handlerHideWarning.postDelayed({
            TransitionManager.beginDelayedTransition(
                binding.warningTextContainer,
                Slide(Gravity.TOP).apply { duration = animationDisappearDuration }
            )
            binding.warningTextContainer.hide()
            binding.warningText.text = ""
        }, warningAndScoreShowDuration)
    }

    private fun startTimerHideScore() {
        handlerHideScore.removeCallbacksAndMessages(null)
        handlerHideScore.postDelayed({
            binding.scoreWarningContainer.hide()
            binding.scoreWarningText.text = ""
        }, warningAndScoreShowDuration)
    }
}

private sealed class DriverScore(score: Int) {
    data class Positive(val score: Int) : DriverScore(score)
    data class Negative(val score: Int, val reason: String) : DriverScore(score)
}
