package com.example.quizapp_kotlin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.opencsv.CSVReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var currentSubject: String = ""
    private val usedQuestions = mutableListOf<Int>()
    private var filteredQuestions = mutableListOf<Question>()
    private var score: Int = 0
    private var totalQuestions: Int = 0
    private var hasAnswered: Boolean = false
    private var currentQuestion: Question? = null
    private var selectedAnswerIndex: Int? = null

    private val questionsDB = mutableMapOf<String, MutableList<Question>>()
    private val prefs by lazy { getSharedPreferences("QuizScores", MODE_PRIVATE) }

    private fun loadQuestionsFromCSV(fileName: String): List<Question> {
        val questions = mutableListOf<Question>()
        try {
            assets.open(fileName).bufferedReader().useLines { lines ->
                lines.drop(1).forEach { line -> // 첫 줄은 헤더
                    val tokens = line.split(",")
                    if (tokens.size >= 8) {
                        questions.add(
                            Question(
                                subject = tokens[0].trim(),
                                question = tokens[1].trim(),
                                passage = tokens[2].trim(),
                                choices = tokens.subList(3, 7).map { it.trim() },
                                answer = tokens[7].trim().toIntOrNull() ?: 0,
                                explanation = tokens.getOrNull(8)?.trim() ?: "",
                                level = tokens.getOrNull(9)?.trim()?.toIntOrNull() ?: 1,
                                themes = tokens.drop(10).map { it.trim() }
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "$fileName 로드 실패", Toast.LENGTH_SHORT).show()
        }
        return questions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadAllQuestions()
        showScreen("start")

        // 시작 화면
        findViewById<Button>(R.id.start_quiz_button).setOnClickListener { showScreen("main") }
        findViewById<Button>(R.id.exit_start_button).setOnClickListener { showExitConfirmDialog() }

        // 메인 화면
        findViewById<Button>(R.id.go_to_start_from_main_button).setOnClickListener { showScreen("start") }
        findViewById<Button>(R.id.exit_main_button).setOnClickListener { showExitConfirmDialog() }

        // 과목 버튼
        findViewById<Button>(R.id.korean_subject_button).setOnClickListener { goToTheme("국어") }
        findViewById<Button>(R.id.history_subject_button).setOnClickListener { goToTheme("한국사") }
        findViewById<Button>(R.id.social_subject_button).setOnClickListener { goToTheme("사회") }
        findViewById<Button>(R.id.math_subject_button).setOnClickListener { goToTheme("수학") }

        // 테마 화면
        findViewById<Button>(R.id.go_to_main_from_theme_button).setOnClickListener { showScreen("main") }
        findViewById<Button>(R.id.exit_theme_button).setOnClickListener { showExitConfirmDialog() }
        findViewById<Button>(R.id.start_quiz_all_button).setOnClickListener { startQuiz() }
        findViewById<Button>(R.id.start_quiz_filtered_button).setOnClickListener { startQuiz(true) }

        // 퀴즈 화면
        findViewById<Button>(R.id.go_to_main_from_quiz_button).setOnClickListener { showScreen("main") }
        findViewById<Button>(R.id.exit_quiz_button).setOnClickListener { showExitConfirmDialog() }
        findViewById<Button>(R.id.check_answer_button).setOnClickListener { checkAnswer() }
        findViewById<Button>(R.id.show_explanation_button).setOnClickListener { showExplanation() }
        findViewById<Button>(R.id.next_question_button).setOnClickListener { nextQuestion() }
        findViewById<Button>(R.id.finish_quiz_button).setOnClickListener { goToFinish() }

        // 종료 화면
        findViewById<Button>(R.id.go_to_start_from_finish_button).setOnClickListener { showScreen("start") }
        findViewById<Button>(R.id.exit_finish_button).setOnClickListener { showExitConfirmDialog() }
        findViewById<Button>(R.id.restart_quiz_button).setOnClickListener { startQuiz() }
        findViewById<Button>(R.id.go_to_main_from_finish_button).setOnClickListener { showScreen("main") }
    }

    // 오늘 날짜 키
    private fun getTodayKey(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    // 오늘 점수 저장
    private fun saveTodayScore(subject: String, correct: Int, total: Int) {
        val today = getTodayKey()
        val editor = prefs.edit()
        val correctKey = "${today}_${subject}_correct"
        val totalKey = "${today}_${subject}_total"

        editor.putInt(correctKey, prefs.getInt(correctKey, 0) + correct)
        editor.putInt(totalKey, prefs.getInt(totalKey, 0) + total)
        editor.apply()
    }

    // 오늘 점수 불러오기
    private fun loadTodayScores(): Map<String, Pair<Int, Int>> {
        val today = getTodayKey()
        val subjects = listOf("국어", "한국사", "사회", "수학")
        val scores = mutableMapOf<String, Pair<Int, Int>>()

        for (sub in subjects) {
            val correct = prefs.getInt("${today}_${sub}_correct", 0)
            val total = prefs.getInt("${today}_${sub}_total", 0)
            scores[sub] = correct to total
        }
        return scores
    }

    // CSV 문제 로드
    private fun loadAllQuestions() {
        val fileNames = mapOf(
            "국어" to "korean_questions.csv",
            "한국사" to "history_questions.csv",
            "사회" to "social_questions.csv",
            "수학" to "math_questions.csv"
        )
        fileNames.forEach { (subject, fileName) ->
            try {
                val inputStream = assets.open(fileName)
                val reader = CSVReader(InputStreamReader(inputStream))
                val lines = reader.readAll()

                val questions = lines.drop(1).mapNotNull { line ->
                    val values = line.map { it.trim() }
                    if (values.size >= 11) {
                        try {
                            Question(
                                subject = values[0],
                                question = values[1],
                                passage = values[2],
                                choices = listOf(values[3], values[4], values[5], values[6]),
                                answer = values[7].toInt(),
                                explanation = values[8],
                                level = values[9].toInt(),
                                themes = values.drop(10).filter { it.isNotEmpty() }
                            )
                        } catch (e: Exception) {
                            Log.e("CSV_PARSING", "Parsing error: $line", e)
                            null
                        }
                    } else null
                }
                questionsDB[subject] = questions.toMutableList()
                reader.close()
            } catch (e: Exception) {
                Log.e("CSV_LOAD", "Failed to load $fileName", e)
                Toast.makeText(this, "파일 로드 실패: $fileName", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 화면 전환
    private fun showScreen(screen: String) {
        findViewById<LinearLayout>(R.id.start_screen).visibility = View.GONE
        findViewById<LinearLayout>(R.id.main_screen).visibility = View.GONE
        findViewById<LinearLayout>(R.id.theme_screen).visibility = View.GONE
        findViewById<ScrollView>(R.id.quiz_screen_scrollview).visibility = View.GONE
        findViewById<LinearLayout>(R.id.finish_screen).visibility = View.GONE

        when (screen) {
            "start" -> findViewById<LinearLayout>(R.id.start_screen).visibility = View.VISIBLE
            "main" -> {
                findViewById<LinearLayout>(R.id.main_screen).visibility = View.VISIBLE
                updateScores()
            }
            "theme" -> findViewById<LinearLayout>(R.id.theme_screen).visibility = View.VISIBLE
            "quiz" -> findViewById<ScrollView>(R.id.quiz_screen_scrollview).visibility = View.VISIBLE
            "finish" -> findViewById<LinearLayout>(R.id.finish_screen).visibility = View.VISIBLE
        }
    }

    private fun showExitConfirmDialog() {
        val dialog = AlertDialog.Builder(this)
            .setMessage("종료 하시겠습니까?")
            .setPositiveButton("확인") { _, _ -> finishAffinity() }
            .setNegativeButton("취소") { d, _ -> d.dismiss() }
            .create()
        dialog.show()
    }

    // 테마 화면
    private fun goToTheme(subject: String) {
        currentSubject = subject
        findViewById<TextView>(R.id.theme_subject_title).text = subject

        val levelSpinner = findViewById<Spinner>(R.id.level_list_spinner)
        val levels = listOf("난이도 선택", "1", "2", "3")
        val levelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        levelSpinner.adapter = levelAdapter

        val themeSpinner = findViewById<Spinner>(R.id.theme_list_spinner)
        val themes = questionsDB[currentSubject]?.flatMap { it.themes }?.toSet()?.sorted()?.toMutableList() ?: mutableListOf()
        themes.add(0, "전체영역")
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = themeAdapter

        showScreen("theme")
    }

    // 퀴즈 시작
    private fun startQuiz(isFiltered: Boolean = false) {
        score = 0
        totalQuestions = 0
        usedQuestions.clear()
        filteredQuestions = questionsDB[currentSubject]?.toMutableList() ?: mutableListOf()

        if (isFiltered) {
            val selectedLevel = findViewById<Spinner>(R.id.level_list_spinner).selectedItem.toString()
            val selectedTheme = findViewById<Spinner>(R.id.theme_list_spinner).selectedItem.toString()

            if (selectedLevel != "난이도 선택") {
                filteredQuestions = filteredQuestions.filter { it.level == selectedLevel.toInt() }.toMutableList()
            }
            if (selectedTheme != "전체영역") {
                filteredQuestions = filteredQuestions.filter { it.themes.contains(selectedTheme) }.toMutableList()
            }
        }

        if (filteredQuestions.isEmpty()) {
            Toast.makeText(this, "선택한 조건에 맞는 문제가 없습니다.", Toast.LENGTH_LONG).show()
            return
        }

        findViewById<TextView>(R.id.quiz_subject_title).text = currentSubject
        findViewById<TextView>(R.id.score_display).text = "현재 점수: 0/0 (총 20문제)"
        showScreen("quiz")
        nextQuestion()
    }

    // 다음 문제
    private fun nextQuestion() {
        hasAnswered = false
        selectedAnswerIndex = null

        findViewById<TextView>(R.id.result_text).visibility = View.GONE
        findViewById<TextView>(R.id.explanation_text).visibility = View.GONE
        findViewById<TextView>(R.id.correct_answer_text).visibility = View.GONE
        findViewById<Button>(R.id.show_explanation_button).visibility = View.GONE
        findViewById<Button>(R.id.next_question_button).visibility = View.GONE
        findViewById<Button>(R.id.finish_quiz_button).visibility = View.GONE
        findViewById<Button>(R.id.check_answer_button).visibility = View.VISIBLE

        val availableQuestions = filteredQuestions.filterIndexed { index, _ -> index !in usedQuestions }
        if (availableQuestions.isEmpty() || totalQuestions >= 20) {
            goToFinish()
            return
        }

        val randomIndex = (0 until availableQuestions.size).random()
        currentQuestion = availableQuestions[randomIndex]
        usedQuestions.add(filteredQuestions.indexOf(currentQuestion))
        totalQuestions++

        findViewById<TextView>(R.id.question_text).text = "문제: ${currentQuestion?.question}"
        val passageText = findViewById<TextView>(R.id.passage_text)
        if (currentQuestion?.passage != "없음" && !currentQuestion?.passage.isNullOrEmpty()) {
            passageText.text = "지문: ${currentQuestion?.passage}"
            passageText.visibility = View.VISIBLE
        } else {
            passageText.visibility = View.GONE
        }

        val choicesContainer = findViewById<LinearLayout>(R.id.choices_container)
        choicesContainer.removeAllViews()

        val shuffledChoices = currentQuestion?.choices
            ?.mapIndexed { index, choice -> index to choice }
            ?.shuffled()

        shuffledChoices?.forEach { (index, choice) ->
            val btn = Button(this).apply {
                text = "${index + 1}. $choice"
                textSize = 16f
                tag = index
                setOnClickListener { onChoiceSelected(this, index) }
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))
            }
            choicesContainer.addView(btn)
        }
    }

    // 선택지 클릭
    private fun onChoiceSelected(button: Button, index: Int) {
        val container = findViewById<LinearLayout>(R.id.choices_container)
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is Button) {
                // 모든 버튼의 색상을 초기화 (어두운 회색)
                child.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
        }
        // 선택한 버튼만 파란색으로 변경
        button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))
        selectedAnswerIndex = index
    }

    // 답 확인
    private fun checkAnswer() {
        if (hasAnswered) {
            Toast.makeText(this, "이미 푼 문제입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedAnswerIndex == null) {
            Toast.makeText(this, "답안을 선택하세요!", Toast.LENGTH_SHORT).show()
            return
        }

        hasAnswered = true
        findViewById<Button>(R.id.check_answer_button).visibility = View.GONE
        findViewById<Button>(R.id.show_explanation_button).visibility = View.VISIBLE
        findViewById<Button>(R.id.next_question_button).visibility = View.VISIBLE

        if (totalQuestions >= 20 || usedQuestions.size >= filteredQuestions.size) {
            findViewById<Button>(R.id.next_question_button).visibility = View.GONE
            findViewById<Button>(R.id.finish_quiz_button).visibility = View.VISIBLE
        }

        findViewById<TextView>(R.id.result_text).visibility = View.VISIBLE
        findViewById<TextView>(R.id.correct_answer_text).visibility = View.VISIBLE

        val choicesContainer = findViewById<LinearLayout>(R.id.choices_container)

        // 정답 및 오답에 따른 색상 변경 로직
        val correctButtonIndex = currentQuestion?.answer

        for (i in 0 until choicesContainer.childCount) {
            val button = choicesContainer.getChildAt(i) as Button
            button.isClickable = false

            val buttonIndex = button.tag as Int // tag에 저장된 원래 인덱스 가져오기

            if (buttonIndex == selectedAnswerIndex) {
                // 사용자가 선택한 버튼
                if (selectedAnswerIndex == correctButtonIndex) {
                    // 정답인 경우
                    button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                    score++
                } else {
                    // 오답인 경우
                    button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                }
            } else if (buttonIndex == correctButtonIndex) {
                // 선택하지 않았지만 정답인 버튼
                button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            }
        }

        if (selectedAnswerIndex == currentQuestion?.answer) {
            findViewById<TextView>(R.id.result_text).text = "결과: 정답입니다!"
        } else {
            findViewById<TextView>(R.id.result_text).text = "결과: 오답입니다."
        }

        findViewById<TextView>(R.id.correct_answer_text).text = "정답: ${currentQuestion!!.choices[currentQuestion!!.answer]}"
        findViewById<TextView>(R.id.score_display).text = "현재 점수: $score/$totalQuestions (총 20문제)"

        // 해설 버튼을 보이고 해설 텍스트를 표시하도록 수정
        showExplanation()
    }

    private fun showExplanation() {
        findViewById<TextView>(R.id.explanation_text).visibility = View.VISIBLE
        findViewById<TextView>(R.id.explanation_text).text = "해설: ${currentQuestion?.explanation}"
    }

    // 메인 화면 점수 업데이트
    private fun updateScores() {
        val todayScores = loadTodayScores()
        findViewById<TextView>(R.id.score_korean).text = "국어: ${todayScores["국어"]?.first}/${todayScores["국어"]?.second}"
        findViewById<TextView>(R.id.score_history).text = "한국사: ${todayScores["한국사"]?.first}/${todayScores["한국사"]?.second}"
        findViewById<TextView>(R.id.score_social).text = "사회: ${todayScores["사회"]?.first}/${todayScores["사회"]?.second}"
        findViewById<TextView>(R.id.score_math).text = "수학: ${todayScores["수학"]?.first}/${todayScores["수학"]?.second}"
    }

    // 종료 화면
    private fun goToFinish() {
        saveTodayScore(currentSubject, score, totalQuestions)

        val todayScores = loadTodayScores()
        val totalCorrectAll = todayScores.values.sumOf { it.first }
        val totalQuestionsAll = todayScores.values.sumOf { it.second }

        findViewById<TextView>(R.id.finish_subject_title).text = currentSubject
        findViewById<TextView>(R.id.final_score).text = "이번 정답 수: $score/$totalQuestions"
        findViewById<TextView>(R.id.total_score_all).text = "오늘의 누적 점수: $totalCorrectAll/$totalQuestionsAll"

        showScreen("finish")
    }
}