# Используем базовый образ с Java Runtime Environment (JRE) нужной версии
# Замените '17' на вашу версию Java, если она другая (например, 21)
FROM eclipse-temurin:17-jre-jammy

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем собранный JAR-файл из папки сборки вашего проекта (target или build/libs)
# внутрь контейнера и переименовываем его в app.jar для простоты.
# ВАЖНО: Перед сборкой Docker-образа нужно выполнить 'mvn package' или 'gradle bootJar'!
# Замените 'target/*.jar' на 'build/libs/*.jar', если используете Gradle
COPY target/*.jar app.jar

# Указываем порт, который слушает ваше Spring Boot приложение внутри контейнера
EXPOSE 8080

# Команда для запуска вашего приложения при старте контейнера
ENTRYPOINT ["java", "-jar", "app.jar"]