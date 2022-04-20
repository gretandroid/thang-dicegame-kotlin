package com.example.dicegamekotlin

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.dicegamekotlin.databinding.ActivityMainBinding
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val diceImages: MutableList<ImageView> = ArrayList()
    private lateinit var diceResIds: IntArray
    var rotateHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == ROTATE_START) {
                refreshRotateButtonToRotating()
            }
            if (msg.what == ROTATE_IN_PROGRESS) {
                refreshDice(msg.obj as ImageView, msg.arg1)
            }
            if (msg.what == ROTATE_END) {
                refreshRotateButtonToRotate()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initDiceImages()

        // init models dices
        diceResIds = intArrayOf(
            R.drawable.de1,
            R.drawable.de2,
            R.drawable.de3,
            R.drawable.de4,
            R.drawable.de5,
            R.drawable.de6
        )

        // Restore state
        if (savedInstanceState != null) {
            diceImages.forEach(Consumer { dice: ImageView ->
                val key = buildDiceStateKey(dice)
                Log.d("MainActivity", "onCreatekey=$key")
                val resId = savedInstanceState.getInt(key)
                if (resId != 0) {
                    Log.d("MainActivity", "onCreatevalue=$resId")
                    dice.setImageResource(resId)
                    dice.tag = resId
                }
            })
        }
        Log.d("MainActivity", "onCreate")
    }

    private fun initDiceImages() {
        val diceGroupLayout = findViewById<LinearLayout>(R.id.diceGroupLayout)
        val numberDice = diceGroupLayout.childCount
        for (i in 0 until numberDice) {
            diceImages.add(diceGroupLayout.getChildAt(i) as ImageView)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        diceImages.forEach(Consumer { dice: ImageView ->
            val resId = dice.tag as Int
            val key = buildDiceStateKey(dice)
            outState.putInt(key, resId)
            Log.d(
                "MainActivity",
                "onSaveInstanceState[key=$key, value=$resId]"
            )
        })
    }

    private fun buildDiceStateKey(dice: ImageView): String {
        return dice.id.toString()
    }

    override fun onStart() {
        super.onStart()
    }

    @Throws(InterruptedException::class)
    fun doRotate(view: View?) {
        val childThread: Thread = object : Thread() {
            override fun run() {
                // BEGIN
                Message().also {
                    it.what = ROTATE_START
                    rotateHandler.sendMessage(it)
                }

                // ROTATING
                val numberDice = diceImages.size
                Executors.newFixedThreadPool(numberDice).also {
                    for (dice in diceImages) {
                        it.submit { doRotate(dice) }
                    }
                    it.shutdown()
                    try {
                        it.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }

                // END
                Message().also {
                    it.what = ROTATE_END
                    rotateHandler.sendMessage(it)
                }
            }
        }
        childThread.start()
    }

    private fun doRotate(dice: ImageView) {
        val numberCycle = ThreadLocalRandom.current().nextInt(MAX_CYCLE - MIN_CYCLE + 1) + MIN_CYCLE
        val random = Random(System.currentTimeMillis())
        var i = 1
        while (i < numberCycle) {
            val resultResId = random.nextInt(6)
            try {
                Thread.sleep((10 * i).toLong())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            Message().also {
                it.what = ROTATE_IN_PROGRESS
                it.obj = dice
                it.arg1 = resultResId
                rotateHandler.sendMessage(it)
            }
            i++
        }
    }

    private fun refreshRotateButtonToRotating() {
        binding.rotateButton.setText(R.string.rotating)
        binding.rotateButton.isEnabled = false
    }

    private fun refreshRotateButtonToRotate() {
        binding.rotateButton.setText(R.string.rotate)
        binding.rotateButton.isEnabled = true
    }

    private fun refreshDice(dice: ImageView, resultResId: Int) {
        dice.setImageResource(diceResIds[resultResId])
        dice.tag = diceResIds[resultResId]
    }

    companion object {
        const val MAX_CYCLE = 30
        const val MIN_CYCLE = 10
        private const val ROTATE_START = 1
        private const val ROTATE_IN_PROGRESS = 2
        private const val ROTATE_END = 3
        const val SEPARATOR = "_"
    }
}