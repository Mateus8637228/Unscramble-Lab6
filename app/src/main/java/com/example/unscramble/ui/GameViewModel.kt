package com.example.unscramble.ui

import androidx.lifecycle.ViewModel
import com.example.unscramble.data.MAX_NO_OF_WORDS
import com.example.unscramble.data.SCORE_INCREASE
import com.example.unscramble.data.allWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class GameViewModel(private val random: Random = Random.Default) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    private var words = emptyList<String>()
    private var currentWord = ""

    init {
        resetGame()
    }

    fun resetGame() {
        // A shuffled deck selects random words without replacement (no recursive retries).
        words = allWords.shuffled(random).take(MAX_NO_OF_WORDS)
        _uiState.value = GameUiState(currentScrambledWord = scrambledWordAt(0))
    }

    fun updateUserGuess(guessedWord: String) {
        val state = _uiState.value
        if (!state.isGameOver) {
            _uiState.value = state.copy(userGuess = guessedWord)
        }
    }

    fun checkUserGuess() {
        val state = _uiState.value
        if (state.isGameOver) return
        if (state.userGuess.equals(currentWord, ignoreCase = true)) {
            advance(state.score + SCORE_INCREASE)
        } else {
            _uiState.value = state.copy(isGuessedWordWrong = true, userGuess = "")
        }
    }

    fun skipWord() {
        val state = _uiState.value
        if (state.isGameOver) return
        advance(state.score)
    }

    private fun advance(score: Int) {
        val state = _uiState.value
        _uiState.value = if (state.currentWordCount == MAX_NO_OF_WORDS) {
            state.copy(score = score, isGameOver = true, isGuessedWordWrong = false, userGuess = "")
        } else {
            state.copy(
                currentScrambledWord = scrambledWordAt(state.currentWordCount),
                currentWordCount = state.currentWordCount + 1,
                score = score,
                isGuessedWordWrong = false,
                userGuess = ""
            )
        }
    }

    private fun scrambledWordAt(index: Int): String {
        currentWord = words[index]
        val letters = currentWord.toCharArray()
        letters.shuffle(random)
        val scrambled = String(letters)
        // All starter words have at least two different letters. A rotation guarantees
        // a changed word even when the random permutation happens to be the identity.
        return if (scrambled == currentWord) currentWord.drop(1) + currentWord.first() else scrambled
    }
}
