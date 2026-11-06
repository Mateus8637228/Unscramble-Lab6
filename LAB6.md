# Лабораторная №6 — задание 4: Unscramble

Официальный starter Google Android Training дополнен игровой логикой,
GameUiState, GameViewModel, StateFlow и UDF. Слова не повторяются; правильный
ответ даёт 20 очков, пропуск не меняет счёт. После 10 слов показывается итоговый
диалог с возможностью новой игры. Состояние сохраняется при повороте.

Оба WordsData.kt сохранены без изменений. Есть пять требуемых тестов и четыре
дополнительных; покрытие GameViewModel проверяется для строк, ветвей,
инструкций и методов с порогом 100%, без исключения методов класса.

Открыть проект отдельно в Android Studio, выбрать JDK 25 и SDK 37.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:verifyGameViewModelCoverage
./gradlew :app:connectedDebugAndroidTest
```

HTML JaCoCo создаётся локально в `app/build/reports/jacoco/jacocoGameViewModelReport/html/`.
Предупреждение о WordsDataKt ожидаемо: starter использует одноимённый тестовый
словарь. Оно не относится к GameViewModel; 100% — покрытие ViewModel, не всего APK.
Инструментальный сценарий проверяет настоящий экран и production-словарь.
Защита перед публикацией: [SECURITY.md](SECURITY.md).
