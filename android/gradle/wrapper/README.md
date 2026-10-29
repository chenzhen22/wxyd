# Gradle Wrapper 说明

本工程仅提交 `gradle-wrapper.properties`。`gradle-wrapper.jar` 与 `gradlew` 脚本无法在当前
JDK 8 环境生成，首次 checkout 后请在装有 JDK 17 的环境中于本目录（`android/`）执行：

    gradle wrapper --gradle-version 8.7

即可生成 `gradlew`、`gradlew.bat` 与 `gradle/wrapper/gradle-wrapper.jar`，随后即可
`./gradlew assembleDebug`。请勿伪造提交空的 wrapper jar。
