# Return by Death — Minecraft Mod

Re:Zero inspired mod for Minecraft 26.1.2 (Fabric).

## Механіка
- Кожні **20 секунд** зберігається snapshot світу (блоки, інвентар, позиція, HP, їжа, XP)
- При смерті — чорно-білий фільтр (0.3 сек), пауза 2 сек, звук воскресіння, відкат світу назад
- **Один файл snapshot** — постійно перезаписується, не займає місце
- Зберігаються всі завантажені чанки

## Встановлення через Termux + GitHub Actions

### 1. Конвертуй аудіо
```bash
cd /storage/shared/Download
# Клонуй репо спочатку, потім:
bash convert_audio.sh
```

### 2. Запуш на GitHub
```bash
git init
git add .
git commit -m "Initial mod"
git remote add origin https://github.com/ТВІЙ_НІКНЕЙМ/return-by-death.git
git push -u origin main
```

### 3. Скачай .jar
- Зайди на GitHub → Actions → остання збірка → Artifacts → `return-by-death-mod`
- Скинь `.jar` в папку `mods` в Zilaith

## Вимоги
- Minecraft 26.1.2
- Fabric Loader 0.18.5+
- Fabric API 0.144.3+26.1
- Java 25
