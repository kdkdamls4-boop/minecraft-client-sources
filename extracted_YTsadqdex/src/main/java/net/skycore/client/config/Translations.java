package net.skycore.client.config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class Translations {
   private static final Map<String, String> ENGLISH = Map.ofEntries(
      Map.entry(" добавлен в друзья", " added to friends"),
      Map.entry(" удалён из друзей", " removed from friends"),
      Map.entry("Камера клип", "Camera Clip"),
      Map.entry("AutoPenisBuilder: готово", "AutoPenisBuilder: done"),
      Map.entry("AutoPenisBuilder: нет блоков", "AutoPenisBuilder: no blocks available"),
      Map.entry("Авто", "Auto"),
      Map.entry("Автоматически возрождает после смерти", "Respawns automatically after death"),
      Map.entry("Автоматически выбирает гриферский сервер", "Automatically joins a selected grief server"),
      Map.entry("Автоматически вызывает игрока на дуэль", "Automatically challenges players to duels"),
      Map.entry("Автоматически строит фигуру из пяти блоков", "Automatically builds a five-block shape"),
      Map.entry("Автоматические сообщения после убийства", "Automatically sends a message after a kill"),
      Map.entry("Автоходьба", "Auto Walk"),
      Map.entry("Без кулдауна", "No Cooldown"),
      Map.entry("Без ротации", "No Rotation"),
      Map.entry("Брызги", "Splashes"),
      Map.entry("Быстрый", "Fast"),
      Map.entry("Взрыв по пкм", "Right-Click Explosion"),
      Map.entry("Восток", "East"),
      Map.entry("Впитывай дальше мою хуяку by Skycore 26.2", "Better luck next time — Skycore 26.2"),
      Map.entry("Высота слоя", "Layer Height"),
      Map.entry("Дальний блюр", "Distant Blur"),
      Map.entry("Действия", "Actions"),
      Map.entry("Делать подкоп", "Dig Under Target"),
      Map.entry("Дистанция", "Distance"),
      Map.entry("Дистанция ломания", "Break Distance"),
      Map.entry("Дистанция постановки", "Place Distance"),
      Map.entry("Дистанция цели", "Target Distance"),
      Map.entry("Дождь", "Rain"),
      Map.entry("Друзей", "Friends"),
      Map.entry("Забирает предметы из открытого контейнера", "Takes items from an open container"),
      Map.entry("Задержка", "Delay"),
      Map.entry("Задержка (мс)", "Delay (ms)"),
      Map.entry("Запад", "West"),
      Map.entry("Застройка ног", "Surround Feet"),
      Map.entry("Интенсивность", "Intensity"),
      Map.entry("Исцеление", "Healing"),
      Map.entry("Капли", "Drops"),
      Map.entry("Качание рукой", "Swing Arm"),
      Map.entry("Кит", "Kit"),
      Map.entry("Классик", "Classic"),
      Map.entry("Команда", "Command"),
      Map.entry("Конец тумана", "Fog End"),
      Map.entry("Координаты: X: ", "Coordinates: X: "),
      Map.entry("Коррекция движения", "Movement Correction"),
      Map.entry("Кристаллов/сек", "Crystals per Second"),
      Map.entry("Круги", "Ripples"),
      Map.entry("Кулдаун обсидиана", "Obsidian Cooldown"),
      Map.entry("Лук", "Bow"),
      Map.entry("Лутать", "Pick Up"),
      Map.entry("Макс. урон себе", "Max Self Damage"),
      Map.entry("Мин дистанция", "Min Distance"),
      Map.entry("Мин. HP", "Min HP"),
      Map.entry("Мин. баланс", "Min Balance"),
      Map.entry("Мин. урон по цели", "Min Target Damage"),
      Map.entry("Мод", "Mode"),
      Map.entry("Мягкость", "Softness"),
      Map.entry("Наводит камеру на нужные предметы", "Aims the camera at selected items"),
      Map.entry("Наклон поверхностей", "Surface Slope"),
      Map.entry("Начало тумана", "Fog Start"),
      Map.entry("Не взрывать", "Do Not Detonate"),
      Map.entry("Не взрывать ресурсы", "Protect Resources"),
      Map.entry("Не даёт серверу кикнуть за бездействие", "Prevents idle disconnects"),
      Map.entry("Незерка", "Netherite"),
      Map.entry("Нет", "None"),
      Map.entry("Обход", "Bypass"),
      Map.entry("Объёмный туман", "Volumetric Fog"),
      Map.entry("Отражения", "Reflections"),
      Map.entry("Отражения блоков", "Block Reflections"),
      Map.entry("Отражения воды", "Water Reflections"),
      Map.entry("Отражения неба", "Sky Reflections"),
      Map.entry("Перемещает вперёд сквозь блоки", "Moves forward through blocks"),
      Map.entry("Плотность", "Density"),
      Map.entry("Поворот", "Facing"),
      Map.entry("Подкоп под собой", "Dig Under Self"),
      Map.entry("Позволяет двигаться по поверхности воды", "Allows movement across water"),
      Map.entry("Позволяет ходить с открытым интерфейсом", "Allows movement while a GUI is open"),
      Map.entry("Показывает координаты смерти в чате", "Shows death coordinates in chat"),
      Map.entry("Покрытие", "Coverage"),
      Map.entry("Постоянно", "Always"),
      Map.entry("Пропускать друзей", "Skip Friends"),
      Map.entry("Прыгает, когда Aura находит цель", "Jumps when Aura acquires a target"),
      Map.entry("Прыжки RW", "RW Jumps"),
      Map.entry("Прыжок", "Jump"),
      Map.entry("Режим", "Mode"),
      Map.entry("Ресурсы", "Resources"),
      Map.entry("Ротация", "Rotation"),
      Map.entry("Свободная", "Free"),
      Map.entry("Свой", "Custom"),
      Map.entry("Север", "North"),
      Map.entry("Сила слоя", "Layer Strength"),
      Map.entry("Сияние", "Aurora"),
      Map.entry("Скорость", "Speed"),
      Map.entry("Скорость шейдера", "Aurora Speed"),
      Map.entry("Слоты", "Slots"),
      Map.entry("Случайный порядок", "Random Order"),
      Map.entry("Ставка", "Bet"),
      Map.entry("Сумеречный слой", "Dusk Layer"),
      Map.entry("Сумма", "Amount"),
      Map.entry("Сфокусированная", "Focused"),
      Map.entry("Только в воздухе", "Airborne Only"),
      Map.entry("Только за таргетом", "Target Only"),
      Map.entry("Тотемы", "Totems"),
      Map.entry("Туман, сумеречный слой, отражения и дождь", "Fog, dusk lighting, reflections, and rain"),
      Map.entry("Убирает ботов", "Filters out bots"),
      Map.entry("Убирает урон от падения", "Prevents fall damage"),
      Map.entry("Ускоренный полёт DRAGON (/fly)", "Faster flight with DRAGON (/fly)"),
      Map.entry("Ускоряет ломание блоков", "Breaks blocks faster"),
      Map.entry("Цвет из темы", "Theme Color"),
      Map.entry("Цвет тумана", "Fog Color"),
      Map.entry("Читерский рай", "Cheater Paradise"),
      Map.entry("Шары", "Heads"),
      Map.entry("Шейдерная обводка блока из SkyCoreFree", "Shader-based block outline"),
      Map.entry("Шипы 3", "Thorns III"),
      Map.entry("Щит", "Shield"),
      Map.entry("Элитры", "Elytra"),
      Map.entry("Юг", "South"),
      Map.entry("полетел на спавн хуесос by Skycore 26.2", "Back to spawn — Skycore 26.2")
   );
   private static final Map<String, String> RUSSIAN = createRussianNames();

   public static String english(String text) {
      return text == null ? null : ENGLISH.getOrDefault(text, text);
   }

   public static String russian(String text) {
      return text == null ? null : RUSSIAN.getOrDefault(text, text);
   }

   private static Map<String, String> createRussianNames() {
      HashMap lookup = new HashMap();
      var eNGLISH = ENGLISH;
      BiConsumer<java.lang.String, java.lang.String> biConsumer = (text, text2) -> {
         lookup.putIfAbsent(text2, text);
      };
      eNGLISH.forEach(biConsumer);
      lookup.put("Mode", "Режим");
      return Map.copyOf(lookup);
   }

   private Translations() {
   }
}
