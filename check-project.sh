#!/bin/bash
# Скрипт для проверки проекта на наличие всех необходимых файлов и зависимостей

echo "============================================================"
echo "Проверка проекта Truck Navigator"
echo "============================================================"

# Функция для проверки наличия файла
check_file() {
    if [ -f "$1" ]; then
        echo "[✓] Файл $1 существует"
    else
        echo "[✗] ОШИБКА: Файл $1 не найден!"
        MISSING_FILES=1
    fi
}

# Функция для проверки наличия папки
check_dir() {
    if [ -d "$1" ]; then
        echo "[✓] Директория $1 существует"
    else
        echo "[✗] ОШИБКА: Директория $1 не найдена!"
        MISSING_DIRS=1
    fi
}

# Проверка наличия основных инструментов
echo "Проверка наличия инструментов:"
command -v java >/dev/null 2>&1 || { echo "[✗] ОШИБКА: Java не установлена"; exit 1; }
echo "[✓] Java установлена: $(java -version 2>&1 | head -n 1)"

command -v mvn >/dev/null 2>&1 || { echo "[✗] ОШИБКА: Maven не установлен"; exit 1; }
echo "[✓] Maven установлен: $(mvn -version | head -n 1)"

command -v docker >/dev/null 2>&1 || { echo "[✗] ПРЕДУПРЕЖДЕНИЕ: Docker не установлен, докеризация невозможна"; }
if command -v docker >/dev/null 2>&1; then
    echo "[✓] Docker установлен: $(docker --version)"
fi

# Проверка критически важных файлов проекта
echo ""
echo "Проверка критически важных файлов:"
check_file "pom.xml"
check_file "src/main/resources/application.properties"
check_file "src/main/resources/application-dev.properties"
check_file "Dockerfile"
check_file "docker-compose.yml"

# Проверка директорий
echo ""
echo "Проверка структуры проекта:"
check_dir "src/main/java/ru/maslov/trucknavigator/config"
check_dir "src/main/java/ru/maslov/trucknavigator/controller"
check_dir "src/main/java/ru/maslov/trucknavigator/dto"
check_dir "src/main/java/ru/maslov/trucknavigator/entity"
check_dir "src/main/java/ru/maslov/trucknavigator/exception"
check_dir "src/main/java/ru/maslov/trucknavigator/repository"
check_dir "src/main/java/ru/maslov/trucknavigator/service"

# Проверка наличия файла .env
echo ""
echo "Проверка файла конфигурации .env:"
if [ -f ".env" ]; then
    echo "[✓] Файл .env существует"

    # Проверка наличия ключевых переменных в .env
    grep -q "POSTGRES_PASSWORD" .env && echo "  [✓] POSTGRES_PASSWORD определен" || echo "  [✗] ПРЕДУПРЕЖДЕНИЕ: POSTGRES_PASSWORD не определен в .env"
    grep -q "OPENWEATHER_API_KEY" .env && echo "  [✓] OPENWEATHER_API_KEY определен" || echo "  [✗] ПРЕДУПРЕЖДЕНИЕ: OPENWEATHER_API_KEY не определен в .env"
else
    echo "[✗] ПРЕДУПРЕЖДЕНИЕ: Файл .env не найден. Создайте его из .env.example"
fi

# Проверка наличия директории для OSM данных
echo ""
echo "Проверка наличия каталога с данными OSM:"
if [ -d "osm-data" ]; then
    echo "[✓] Каталог osm-data существует"
    OSM_FILES_COUNT=$(find osm-data -name "*.osm.pbf" | wc -l)
    if [ $OSM_FILES_COUNT -gt 0 ]; then
        echo "  [✓] Найдено $OSM_FILES_COUNT OSM файлов: $(find osm-data -name "*.osm.pbf" | xargs basename)"
    else
        echo "  [✗] ПРЕДУПРЕЖДЕНИЕ: В каталоге osm-data нет файлов .osm.pbf"
    fi
else
    echo "[✗] ПРЕДУПРЕЖДЕНИЕ: Каталог osm-data не найден. Создайте его и поместите туда данные OSM"
fi

# Проверка компиляции проекта
echo ""
echo "Проверка компиляции проекта:"
if mvn clean compile -DskipTests -q; then
    echo "[✓] Проект успешно компилируется"
else
    echo "[✗] ОШИБКА: Проект не компилируется"
    exit 1
fi

echo ""
echo "============================================================"
echo "Проверка завершена."
if [ "$MISSING_FILES" == "1" ] || [ "$MISSING_DIRS" == "1" ]; then
    echo "Обнаружены отсутствующие файлы или директории. Исправьте проблемы перед запуском."
else
    echo "Основные проверки прошли успешно. Вы можете запустить проект."
fi
echo "============================================================"