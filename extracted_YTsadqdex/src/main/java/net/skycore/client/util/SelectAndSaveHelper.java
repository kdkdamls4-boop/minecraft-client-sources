package net.skycore.client.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Locale;
import java.util.Map;
import net.skycore.client.config.Translations;

public final class SelectAndSaveHelper {
   private static boolean russian;
   private static Path languageFile;
   private static boolean savedSelection;
   private static final Map<String, String> RUSSIAN = Map.ofEntries(
      Map.entry("Totem Glow", "Свечение тотема"),
      Map.entry("Totem Glow Size", "Размер свечения тотема"),
      Map.entry("Totem Size", "Размер частиц тотема"),
      Map.entry("Totem Icon", "Иконка тотема"),
      Map.entry("Actions", "Действия"),
      Map.entry("3D player boxes with a uniform outline and flush fill", "3D-боксы игроков с ровной обводкой и заливкой"),
      Map.entry("A one-minute bird family scene with a twig nest and chicks", "Минутная сценка с птичьей семьёй, гнездом и птенцами"),
      Map.entry("A small companion that follows you", "Маленький питомец, который следует за вами"),
      Map.entry("Adds a player as a friend with the middle mouse button", "Добавляет игрока в друзья средней кнопкой мыши"),
      Map.entry("Allows breaking and interacting through cobwebs", "Позволяет ломать и взаимодействовать сквозь паутину"),
      Map.entry("360 keep-alive", "Обзор 360"),
      Map.entry("Accent", "Акцент"),
      Map.entry("Adaptive Brightness", "Адаптивная яркость"),
      Map.entry("All", "Все"),
      Map.entry("Always", "Всегда"),
      Map.entry("Amount", "Количество"),
      Map.entry("Comets", "Кометы"),
      Map.entry("DynamicSurroundings", "Динамическое окружение"),
      Map.entry(
         "Standalone Dynamic Surroundings audio, ambience and environment effects for 26.2",
         "Атмосферные звуки и эффекты окружения Dynamic Surroundings для 26.2"
      ),
      Map.entry("Ambient Soundscapes", "Фоновое окружение"),
      Map.entry("Ambience Volume", "Громкость атмосферы"),
      Map.entry("Wildlife and Cave Sounds", "Живность и пещерные звуки"),
      Map.entry("Footstep Accents", "Дополнительные шаги"),
      Map.entry("Equip Sounds", "Звуки экипировки"),
      Map.entry("Fireflies", "Светлячки"),
      Map.entry("Rain Splashes", "Брызги дождя"),
      Map.entry("JumpCircles", "Круги прыжка"),
      Map.entry("Trails", "Следы движения"),
      Map.entry("Cartoonish black-and-white 3D brushstrokes behind the moving player", "Мультяшные объёмные чёрно-белые мазки за движущимся персонажем"),
      Map.entry("Number of individual hand-painted motion strokes", "Количество отдельных рисованных мазков"),
      Map.entry("Maximum length of each painted streak", "Максимальная длина мазка"),
      Map.entry("Brushstroke thickness", "Толщина мазка кисти"),
      Map.entry("Brush drawing and disappearance speed", "Скорость прорисовки и исчезновения штрихов"),
      Map.entry("A hand-drawn theme-coloured jump ring with refracted ground", "Нарисованный от руки круг при прыжке с преломлением земли"),
      Map.entry("Appearance Speed", "Скорость появления"),
      Map.entry("How fast jump circles unfold and fade", "Скорость раскрытия и затухания кругов"),
      Map.entry("Maximum radius in blocks", "Максимальный радиус в блоках"),
      Map.entry("Distortion Strength", "Сила искажения"),
      Map.entry("Refraction inside the hand-drawn circle", "Преломление изображения внутри нарисованного круга"),
      Map.entry("Thunder Shatter", "Грозовой раскол"),
      Map.entry("Electric lightning and prismatic glass burst from a totem pop", "Электрические разряды и сияющие осколки при срабатывании тотема"),
      Map.entry("Naked", "Без брони"),
      Map.entry("Two glowing comets flying around the shoulders and chest", "Две светящиеся кометы вокруг плеч, спины и груди"),
      Map.entry("Comet and trail size", "Размер комет и следов"),
      Map.entry("Flight speed", "Скорость полёта"),
      Map.entry("Follow", "Цель"),
      Map.entry("Animals", "Животные"),
      Map.entry("Animation Strength", "Сила анимации"),
      Map.entry("Anti flight key", "Клавиша антифлая"),
      Map.entry("Armor", "Броня"),
      Map.entry("Arrows", "Стрелы"),
      Map.entry("Attack", "Атака"),
      Map.entry("Attack Count", "Количество атак"),
      Map.entry("Attack range", "Дальность атаки"),
      Map.entry("Attack Speed", "Скорость атаки"),
      Map.entry("Traps the target in cobwebs and hands it to CrystalAura", "Ловит цель в паутину и передаёт её CrystalAura"),
      Map.entry("Attacks nearby targets with silent aim", "Атакует ближайшие цели с незаметной наводкой"),
      Map.entry("Attacks only the target under your crosshair", "Атакует только цель под прицелом"),
      Map.entry("Automatic wind-charge mace smash", "Автоматический удар булавой с зарядом ветра"),
      Map.entry("Automatically detonates crystals within three blocks", "Автоматически взрывает кристаллы в радиусе трёх блоков"),
      Map.entry("Automatically equips a totem in dangerous situations", "Автоматически берёт тотем в опасных ситуациях"),
      Map.entry("Automatically sprints while the player is moving", "Автоматически включает спринт при движении"),
      Map.entry("Automatically throws splash potions", "Автоматически бросает взрывные зелья"),
      Map.entry("Avoids knockback from players, blocks, water, and fishing hooks", "Убирает отталкивание игроками, блоками, водой и поплавками"),
      Map.entry("Aura range", "Дальность ауры"),
      Map.entry("Auto Jump", "Автопрыжок"),
      Map.entry("Auto Takeoff", "Автовзлёт"),
      Map.entry("Bad Effects", "Негативные эффекты"),
      Map.entry("Block", "Блок"),
      Map.entry("Blocks", "Блоки"),
      Map.entry("Bloom", "Свечение"),
      Map.entry("Blur Distance", "Дальность размытия"),
      Map.entry("Blur Strength", "Сила размытия"),
      Map.entry("Blur Tint", "Оттенок размытия"),
      Map.entry("Boost", "Ускорение"),
      Map.entry("Boost Mode", "Режим ускорения"),
      Map.entry("Burst Power", "Сила рывка"),
      Map.entry("Boss Bar", "Полоса босса"),
      Map.entry("Break Shield", "Ломать щит"),
      Map.entry("Breaks blocks faster", "Ускоряет ломание блоков"),
      Map.entry("Block selection overlay", "Подсветка выбранного блока"),
      Map.entry("Boosts elytra flight from fireworks", "Ускоряет полёт на элитрах от фейерверков"),
      Map.entry("Brightens dark areas of the world", "Осветляет тёмные участки мира"),
      Map.entry("Brightness", "Яркость"),
      Map.entry("Bypass", "Обход"),
      Map.entry("Camera Clip", "Камера сквозь стены"),
      Map.entry("Catch peak", "Ловить пик"),
      Map.entry("Celestial Rotation", "Вращение неба"),
      Map.entry("Cape", "Плащ"),
      Map.entry("Change aura range", "Изменение дальности ауры"),
      Map.entry("Change Saturation", "Изменение насыщенности"),
      Map.entry("Change Sky", "Изменение неба"),
      Map.entry("Change Time", "Изменение времени"),
      Map.entry("Change World Color", "Изменение цвета мира"),
      Map.entry("Check Sky Access", "Проверять доступ к небу"),
      Map.entry("Chat filter", "Фильтр чата"),
      Map.entry("Client", "Клиент"),
      Map.entry("Client Look", "Клиентский поворот"),
      Map.entry("Client Sounds", "Звуки клиента"),
      Map.entry("Combat", "Бой"),
      Map.entry("Clouds", "Облака"),
      Map.entry("Close menu", "Закрывать меню"),
      Map.entry("Color", "Цвет"),
      Map.entry("Color Mode", "Режим цвета"),
      Map.entry("Coverage", "Покрытие"),
      Map.entry("Current", "Текущий"),
      Map.entry("Custom", "Свой"),
      Map.entry("Custom Color", "Свой цвет"),
      Map.entry("Custom Time", "Своё время"),
      Map.entry("Configures first-person hand positions", "Настраивает положение рук от первого лица"),
      Map.entry("Client tick timer", "Таймер клиентских тиков"),
      Map.entry("Custom wavey cape behind the player", "Плавно развевающийся плащ за игроком"),
      Map.entry("Customizes attack animations", "Настраивает анимации атаки"),
      Map.entry("Configure in World", "Настроить в мире"),
      Map.entry("Cycle Duration", "Длительность цикла"),
      Map.entry("Cycle From", "Начало цикла"),
      Map.entry("Cycle To", "Конец цикла"),
      Map.entry("Day", "День"),
      Map.entry("Default Material Lists", "Стандартные списки материалов"),
      Map.entry("Default", "Обычный"),
      Map.entry("Delay", "Задержка"),
      Map.entry("Delay (ms)", "Задержка (мс)"),
      Map.entry("Density", "Плотность"),
      Map.entry("Diagnostics", "Диагностика"),
      Map.entry("Dimension Reverb", "Реверберация измерения"),
      Map.entry("Disable", "Отключить"),
      Map.entry("Disable Effect", "Отключать эффект"),
      Map.entry("Disable on death", "Отключать после смерти"),
      Map.entry("Distance", "Дистанция"),
      Map.entry("Don't attack while eating", "Не атаковать во время еды"),
      Map.entry("Downward Swing", "Взмах вниз"),
      Map.entry("DropDown Angle", "Угол опускания"),
      Map.entry("DropDown Slant", "Наклон опускания"),
      Map.entry("Draws compact nametags for selected entities", "Рисует компактные теги выбранных сущностей"),
      Map.entry("Draws directional arrows for selected targets", "Показывает стрелки направления на выбранные цели"),
      Map.entry("Depth fog blur using the GUI backdrop blur", "Размытие тумана по глубине через размытие фона GUI"),
      Map.entry("Depth-of-field autofocus for the object under the crosshair", "Автофокус глубины резкости на объекте под прицелом"),
      Map.entry("Down", "Вниз"),
      Map.entry("Drops", "Капли"),
      Map.entry("Duration", "Длительность"),
      Map.entry("East", "Восток"),
      Map.entry("Effect", "Эффект"),
      Map.entry("Eating Animation", "Анимация еды"),
      Map.entry("Effect Icons", "Иконки эффектов"),
      Map.entry("Effects", "Эффекты"),
      Map.entry("Elements", "Элементы"),
      Map.entry("Elytra Health", "Здоровье для элитр"),
      Map.entry("End Sky Override", "Изменять небо Энда"),
      Map.entry("Equipment", "Экипировка"),
      Map.entry("Equipment Glint", "Блеск экипировки"),
      Map.entry("Facing", "Направление"),
      Map.entry("Fade", "Затухание"),
      Map.entry("Fall Distance", "Высота падения"),
      Map.entry("Fast", "Быстро"),
      Map.entry("Fast EXP", "Быстрый опыт"),
      Map.entry("Faster flight with DRAGON (/fly)", "Ускоренный полёт DRAGON (/fly)"),
      Map.entry("Flight modes", "Режимы полёта"),
      Map.entry("Floating glowing wireframe cubes", "Парящие светящиеся каркасные кубы"),
      Map.entry("Fill", "Заливка"),
      Map.entry("Fill Alpha", "Прозрачность заливки"),
      Map.entry("Fire Resistance", "Огнестойкость"),
      Map.entry("Firework", "Фейерверк"),
      Map.entry("Firework in Offhand", "Фейерверк во второй руке"),
      Map.entry("Firework Key", "Клавиша фейерверка"),
      Map.entry("Firework on Takeoff", "Фейерверк при взлёте"),
      Map.entry("Flash", "Вспышка"),
      Map.entry("First Arrow", "Первая стрела"),
      Map.entry("Fog", "Туман"),
      Map.entry("Fog Blur", "Размытие тумана"),
      Map.entry("Fog Color", "Цвет тумана"),
      Map.entry("Fog Distance", "Дальность тумана"),
      Map.entry("Fog End", "Конец тумана"),
      Map.entry("Fog Start", "Начало тумана"),
      Map.entry("Free", "Свободный"),
      Map.entry("Free camera without moving the server-side player", "Свободная камера без движения серверного игрока"),
      Map.entry("FunTime item binds (no HUD)", "Бинды предметов FunTime без HUD"),
      Map.entry("Friend Color", "Цвет друзей"),
      Map.entry("Friends", "Друзья"),
      Map.entry("Gamma", "Гамма"),
      Map.entry("Glass Color", "Цвет стекла"),
      Map.entry("Glass Trails", "Стеклянные следы"),
      Map.entry("Glow", "Свечение"),
      Map.entry("Glow Strength", "Сила свечения"),
      Map.entry("Grass", "Трава"),
      Map.entry("Gravity", "Гравитация"),
      Map.entry("Grief", "Гриф"),
      Map.entry("Grim bypass", "Обход Grim"),
      Map.entry("Ground Scatter", "Разброс по земле"),
      Map.entry("Hand", "Рука"),
      Map.entry("Hand Blur", "Размытие руки"),
      Map.entry("Hand Fade", "Затухание руки"),
      Map.entry("Hand Softness", "Мягкость руки"),
      Map.entry("Heal Health", "Здоровье для лечения"),
      Map.entry("Healing", "Лечение"),
      Map.entry("Health", "Здоровье"),
      Map.entry("Health with Ball", "Здоровье с шаром"),
      Map.entry("Heavy Obstruction Blocks", "Блоки сильного заглушения"),
      Map.entry("Hide Balance", "Скрывать баланс"),
      Map.entry("Hide Daisies", "Скрывать дейзи"),
      Map.entry("Hide Grief", "Скрывать гриф"),
      Map.entry("Hide IP", "Скрывать IP"),
      Map.entry("Hide Rank", "Скрывать ранг"),
      Map.entry("Hide Nametag Prefixes", "Скрывать префиксы ников"),
      Map.entry("Hide Relics", "Скрывать реликвии"),
      Map.entry("High Reverb Blocks", "Блоки сильной реверберации"),
      Map.entry("Hit through doors", "Бить сквозь двери"),
      Map.entry("Hold", "Удержание"),
      Map.entry("Hold Key", "Клавиша удержания"),
      Map.entry("Ignored Sound Events", "Игнорируемые звуки"),
      Map.entry("Ignore Friends", "Игнорировать друзей"),
      Map.entry("Interface color", "Цвет интерфейса"),
      Map.entry("Highlights the current AttackAura target", "Подсвечивает текущую цель AttackAura"),
      Map.entry(
         "Hides overlays, world effects, sounds, and third-person camera clipping", "Скрывает оверлеи, эффекты мира, звуки и клип камеры от третьего лица"
      ),
      Map.entry("HUD elements", "Элементы HUD"),
      Map.entry("In-game IRC between client users", "Внутриигровой IRC между пользователями клиента"),
      Map.entry("Intensity", "Интенсивность"),
      Map.entry("In World", "В мире"),
      Map.entry("Item", "Предмет"),
      Map.entry("Items", "Предметы"),
      Map.entry("Jump", "Прыжок"),
      Map.entry("Length", "Длина"),
      Map.entry("Left Hand X", "Левая рука X"),
      Map.entry("Left Hand Y", "Левая рука Y"),
      Map.entry("Left Hand Z", "Левая рука Z"),
      Map.entry("Legit", "Легитный"),
      Map.entry("Liquid Filter", "Фильтр жидкости"),
      Map.entry("Low Reverb Blocks", "Блоки слабой реверберации"),
      Map.entry("Linear", "Линейный"),
      Map.entry("Makes dropped items lie naturally on the ground", "Укладывает выброшенные предметы на землю"),
      Map.entry("Local cosmetics ported from the source client", "Локальная косметика, перенесённая из исходного клиента"),
      Map.entry("Minimal glass slivers land and scatter after a totem pop", "Стеклянные осколки разлетаются после срабатывания тотема"),
      Map.entry("Moves forward through blocks", "Перемещает вперёд сквозь блоки"),
      Map.entry("Mask Donation", "Маскировать донат"),
      Map.entry("Marker", "Маркер"),
      Map.entry("Min Balance", "Мин. баланс"),
      Map.entry("Min Distance", "Мин. дистанция"),
      Map.entry("Mode", "Режим"),
      Map.entry("Modes", "Режимы"),
      Map.entry("Mobs", "Мобы"),
      Map.entry("Move", "Движение"),
      Map.entry("Movement", "Движение"),
      Map.entry("Move Count", "Количество движений"),
      Map.entry("Movement correction", "Коррекция движения"),
      Map.entry("New", "Новый"),
      Map.entry("Night", "Ночь"),
      Map.entry("No Cooldown", "Без задержки"),
      Map.entry("No Delay", "Без задержки"),
      Map.entry("None", "Нет"),
      Map.entry("Normal", "Обычный"),
      Map.entry("North", "Север"),
      Map.entry("Notifications", "Уведомления"),
      Map.entry("Obstruction Filter", "Фильтр препятствий"),
      Map.entry("On main thread", "В главном потоке"),
      Map.entry("Opens the Skycore interface", "Открывает интерфейс Skycore"),
      Map.entry(
         "Original underwater, obstruction and cave reverb filters for Minecraft 26.2",
         "Оригинальные фильтры звука под водой, за препятствиями и в пещерах для Minecraft 26.2"
      ),
      Map.entry("Open Shulker", "Открывать шалкер"),
      Map.entry("On Attack", "При атаке"),
      Map.entry("On Move", "При движении"),
      Map.entry("On Throw", "При броске"),
      Map.entry("On Totem", "При тотеме"),
      Map.entry("Only crits", "Только криты"),
      Map.entry("Only During Hit", "Только во время удара"),
      Map.entry("Only If Not Visible", "Только если не видно"),
      Map.entry("Only On Ground", "Только на земле"),
      Map.entry("Only with AttackAura", "Только с AttackAura"),
      Map.entry("Options", "Параметры"),
      Map.entry("Others", "Остальные"),
      Map.entry("Outline Alpha", "Прозрачность обводки"),
      Map.entry("Packet", "Пакетный"),
      Map.entry("Packet Swap Mode", "Режим пакетного свапа"),
      Map.entry("Palette", "Палитра"),
      Map.entry("Particle Glow", "Свечение частиц"),
      Map.entry("Particle Rotation", "Вращение частиц"),
      Map.entry("Particle Size", "Размер частиц"),
      Map.entry("Particle Type", "Тип частиц"),
      Map.entry("Particles", "Частицы"),
      Map.entry("Particles on attacks, projectiles, totems, movement and in the world", "Частицы при атаках, снарядах, тотемах, движении и в мире"),
      Map.entry("Places cobwebs on the target's predicted path", "Ставит паутину на предсказанном пути цели"),
      Map.entry("Plays sounds when modules are toggled", "Воспроизводит звуки при переключении модулей"),
      Map.entry("Prevents dropping items from selected slots", "Не даёт выбрасывать предметы из выбранных слотов"),
      Map.entry("Pet", "Питомец"),
      Map.entry("Players", "Игроки"),
      Map.entry("Player", "Игрок"),
      Map.entry("Prediction", "Предикт"),
      Map.entry("Privilege", "Привилегия"),
      Map.entry("Primary Color", "Основной цвет"),
      Map.entry("Protect Resources", "Беречь ресурсы"),
      Map.entry("Protect Self", "Защищать себя"),
      Map.entry("Protects player names and sensitive scoreboard info", "Скрывает имена игроков и личные данные скорборда"),
      Map.entry("Quickly moves items while holding the mouse button", "Быстро перемещает предметы при удержании кнопки мыши"),
      Map.entry("Quickly swaps elytra and uses fireworks", "Быстро меняет элитры и использует фейерверки"),
      Map.entry("Reads opponent health from the scoreboard", "Считывает здоровье противника из скорборда"),
      Map.entry("ReallyWorld QoL helpers", "Полезные функции для ReallyWorld"),
      Map.entry("Removes cobweb slowdown", "Убирает замедление в паутине"),
      Map.entry("Removes jump delay (not block place)", "Убирает задержку прыжка"),
      Map.entry("Radius", "Радиус"),
      Map.entry("Rage", "Рейдж"),
      Map.entry("Random", "Случайный"),
      Map.entry("Random Order", "Случайный порядок"),
      Map.entry("Ray trace", "Проверка лучом"),
      Map.entry("Release shield", "Отпускать щит"),
      Map.entry("Render factor", "Масштаб интерфейса"),
      Map.entry("Render Predict", "Показывать предикт"),
      Map.entry("Render", "Рендер"),
      Map.entry("Reset Left Hand", "Сбросить левую руку"),
      Map.entry("Reset Right Hand", "Сбросить правую руку"),
      Map.entry("Reverb Filter", "Фильтр реверберации"),
      Map.entry("Right Hand X", "Правая рука X"),
      Map.entry("Right Hand Y", "Правая рука Y"),
      Map.entry("Right Hand Z", "Правая рука Z"),
      Map.entry("Rotation", "Ротация"),
      Map.entry("Rotation behavior", "Поведение ротации"),
      Map.entry("Rotation mode", "Режим ротации"),
      Map.entry("RW wall bypass", "Обход стен RW"),
      Map.entry("Saturation", "Насыщенность"),
      Map.entry("Scale", "Масштаб"),
      Map.entry("Second Arrow", "Вторая стрела"),
      Map.entry("Secondary Color", "Дополнительный цвет"),
      Map.entry("Secondary", "Вторичный"),
      Map.entry("Server Mode", "Режим сервера"),
      Map.entry("Screen motion trail from previous frames", "Шлейф движения из предыдущих кадров"),
      Map.entry("Smooth F5 camera transitions", "Плавные переходы камеры F5"),
      Map.entry("Stops elytra movement near aura targets", "Останавливает движение на элитрах рядом с целями ауры"),
      Map.entry("Swaps configured offhand and helmet items", "Меняет выбранные предметы второй руки и шлема"),
      Map.entry("Swaps two selected arrows in inventory on keybind", "Меняет две выбранные стрелы в инвентаре по клавише"),
      Map.entry("Self", "Себя"),
      Map.entry("Self Protect Radius", "Радиус защиты себя"),
      Map.entry("Shader", "Шейдер"),
      Map.entry("Shader Alpha", "Прозрачность шейдера"),
      Map.entry("Shader Fill", "Заливка шейдера"),
      Map.entry("Shader Glow", "Свечение шейдера"),
      Map.entry("Shader Outline", "Обводка шейдера"),
      Map.entry("Shader Scale", "Масштаб шейдера"),
      Map.entry("Shader Speed", "Скорость шейдера"),
      Map.entry("Shield", "Щит"),
      Map.entry("Shards", "Осколки"),
      Map.entry("Shockwaves", "Ударные волны"),
      Map.entry("Size", "Размер"),
      Map.entry("Skip Friends", "Пропускать друзей"),
      Map.entry("Sky Color Mode", "Режим цвета неба"),
      Map.entry("Slots", "Слоты"),
      Map.entry("Smoke", "Дым"),
      Map.entry("Smart crits", "Умные криты"),
      Map.entry("Smooth Time Cycle", "Плавный цикл времени"),
      Map.entry("Smooth", "Плавный"),
      Map.entry("Soft", "Мягкий"),
      Map.entry("Soft Fade", "Плавное затухание"),
      Map.entry("Softness", "Мягкость"),
      Map.entry("Solid", "Сплошной"),
      Map.entry("Sort Mode", "Сортировка"),
      Map.entry("Sounds", "Звуки"),
      Map.entry("South", "Юг"),
      Map.entry("Spawn On", "Создавать при"),
      Map.entry("Speed", "Скорость"),
      Map.entry("Strength", "Сила"),
      Map.entry("Strict HitVec", "Строгий HitVec"),
      Map.entry("Style", "Стиль"),
      Map.entry("Summer Mode", "Летний режим"),
      Map.entry("Text", "Текст"),
      Map.entry("Tertiary", "Третичный"),
      Map.entry("HUD Background", "Фон HUD"),
      Map.entry("GUI Background", "Фон GUI"),
      Map.entry("Swap Key", "Клавиша свапа"),
      Map.entry("Swap Mode", "Режим свапа"),
      Map.entry("Swing Arm", "Взмах рукой"),
      Map.entry("Swing Power", "Сила взмаха"),
      Map.entry("Swing Smoothness", "Плавность взмаха"),
      Map.entry("Swap Hands", "Менять руки"),
      Map.entry("Sync", "Синхронизация"),
      Map.entry("Target", "Цель"),
      Map.entry("Target Only", "Только цель"),
      Map.entry("Temperature", "Температура"),
      Map.entry("Temperature Amount", "Сила температуры"),
      Map.entry("test", "Тест"),
      Map.entry("Targets", "Цели"),
      Map.entry("Theme", "Тема"),
      Map.entry("Theme Color", "Цвет темы"),
      Map.entry("Third Person Self", "Себя от третьего лица"),
      Map.entry("Through walls", "Сквозь стены"),
      Map.entry("Throw", "Бросок"),
      Map.entry("Throw Count", "Количество бросков"),
      Map.entry("Throw key", "Клавиша броска"),
      Map.entry("Throws a pearl toward the target's pearl landing position", "Бросает жемчуг к месту приземления жемчуга цели"),
      Map.entry("Throws an ender pearl on keybind", "Бросает эндер-жемчуг по клавише"),
      Map.entry("Time of Day", "Время суток"),
      Map.entry("Time Speed", "Скорость времени"),
      Map.entry("Totem", "Тотем"),
      Map.entry("Totem Count", "Количество тотемов"),
      Map.entry("Totems", "Тотемы"),
      Map.entry("TPS Sync", "Синхронизация с TPS"),
      Map.entry("Trail Blur", "Размытие следа"),
      Map.entry("Trail Fade", "Затухание следа"),
      Map.entry("Trail Softness", "Мягкость следа"),
      Map.entry("Transparency", "Прозрачность"),
      Map.entry("Transition", "Переход"),
      Map.entry("Trap bind", "Клавиша ловушки"),
      Map.entry("Type", "Тип"),
      Map.entry("Under self", "Под собой"),
      Map.entry("Up", "Вверх"),
      Map.entry("Use Mode", "Режим использования"),
      Map.entry("Visible in blocks", "Видно в блоках"),
      Map.entry("Volume", "Громкость"),
      Map.entry("Water", "Вода"),
      Map.entry("Weather", "Погода"),
      Map.entry("Weather Control", "Управление погодой"),
      Map.entry("Time, weather, sky, fog, particles, grading, and world lighting", "Время, погода, небо, туман, частицы, цветокоррекция и освещение мира"),
      Map.entry("West", "Запад"),
      Map.entry("Wind", "Ветер"),
      Map.entry("Misc", "Разное"),
      Map.entry("Matrix Bypass", "Обход Matrix"),
      Map.entry("World", "Мир"),
      Map.entry("World Color", "Цвет мира"),
      Map.entry("World Color Mode", "Режим цвета мира"),
      Map.entry("World Count", "Количество в мире")
   );

