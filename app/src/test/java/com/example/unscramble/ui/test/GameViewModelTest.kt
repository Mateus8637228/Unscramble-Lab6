package com.example.unscramble.ui.test

import com.example.unscramble.data.MAX_NO_OF_WORDS
import com.example.unscramble.data.SCORE_INCREASE
import com.example.unscramble.data.getUnscrambledWord
import com.example.unscramble.ui.GameViewModel
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameViewModelTest {
    private val viewModel = GameViewModel()

    @Test fun gameViewModel_IncorrectGuess_ErrorFlagSet() {
        val before = viewModel.uiState.value
        viewModel.updateUserGuess("not-a-word")
        viewModel.checkUserGuess()
        val after = viewModel.uiState.value
        assertTrue(after.isGuessedWordWrong)
        assertEquals(before.currentScrambledWord, after.currentScrambledWord)
        assertEquals(before.currentWordCount, after.currentWordCount)
        assertEquals(0, after.score)
        assertEquals("", after.userGuess)
        assertFalse(after.isGameOver)
    }

    @Test fun gameViewModel_Initialization_FirstWordLoaded() {
        val state = viewModel.uiState.value
        val original = getUnscrambledWord(state.currentScrambledWord)
        assertTrue(original.isNotEmpty())
        assertNotEquals(original, state.currentScrambledWord)
        assertEquals(original.toList().sorted(), state.currentScrambledWord.toList().sorted())
        assertEquals(1, state.currentWordCount)
        assertEquals(0, state.score)
        assertEquals("", state.userGuess)
        assertFalse(state.isGuessedWordWrong)
        assertFalse(state.isGameOver)
    }

    @Test fun gameViewModel_CorrectWordGuessed_ScoreUpdatedAndErrorFlagUnset() {
        viewModel.updateUserGuess("wrong")
        viewModel.checkUserGuess()
        assertTrue(viewModel.uiState.value.isGuessedWordWrong)
        guessCurrentWord(viewModel)
        val state = viewModel.uiState.value
        assertEquals(SCORE_INCREASE, state.score)
        assertEquals(2, state.currentWordCount)
        assertFalse(state.isGuessedWordWrong)
        assertFalse(state.isGameOver)
        assertEquals("", state.userGuess)
    }

    @Test fun gameViewModel_AllWordsGuessed_UiStateUpdatedCorrectly() {
        val seen = mutableSetOf<String>()
        repeat(MAX_NO_OF_WORDS) { round ->
            val before = viewModel.uiState.value
            assertFalse(before.isGameOver)
            assertEquals(round + 1, before.currentWordCount)
            assertTrue("A word must not repeat", seen.add(getUnscrambledWord(before.currentScrambledWord)))
            guessCurrentWord(viewModel)
            assertEquals((round + 1) * SCORE_INCREASE, viewModel.uiState.value.score)
        }
        val state = viewModel.uiState.value
        assertTrue(state.isGameOver)
        assertEquals(MAX_NO_OF_WORDS, state.currentWordCount)
        assertEquals(MAX_NO_OF_WORDS * SCORE_INCREASE, state.score)
        assertFalse(state.isGuessedWordWrong)
        assertEquals("", state.userGuess)
    }

    @Test fun gameViewModel_WordSkipped_ScoreUnchangedAndWordCountIncreased() {
        guessCurrentWord(viewModel)
        val before = viewModel.uiState.value
        viewModel.updateUserGuess("wrong")
        viewModel.checkUserGuess()
        viewModel.updateUserGuess("partial")
        viewModel.skipWord()
        val after = viewModel.uiState.value
        assertEquals(before.score, after.score)
        assertEquals(before.currentWordCount + 1, after.currentWordCount)
        assertNotEquals(getUnscrambledWord(before.currentScrambledWord), getUnscrambledWord(after.currentScrambledWord))
        assertFalse(after.isGuessedWordWrong)
        assertEquals("", after.userGuess)
    }

    @Test fun lastWordSkipped_gameEndsAndFurtherEventsDoNotChangeState() {
        repeat(MAX_NO_OF_WORDS) { viewModel.skipWord() }
        val finished = viewModel.uiState.value
        assertTrue(finished.isGameOver)
        assertEquals(0, finished.score)
        viewModel.updateUserGuess("ignored")
        viewModel.checkUserGuess()
        viewModel.skipWord()
        assertEquals(finished, viewModel.uiState.value)
    }

    @Test fun resetGame_clearsScoreErrorInputAndEndFlag() {
        guessCurrentWord(viewModel)
        viewModel.updateUserGuess("wrong")
        viewModel.checkUserGuess()
        viewModel.updateUserGuess("unfinished")
        viewModel.resetGame()
        assertFreshGame(viewModel)
        repeat(MAX_NO_OF_WORDS) { viewModel.skipWord() }
        viewModel.resetGame()
        assertFreshGame(viewModel)
        guessCurrentWord(viewModel)
        assertEquals(SCORE_INCREASE, viewModel.uiState.value.score)
    }

    @Test fun seededGames_alwaysScrambleAllLettersAndNeverRepeatWords() {
        // Fixed seeds cover ordinary permutations and the identity-permutation fallback
        // with reproducible full games, rather than depending on random coverage.
        for (seed in 0..19) {
            val model = GameViewModel(Random(seed))
            val seen = mutableSetOf<String>()
            repeat(MAX_NO_OF_WORDS) {
                val scrambled = model.uiState.value.currentScrambledWord
                val original = getUnscrambledWord(scrambled)
                assertTrue(original.isNotEmpty())
                assertTrue(seen.add(original))
                assertNotEquals(original, scrambled)
                assertEquals(original.toList().sorted(), scrambled.toList().sorted())
                guessCurrentWord(model)
            }
            assertTrue(model.uiState.value.isGameOver)
            assertEquals(MAX_NO_OF_WORDS * SCORE_INCREASE, model.uiState.value.score)
        }
    }

    @Test fun emptyAnswer_setsErrorWithoutAdvancing() {
        viewModel.checkUserGuess()
        assertTrue(viewModel.uiState.value.isGuessedWordWrong)
        assertEquals(1, viewModel.uiState.value.currentWordCount)
        assertEquals(0, viewModel.uiState.value.score)
    }

    private fun guessCurrentWord(model: GameViewModel) {
        val answer = getUnscrambledWord(model.uiState.value.currentScrambledWord)
        model.updateUserGuess(answer.uppercase())
        model.checkUserGuess()
    }

    private fun assertFreshGame(model: GameViewModel) {
        val state = model.uiState.value
        assertEquals(0, state.score)
        assertEquals(1, state.currentWordCount)
        assertEquals("", state.userGuess)
        assertFalse(state.isGameOver)
        assertFalse(state.isGuessedWordWrong)
        assertTrue(state.currentScrambledWord.isNotEmpty())
    }
}
