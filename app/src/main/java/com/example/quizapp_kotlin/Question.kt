package com.example.quizapp_kotlin// 이 패키지 이름을 본인의 프로젝트에 맞게 수정하세요.

import java.io.Serializable // Question 객체를 화면 간에 전달하기 위해 Serializable 인터페이스를 구현합니다.

// 퀴즈 문제 하나의 데이터를 저장하는 데이터 클래스입니다.
data class Question(
    val subject: String, // 과목 이름을 저장하는 변수입니다.
    val question: String, // 문제 내용을 저장하는 변수입니다.
    val passage: String, // 지문 내용을 저장하는 변수입니다.
    val choices: List<String>, // 4개의 보기들을 리스트로 저장하는 변수입니다.
    val answer: Int, // 정답의 인덱스(0, 1, 2, 3)를 저장하는 변수입니다.
    val explanation: String, // 해설 내용을 저장하는 변수입니다.
    val level: Int, // 문제 난이도를 저장하는 변수입니다.
    val themes: List<String> // 문제 테마들을 리스트로 저장하는 변수입니다.
) : Serializable // Question 클래스를 직렬화하여 Intent로 전달할 수 있게 합니다.