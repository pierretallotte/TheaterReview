package com.example.theaterreview

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.io.IOException
import java.io.InputStreamReader

class ReviewActivity : AppCompatActivity() {
    private lateinit var scene: Scene
    private lateinit var linesIt: MutableIterator<Scene.Line>
    private lateinit var layout: LinearLayout
    private lateinit var tts: TextToSpeech
    private lateinit var preferences: SharedPreferences

    private fun fatalAlert(message: String, title: String) {
        val builder = let { AlertDialog.Builder(it) }
        builder.setMessage(message).setTitle(title)
        val dialog = builder.create()
        dialog.setOnDismissListener { finish() }
        dialog.show()
    }

    private fun check(solution: String, guess: String): SpannableStringBuilder {
        val processedSolution = StringProcessor(solution)
        val processedGuess = StringProcessor(guess)

        val matcher = SequenceMatcher(processedSolution.processed, processedGuess.processed)
        val opcodes = matcher.opcodes

        val spanBuilder = SpannableStringBuilder()

        for(opcode in opcodes) {
            when (opcode.tag) {
                SequenceMatcher.Opcode.Tag.EQUAL -> {
                    val currentString = processedGuess.original.subList(opcode.bBegin, opcode.bEnd).joinToString(separator="") { it }
                    val start = spanBuilder.length
                    spanBuilder.append(currentString as CharSequence)
                    val end = spanBuilder.length
                    spanBuilder.setSpan(ForegroundColorSpan(Color.GREEN), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                SequenceMatcher.Opcode.Tag.INSERT -> {
                    val currentString = processedGuess.original.subList(opcode.bBegin, opcode.bEnd).joinToString(separator="") { it }
                    val start = spanBuilder.length
                    spanBuilder.append(currentString as CharSequence)
                    val end = spanBuilder.length
                    val strikethroughSpan = StrikethroughSpan()
                    spanBuilder.setSpan(ForegroundColorSpan(Color.RED), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spanBuilder.setSpan(strikethroughSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                SequenceMatcher.Opcode.Tag.DELETE -> {
                    val currentString = processedSolution.original.subList(opcode.aBegin, opcode.aEnd).joinToString(separator="") { it }
                    val start = spanBuilder.length
                    spanBuilder.append(currentString as CharSequence)
                    val end = spanBuilder.length
                    spanBuilder.setSpan(ForegroundColorSpan(Color.RED), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                else -> { // REPLACE
                    // INSERT
                    var currentString = processedGuess.original.subList(opcode.bBegin, opcode.bEnd).joinToString(separator="") { it }
                    var start = spanBuilder.length
                    spanBuilder.append(currentString as CharSequence)
                    var end = spanBuilder.length
                    val strikethroughSpan = StrikethroughSpan()
                    spanBuilder.setSpan(ForegroundColorSpan(Color.RED), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spanBuilder.setSpan(strikethroughSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                    // and DELETE
                    currentString = processedSolution.original.subList(opcode.aBegin, opcode.aEnd).joinToString(separator="") { it }
                    start = spanBuilder.length
                    spanBuilder.append(currentString as CharSequence)
                    end = spanBuilder.length
                    spanBuilder.setSpan(ForegroundColorSpan(Color.RED), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        return spanBuilder
    }

    private fun nextLine(character: String) {
        if(linesIt.hasNext()) {
            val currentLine = linesIt.next()

            if(currentLine.character.equals(character)) {
                val inputLayout = LinearLayout(this)
                inputLayout.orientation = LinearLayout.HORIZONTAL

                val guessTxt = EditText(this)
                val layoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                layoutParam.weight = 1f
                guessTxt.layoutParams = layoutParam
                inputLayout.addView(guessTxt)

                val sendBtn = Button(this)
                sendBtn.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
                sendBtn.text = getString(R.string.send)
                sendBtn.setOnClickListener {
                    val spanBuilder = check(currentLine.line, guessTxt.text.toString())

                    val solutionTxt = TextView(this)
                    solutionTxt.text = spanBuilder

                    layout.removeView(inputLayout)

                    val solutionLayoutParam = LinearLayout.LayoutParams(layout.layoutParams)
                    solutionLayoutParam.setMargins(10,10,10,10)

                    solutionTxt.setPadding(10,10,10,10)
                    solutionTxt.layoutParams = solutionLayoutParam

                    layout.addView(solutionTxt)

                    nextLine(character)
                }

                inputLayout.addView(sendBtn)
                layout.addView(inputLayout)

                guessTxt.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(guessTxt, InputMethodManager.SHOW_IMPLICIT)
            } else {
                val lineTxt = TextView(this)
                lineTxt.text = currentLine.character.plus("\n").plus(currentLine.line)

                val layoutParam = LinearLayout.LayoutParams(layout.layoutParams)
                layoutParam.setMargins(10,10,10,10)

                lineTxt.setPadding(10,10,10,10)
                lineTxt.layoutParams = layoutParam

                layout.addView(lineTxt)

                if(preferences.getBoolean("tts", false))
                    tts.speak(currentLine.line, TextToSpeech.QUEUE_ADD, null, null)

                nextLine(character)
            }
        } else {
            val exitBtn = Button(this)
            exitBtn.text = getString(R.string.quit)
            exitBtn.setOnClickListener { finish() }
            layout.addView(exitBtn)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { }
        preferences = getSharedPreferences("settings", MODE_PRIVATE)

        val uri = intent.extras?.get("uri") as Uri
        val reader = InputStreamReader(contentResolver.openInputStream(uri))

        try {
            scene = Scene(reader)
        } catch (e : IOException) {
            fatalAlert(getString(R.string.unreadable_file), getString(R.string.error))
        }

        if(scene.characters.size == 0) {
            fatalAlert(getString(R.string.no_characters), getString(R.string.error))
        }

        if(scene.lines.size == 0) {
            fatalAlert(getString(R.string.no_lines), getString(R.string.error))
        }

        linesIt = scene.lines.iterator()

        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)

        layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.TOP
        layout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)

        val selectCharacterTxt = TextView(this)
        selectCharacterTxt.text = getString(R.string.select_character)
        selectCharacterTxt.gravity = Gravity.CENTER
        layout.addView(selectCharacterTxt)

        for(character in scene.characters!!) {
            val characterBtn = Button(this)
            characterBtn.text = character
            characterBtn.setOnClickListener {
                layout.removeAllViews()
                nextLine(characterBtn.text.toString())
            }
            layout.addView(characterBtn)
        }

        scroll.addView(layout)
        setContentView(scroll)
    }

    override fun onDestroy() {
        super.onDestroy()

        tts.shutdown()
    }
}