   public static void setRussian(boolean russian2) {
      russian = russian2;
   }

   public static boolean isRussian() {
      return russian;
   }

   public static boolean selectAndSave(boolean condition) {
      russian = condition;
      if (languageFile == null) {
         return false;
      }

      try {
         Path parent = languageFile.getParent();
         if (parent != null) {
            FileAttribute[] fileAttributeValues = new FileAttribute[0];
            Files.createDirectories(parent, fileAttributeValues);
         }

         JsonObject json = new JsonObject();
         json.addProperty("language", condition ? "Russian" : "English");
         Path languageFile2 = languageFile;
         String text = json.toString();
         OpenOption[] openOptionValues = new OpenOption[0];
         Files.writeString(languageFile2, text, openOptionValues);
         savedSelection = true;
         return true;
      } catch (Exception failure) {
         return false;
      }
   }

   private SelectAndSaveHelper() {
   }

   public static String text(String text) {
      if (russian && text != null && !text.isBlank()) {
         String text2 = RUSSIAN.get(text);
         if (text2 != null) {
            return text2;
         } else {
            text2 = Translations.russian(text);
            if (!text2.equals(text)) {
               return text2;
            } else if (text.startsWith("Toggle ") && text.endsWith(".")) {
               return "Включает или выключает " + text(text.substring(7, text.length() - 1)) + ".";
            } else if (text.startsWith("Adjust ") && text.endsWith(".")) {
               return "Настраивает " + text(text.substring(7, text.length() - 1)) + ".";
            } else if (text.startsWith("Choose one or more ") && text.endsWith(" options.")) {
               return "Выберите один или несколько вариантов: " + text(text.substring(19, text.length() - 9)) + ".";
            } else if (text.startsWith("Choose ") && text.endsWith(".")) {
               return "Выберите " + text(text.substring(7, text.length() - 1)) + ".";
            } else if (text.startsWith("Enter ") && text.endsWith(".")) {
               return "Введите " + text(text.substring(6, text.length() - 1)) + ".";
            } else if (text.startsWith("Press a key for ") && text.endsWith(".")) {
               return "Нажмите клавишу для \"" + text(text.substring(16, text.length() - 1)) + "\".";
            } else if (text.startsWith("Run ") && text.endsWith(".")) {
               return "Выполняет действие \"" + text(text.substring(4, text.length() - 1)) + "\".";
            } else if (text.endsWith(" selected")) {
               return text.substring(0, text.length() - 9) + " выбрано";
            } else {
               return "Configure this option.".equals(text) ? "Настройте этот параметр." : text;
            }
         }
      } else {
         return text;
      }
   }

   public static boolean hasSavedSelection() {
      return savedSelection;
   }

   public static void initialize(Path path) {
      languageFile = path;
      savedSelection = false;
      if (path != null && Files.isRegularFile(path)) {
         try {
            JsonObject asJsonObject = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (!asJsonObject.has("language") || !asJsonObject.get("language").isJsonPrimitive()) {
               return;
            }

            String asString = asJsonObject.get("language").getAsString().trim().toLowerCase(Locale.ROOT);
            if ("russian".equals(asString)) {
               russian = true;
               savedSelection = true;
            } else if ("english".equals(asString)) {
               russian = false;
               savedSelection = true;
            }
         } catch (Exception failure) {
         }
      }
   }

   public static void toggle() {
      int value = !russian ? 1 : 0;
      if (languageFile != null) {
         selectAndSave((value != 0));
      } else {
         russian = (value != 0);
      }
   }
}
