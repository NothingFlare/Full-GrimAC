package ac.grim.grimac.checks;

import ac.grim.grimac.GrimAPI;
import java.util.HashMap;
import java.util.Map;

/**
 * Internationalized descriptions for all checks across 13 world languages.
 * Check technical names (checkName) are intentionally preserved in English.
 */
public final class CheckDescriptions {
    private static final Map<String, Map<String, String>> TRANSLATIONS = new HashMap<>(16);

    static {
        initRU();
        initUK();
        initZH();
        initDE();
        initFR();
        initES();
        initPT();
        initJA();
        initIT();
        initPL();
        initTR();
        initKO();
    }

    private static void initRU() {
        Map<String, String> m = new HashMap<>(140);
        m.put("AimDuplicateLook", "Отправлен дубликат пакета вращения взгляда без изменения направления");
        m.put("AimModulo360", "Отправлен резкий неестественный разворот взгляда (yaw snap)");
        m.put("AirLiquidBreak", "Попытка сломать неразрушаемый блок или жидкость");
        m.put("AirLiquidPlace", "Установка блока на недопустимую или отсутствующую опору");
        m.put("AntiExplosion", "Не получена ожидаемая отдача от взрыва");
        m.put("AntiKB", "Не получено ожидаемое отбрасывание от сущности (AntiKB)");
        m.put("AntiXray", "Добыча скрытой руды без видимости или невозможная пропорция руды к камню");
        m.put("AutoTotem", "Слишком быстрая, автоматическая или невозможная смена тотема в левую руку");
        m.put("BadPacketsA", "Отправлен дубликат выбранного слота хотбара");
        m.put("BadPacketsB", "Игнорирование серверного пакета принудительного разворота взгляда");
        m.put("BadPacketsC", "Попытка проснуться, не находясь в кровати");
        m.put("BadPacketsD", "Отправлен недопустимый угол наклона взгляда вне диапазона от -90 до 90");
        m.put("BadPacketsE", "Отправлено слишком много пакетов движения без изменения позиции");
        m.put("BadPacketsF", "Отправлен дубликат статуса спринта");
        m.put("BadPacketsG", "Отправлен дубликат статуса приседания");
        m.put("BadPacketsH", "Отправлен неожиданный порядковый номер последовательности (sequence id)");
        m.put("BadPacketsI", "Заявление о полете при отсутствии прав на полет");
        m.put("BadPacketsJ", "Поворот взгляда в пакете использования предмета не совпадает с поворотом в тике");
        m.put("BadPacketsK", "Отправлены пакеты наблюдателя не в режиме наблюдателя");
        m.put("BadPacketsL", "Отправлен невозможный пакет копания блока");
        m.put("BadPacketsM", "Попытка возрождения будучи живым");
        m.put("BadPacketsN", "Игнорирование или отказ принять серверную телепортацию");
        m.put("BadPacketsO", "Ответ с идентификатором keepalive, который не отправлялся сервером");
        m.put("BadPacketsP", "Недопустимый пакет клика в окне инвентаря");
        m.put("BadPacketsQ", "Пакет прыжка лошади с недопустимой сущностью, действием или силой");
        m.put("BadPacketsR", "Прекращение отправки позиций при продолжении ответов на транзакции (десинхрон/freeze)");
        m.put("BadPacketsS", "Пакет подтверждения окна помечен как непринятый");
        m.put("BadPacketsU", "Отправлен невозможный пакет использования предмета");
        m.put("BadPacketsV", "Недостаточное перемещение при заявленном движении");
        m.put("BadPacketsX", "Дубликаты действий скрытности или спринта перед следующим пакетом движения");
        m.put("BadPacketsY", "Отправлен номер слота за пределами допустимого диапазона");
        m.put("BadPacketsZ", "Дубликат пакета ввода действий игрока в одном тике клиента");
        m.put("Baritone", "Обнаружено шаблонное автоматизированное поведение бота Baritone");
        m.put("ChatA", "Отправлен пакет автодополнения (Tab) без команды или введенного текста");
        m.put("ChatB", "Недопустимое или искаженное сообщение в чате");
        m.put("ChatC", "Передвижение одновременно с открытым или используемым окном чата");
        m.put("ChatD", "Отправка сообщений при скрытом или отключенном чате");
        m.put("CombatAutoClicker", "Высокий CPS, нулевая задержка между кликами или механическая стабильность кликера");
        m.put("ComparativeAnomaly", "Статистическая аномалия задержек пакетов относительно игроков со схожим пингом");
        m.put("CrashA", "Отправлены координаты за пределами допустимого мира (краш сервера)");
        m.put("CrashB", "Пакеты клика креативного режима без нахождения в креативе");
        m.put("CrashC", "Отправлены бесконечные (Infinity) или неопределенные (NaN) координаты");
        m.put("CrashD", "Клик по недопустимым слотам в окне кафедры");
        m.put("CrashE", "Дистанция прорисовки клиента ниже допустимого минимума");
        m.put("CrashF", "Клик в инвентаре с недопустимой кнопкой или номером слота");
        m.put("CrashG", "Отправлен отрицательный sequence id");
        m.put("CrashH", "Запрос автодополнения Tab с недопустимой или чрезмерной длиной строки");
        m.put("CrashI", "Выбор предмета в мешке (bundle) с недопустимым отрицательным индексом");
        m.put("DuplicateRotPlace", "Повторение идентичных дельт поворота взгляда при установке блоков");
        m.put("ElytraA", "Активация планирования во время уже активного полета на элитрах");
        m.put("ElytraB", "Активация элитр без предварительного прыжка");
        m.put("ElytraC", "Слишком частая повторная активация элитр");
        m.put("ElytraD", "Активация полета без надетой элитры");
        m.put("ElytraE", "Активация элитр во время творческого полета");
        m.put("ElytraF", "Активация элитр при нахождении на земле");
        m.put("ElytraG", "Активация элитр под эффектом левитации");
        m.put("ElytraH", "Активация элитр находясь в транспорте");
        m.put("ElytraI", "Активация элитр при нахождении в воде");
        m.put("EntitySpeed", "Превышение допустимой скорости при столкновении или прохождении сквозь сущностей");
        m.put("ExploitA", "Слишком длинное имя предмета в наковальне");
        m.put("ExploitB", "Недопустимое или вредоносное редактирование книги");
        m.put("FabricatedPlace", "Отправлены координаты клика курсора вне допустимых границ блока");
        m.put("FarBreak", "Разрушение блока на невозможно большом расстоянии");
        m.put("FarPlace", "Установка блока на невозможно большом расстоянии");
        m.put("FastBreak", "Слишком быстрое разрушение блока с превышением скорости ломания");
        m.put("GroundSpoof", "Пакет сообщает о нахождении на земле, когда игрок находится в воздухе");
        m.put("Hitboxes", "Атака сущности за пределами ее реального хитбокса");
        m.put("InvalidBreak", "Отправлен недопустимый идентификатор стороны разрушаемого блока");
        m.put("InvalidInteractCursor", "Вектор взаимодействия с сущностью находится за пределами хитбокса цели");
        m.put("InvalidInteractTarget", "Взаимодействие с несуществующей или недопустимой сущностью");
        m.put("InvalidPlaceCursor", "Отправлена недопустимая позиция курсора при установке блока");
        m.put("InvalidPlaceFace", "Отправлен недопустимый идентификатор стороны при установке блока");
        m.put("InventoryAction", "Взаимодействие с миром или атака одновременно с действиями в инвентаре");
        m.put("InventoryAutoClicker", "Слишком быстрая скорость кликов или неестественная регулярность в инвентаре");
        m.put("KillAuraBot", "Атака невидимой проверочной сущности-бота античита (KillAura)");
        m.put("MultiActionsA", "Атака сущности одновременно с использованием предмета (еда, лук, щит)");
        m.put("MultiActionsB", "Разрушение блоков одновременно с использованием предмета");
        m.put("MultiActionsC", "Клик или перемещение предметов в инвентаре во время бега/движения");
        m.put("MultiActionsD", "Закрытие окна инвентаря во время движения");
        m.put("MultiActionsE", "Взмах рукой одновременно с непрерывным использованием предмета");
        m.put("MultiActionsF", "Взаимодействие с блоком и сущностью в одном и том же тике");
        m.put("MultiActionsG", "Атака или использование предметов во время гребли на лодке");
        m.put("MultiBreak", "Попытка сломать несколько разных блоков за один тик движения");
        m.put("MultiInteractA", "Взаимодействие с несколькими сущностями в одном тике");
        m.put("MultiInteractB", "Отправка нескольких пакетов взаимодействия с разными точками удара за один тик");
        m.put("MultiPlace", "Установка нескольких блоков за один тик");
        m.put("NegativeTimer", "Отправка пакетов движения неестественно медленнее нормальной частоты тиков (замедление времени)");
        m.put("NoFall", "Пакет сообщает о приземлении без фактического контакта с блоком опоры (NoFall)");
        m.put("NoSlow", "Отсутствие замедления при использовании предмета (еда, лук, щит)");
        m.put("NoSwingBreak", "Разрушение блока без отправки пакета анимации взмаха рукой");
        m.put("PacketOrderA", "Пакеты взятия и быстрого перемещения предметов отправлены в недопустимом порядке");
        m.put("PacketOrderB", "Атака без предварительной отправки пакета взмаха рукой (NoSwing)");
        m.put("PacketOrderC", "Пакеты INTERACT и INTERACT_AT сущности отправлены в неверном порядке");
        m.put("PacketOrderD", "Взаимодействие левой рукой отправлено раньше парного взаимодействия правой рукой");
        m.put("PacketOrderE", "Смена слота хотбара во время выполнения конфликтующего действия");
        m.put("PacketOrderF", "Пакеты действий отправлены после ввода приседания или спринта в неверном порядке");
        m.put("PacketOrderG", "Управление хотбаром или инвентарем во время конфликтующего действия");
        m.put("PacketOrderH", "Пакеты изменения спринта и приседания отправлены в недопустимом порядке");
        m.put("PacketOrderI", "Пакеты атаки, использования, отпускания или копания отправлены в неверном порядке тика");
        m.put("PacketOrderJ", "Использование предмета сразу после атаки без обязательного пакета взаимодействия");
        m.put("PacketOrderK", "Открытие, клик или закрытие инвентаря в неверном порядке пакетов");
        m.put("PacketOrderL", "Пакеты выброса, открытия инвентаря или свапа левой руки в неверном порядке");
        m.put("PacketOrderM", "Пакеты использования предмета и взаимодействия с сущностью в неверном порядке");
        m.put("PacketOrderN", "Пакеты использования предмета и установки блока в неверном порядке");
        m.put("PacketOrderO", "Отправка пакетов после движения до ожидаемого завершения тика клиента");
        m.put("PacketOrderP", "Ответ на пакеты чанков в неверном порядке транзакций");
        m.put("Phase", "Прохождение сквозь твердые блоки при движении (Phase/NoClip)");
        m.put("PositionBreakA", "Попытка сломать грань блока из невозможной позиции глаз");
        m.put("PositionBreakB", "Отмена разрушения блока с недопустимой гранью");
        m.put("PositionPlace", "Установка блока на скрытую или невидимую грань");
        m.put("Post", "Отправка пакетов действий вне ожидаемого порядка тика движения");
        m.put("Reach", "Атака сущности на дистанции больше допустимого радиуса удара");
        m.put("RotationBreak", "Попытка сломать блок, не смотря на него");
        m.put("RotationPlace", "Установка блока без наведения взгляда на него");
        m.put("SelfInteract", "Отправлен пакет взаимодействия с самим собой");
        m.put("Simulation", "Движение расходится с предсказанной физической симуляцией сервера");
        m.put("SprintA", "Бег с ускорением при слишком низком уровне сытости (голод)");
        m.put("SprintB", "Бег с ускорением во время приседания или ползания");
        m.put("SprintC", "Бег с ускорением во время непрерывного использования предмета");
        m.put("SprintD", "Начало бега с ускорением под действием эффекта слепоты");
        m.put("SprintE", "Бег с ускорением при прямом столкновении со стеной");
        m.put("SprintF", "Бег с ускорением во время планирования на элитрах");
        m.put("SprintG", "Недопустимый бег с ускорением в воде без достаточного погружения");
        m.put("TickTimer", "Не отправлен пакет завершения тика клиента");
        m.put("Timer", "Игра игрока работает быстрее нормы (ускорение времени / чит Timer)");
        m.put("TimerLimit", "Игрок отправил слишком много накопленных пакетов движения после задержки сети");
        m.put("TransactionOrder", "Ответы на сетевые транзакции или пинг отправлены в неверном порядке");
        m.put("VehicleA", "Невозможные значения управляющего ввода в транспорте");
        m.put("VehicleB", "Заявление о нахождении в транспорте без фактического нахождения в нем");
        m.put("VehicleC", "Движение транспорта расходится с предсказанной физикой управления");
        m.put("VehicleD", "Прыжок на транспорте, который не поддерживает прыжки (лодка/вагонетка)");
        m.put("VehicleE", "Отправка состояния весел лодки без нахождения в лодке");
        m.put("VehicleF", "Отправлены некорректные или невозможные состояния весел лодки");
        m.put("VehicleTimer", "Пакеты движения транспорта отправлены быстрее допустимой частоты тиков");
        m.put("WrongBreak", "Отправлен прогресс разрушения для другого блока, отличного от добываемого");
        TRANSLATIONS.put("ru", m);
    }

    private static void initUK() {
        Map<String, String> m = new HashMap<>(140);
        m.put("AimDuplicateLook", "Надіслано дублікат пакета обертання погляду без зміни напрямку");
        m.put("AimModulo360", "Надіслано різкий неприродний розворот погляду (yaw snap)");
        m.put("AirLiquidBreak", "Спроба зламати неруйнівний блок або рідину");
        m.put("AirLiquidPlace", "Встановлення блоку на неприпустиму або відсутню опору");
        m.put("AntiExplosion", "Не отримано очікуваного відкидання від вибуху");
        m.put("AntiKB", "Не отримано очікуваного відкидання від сутності (AntiKB)");
        m.put("AntiXray", "Видобуток прихованої руди без видимості або неможлива пропорція руди до каменю");
        m.put("AutoTotem", "Занадто швидка, автоматична або неможлива зміна тотема в ліву руку");
        m.put("BadPacketsA", "Надіслано дублікат вибраного слота хотбару");
        m.put("BadPacketsB", "Ігнорування серверного пакета примусового розвороту погляду");
        m.put("BadPacketsC", "Спроба прокинутися, не перебуваючи в ліжку");
        m.put("BadPacketsD", "Надіслано неприпустимий кут нахилу погляду поза діапазоном від -90 до 90");
        m.put("BadPacketsE", "Надіслано занадто багато пакетів руху без зміни позиції");
        m.put("BadPacketsF", "Надіслано дублікат статусу спринту");
        m.put("BadPacketsG", "Надіслано дублікат статусу присідання");
        m.put("BadPacketsH", "Надіслано неочікуваний порядковий номер послідовності (sequence id)");
        m.put("BadPacketsI", "Заява про політ за відсутності прав на політ");
        m.put("BadPacketsJ", "Поворот погляду в пакеті використання предмета не збігається з поворотом у тіку");
        m.put("BadPacketsK", "Надіслано пакети спостерігача не в режимі спостерігача");
        m.put("BadPacketsL", "Надіслано неможливий пакет копання блоку");
        m.put("BadPacketsM", "Спроба відродження будучи живим");
        m.put("BadPacketsN", "Ігнорування або відмова прийняти серверну телепортацію");
        m.put("BadPacketsO", "Відповідь з ідентифікатором keepalive, який не надсилався сервером");
        m.put("BadPacketsP", "Неприпустимий пакет кліку у вікні інвентарю");
        m.put("BadPacketsQ", "Пакет стрибка коня з неприпустимою сутністю, дією або силою");
        m.put("BadPacketsR", "Припинення надсилання позицій при продовженні відповідей на транзакції");
        m.put("BadPacketsS", "Пакет підтвердження вікна позначено як неприйнятий");
        m.put("BadPacketsU", "Надіслано неможливий пакет використання предмета");
        m.put("BadPacketsV", "Недостатнє переміщення при заявленому русі");
        m.put("BadPacketsX", "Дублікати дій присідання або спринту перед наступним пакетом руху");
        m.put("BadPacketsY", "Надіслано номер слота за межами допустимого діапазону");
        m.put("BadPacketsZ", "Дублікат пакета введення дій гравця в одному тіку клієнта");
        m.put("Baritone", "Виявлено шаблонну автоматизовану поведінку бота Baritone");
        m.put("ChatA", "Надіслано пакет автодоповнення (Tab) без команди або введеного тексту");
        m.put("ChatB", "Неприпустиме або спотворене повідомлення в чаті");
        m.put("ChatC", "Пересування одночасно з відкритим чатом");
        m.put("ChatD", "Надсилання повідомлень при прихованому чаті");
        m.put("CombatAutoClicker", "Високий CPS, нульова затримка між кліками або механічна стабільність клікера");
        m.put("ComparativeAnomaly", "Статистична аномалія затримок пакетів відносно гравців зі схожим піном");
        m.put("CrashA", "Надіслано координати за межами допустимого світу (краш сервера)");
        m.put("CrashB", "Пакети кліку творчого режиму без перебування в креативі");
        m.put("CrashC", "Надіслано нескінченні (Infinity) або невизначені (NaN) координати");
        m.put("CrashD", "Клік по неприпустимих слотах у вікні аналоя");
        m.put("CrashE", "Дистанція промальовування клієнта нижче допустимого мінімуму");
        m.put("CrashF", "Клік в інвентарі з неприпустимою кнопкою або номером слота");
        m.put("CrashG", "Надіслано від'ємний sequence id");
        m.put("CrashH", "Запит автодоповнення Tab з неприпустимою або надмірною довжиною рядка");
        m.put("CrashI", "Вибір предмета в мішку (bundle) з неприпустимим від'ємним індексом");
        m.put("DuplicateRotPlace", "Повторення ідентичних дельт повороту погляду при встановленні блоків");
        m.put("ElytraA", "Активація планування під час вже активного польоту на елітрах");
        m.put("ElytraB", "Активація елітр без попереднього стрибка");
        m.put("ElytraC", "Занадто часта повторна активація елітр");
        m.put("ElytraD", "Активація польоту без одягнених елітр");
        m.put("ElytraE", "Активація елітр під час творчого польоту");
        m.put("ElytraF", "Активація елітр при знаходженні на землі");
        m.put("ElytraG", "Активація елітр під ефектом левітації");
        m.put("ElytraH", "Активація елітр перебуваючи в транспорті");
        m.put("ElytraI", "Активація елітр при знаходженні у воді");
        m.put("EntitySpeed", "Перевищення допустимої швидкості при зіткненні або проходженні крізь сутності");
        m.put("ExploitA", "Занадто довга назва предмета в ковадлі");
        m.put("ExploitB", "Неприпустиме або шкідливе редагування книги");
        m.put("FabricatedPlace", "Надіслано координати кліку курсору за межами допустимих меж блоку");
        m.put("FarBreak", "Руйнування блоку на неможливо великій відстані");
        m.put("FarPlace", "Встановлення блоку на неможливо великій відстані");
        m.put("FastBreak", "Занадто швидке руйнування блоку з перевищенням швидкості");
        m.put("GroundSpoof", "Пакет повідомляє про знаходження на землі, коли гравець у повітрі");
        m.put("Hitboxes", "Атака сутності за межами її реального хітбоксу");
        m.put("InvalidBreak", "Надіслано неприпустимий ідентифікатор грані блоку");
        m.put("InvalidInteractCursor", "Вектор взаємодії з сутністю знаходиться за межами хітбоксу цілі");
        m.put("InvalidInteractTarget", "Взаємодія з неіснуючою або неприпустимою сутністю");
        m.put("InvalidPlaceCursor", "Надіслано неприпустиму позицію курсору при встановленні блоку");
        m.put("InvalidPlaceFace", "Надіслано неприпустимий ідентифікатор грані при встановленні блоку");
        m.put("InventoryAction", "Взаємодія зі світом або атака одночасно з діями в інвентарі");
        m.put("InventoryAutoClicker", "Занадто висока швидкість кліків або неприродна регулярність в інвентарі");
        m.put("KillAuraBot", "Атака невидимої перевірочної сутності-бота античіта (KillAura)");
        m.put("MultiActionsA", "Атака сутності одночасно з використанням предмета (їжа, лук, щит)");
        m.put("MultiActionsB", "Руйнування блоків одночасно з використанням предмета");
        m.put("MultiActionsC", "Клік або переміщення предметів в інвентарі під час бігу/руху");
        m.put("MultiActionsD", "Закриття вікна інвентарю під час руху");
        m.put("MultiActionsE", "Помах рукою одночасно з безперервним використанням предмета");
        m.put("MultiActionsF", "Взаємодія з блоком і сутністю в одному і тому ж тіку");
        m.put("MultiActionsG", "Атака або використання предметів під час веслування на човні");
        m.put("MultiBreak", "Спроба зламати кілька різних блоків за один тік руху");
        m.put("MultiInteractA", "Взаємодія з кількома сутностями в одному тіку");
        m.put("MultiInteractB", "Надсилання кількох пакетів взаємодії з різними точками удару за один тік");
        m.put("MultiPlace", "Встановлення кількох блоків за один тік");
        m.put("NegativeTimer", "Надсилання пакетів руху неприродно повільніше нормальної частоти тіків");
        m.put("NoFall", "Пакет повідомляє про приземлення без фактичного контакту з опорою (NoFall)");
        m.put("NoSlow", "Відсутність уповільнення при використанні предмета (їжа, лук, щит)");
        m.put("NoSwingBreak", "Руйнування блоку без надсилання пакета анімації помаху рукою");
        m.put("PacketOrderA", "Пакети взяття і швидкого переміщення предметів надіслані в неприпустимому порядку");
        m.put("PacketOrderB", "Атака без попереднього надсилання пакета помаху рукою (NoSwing)");
        m.put("PacketOrderC", "Пакети INTERACT і INTERACT_AT сутності надіслані в неправильному порядку");
        m.put("PacketOrderD", "Взаємодія лівою рукою надіслана раніше за праву руку");
        m.put("PacketOrderE", "Зміна слота хотбару під час виконання конфліктної дії");
        m.put("PacketOrderF", "Пакети дій надіслані після присідання або спринту в невірному порядку");
        m.put("PacketOrderG", "Керування хотбаром або інвентарем під час конфліктної дії");
        m.put("PacketOrderH", "Пакети зміни спринту і присідання надіслані в неприпустимому порядку");
        m.put("PacketOrderI", "Пакети атаки, використання, відпускання або копання надіслані в невірному порядку");
        m.put("PacketOrderJ", "Використання предмета одразу після атаки без пакета взаємодії");
        m.put("PacketOrderK", "Відкриття, клік або закриття інвентарю в невірному порядку пакетів");
        m.put("PacketOrderL", "Пакети викидання, відкриття інвентарю або свапу лівої руки в невірному порядку");
        m.put("PacketOrderM", "Пакети використання предмета і взаємодії з сутністю в невірному порядку");
        m.put("PacketOrderN", "Пакети використання предмета і встановлення блоку в невірному порядку");
        m.put("PacketOrderO", "Надсилання пакетів після руху до завершення тіка клієнта");
        m.put("PacketOrderP", "Відповідь на пакети чанків у невірному порядку транзакцій");
        m.put("Phase", "Проходження крізь тверді блоки під час руху (Phase/NoClip)");
        m.put("PositionBreakA", "Спроба зламати грань блоку з неможливої позиції очей");
        m.put("PositionBreakB", "Скасування руйнування блоку з неприпустимою гранню");
        m.put("PositionPlace", "Встановлення блоку на приховану або невидиму грань");
        m.put("Post", "Надсилання пакетів дій поза очікуваним порядком тіка руху");
        m.put("Reach", "Атака сутності на дистанції більше допустимого радіуса удару");
        m.put("RotationBreak", "Спроба зламати блок, не дивлячись на нього");
        m.put("RotationPlace", "Встановлення блоку без наведення погляду на нього");
        m.put("SelfInteract", "Надіслано пакет взаємодії з самим собою");
        m.put("Simulation", "Рух розходиться з передбаченою фізичною симуляцією сервера");
        m.put("SprintA", "Біг з прискоренням при занадто низькому рівні ситості (голод)");
        m.put("SprintB", "Біг з прискоренням під час присідання або повзання");
        m.put("SprintC", "Біг з прискоренням під час безперервного використання предмета");
        m.put("SprintD", "Початок бігу з прискоренням під дією ефекту сліпоти");
        m.put("SprintE", "Біг з прискоренням при прямому зіткненні зі стіною");
        m.put("SprintF", "Біг з прискоренням під час планування на елітрах");
        m.put("SprintG", "Неприпустимий біг з прискоренням у воді без достатнього занурення");
        m.put("TickTimer", "Не надіслано пакет завершення тіка клієнта");
        m.put("Timer", "Гра гравця працює швидше за норму (прискорення часу / чіт Timer)");
        m.put("TimerLimit", "Гравець надіслав занадто багато накопичених пакетів руху після затримки мережі");
        m.put("TransactionOrder", "Відповіді на мережеві транзакції або пінг надіслані в неправильному порядку");
        m.put("VehicleA", "Неможливі значення керуючого введення в транспорті");
        m.put("VehicleB", "Заява про знаходження в транспорті без фактичного перебування в ньому");
        m.put("VehicleC", "Рух транспорту розходиться з передбаченою фізикою керування");
        m.put("VehicleD", "Стрибок на транспорті, який не підтримує стрибки (човен/вагонетка)");
        m.put("VehicleE", "Надсилання стану весел човна без перебування в човні");
        m.put("VehicleF", "Надіслано некоректні або неможливі стани весел човна");
        m.put("VehicleTimer", "Пакети руху транспорту надіслані швидше допустимої частоти тіків");
        m.put("WrongBreak", "Надіслано прогрес руйнування для іншого блоку, відмінного від видобуваного");
        TRANSLATIONS.put("uk", m);
    }

    private static void initZH() {
        Map<String, String> m = new HashMap<>(140);
        m.put("AimDuplicateLook", "发送了重复的视角旋转数据包但视角未发生改变");
        m.put("AimModulo360", "发送了异常的大角度偏航跳变 (yaw snap)");
        m.put("AirLiquidBreak", "尝试破坏不可破坏的方块或流体");
        m.put("AirLiquidPlace", "在无效的支撑面或空中放置方块");
        m.put("AntiExplosion", "未受到预期的爆炸击退");
        m.put("AntiKB", "未受到预期的实体击退 (AntiKB)");
        m.put("AntiXray", "挖掘未暴露的隐藏矿石或非正常的矿石挖掘比例");
        m.put("AutoTotem", "异常瞬时或机械化地将不死图腾放入副手");
        m.put("BadPacketsA", "发送了重复的快捷栏槽位选择包");
        m.put("BadPacketsB", "忽略了服务端强制指定的视角旋转数据包");
        m.put("BadPacketsC", "在未处于睡眠状态时尝试发送起床数据包");
        m.put("BadPacketsD", "视角俯仰角超出 -90° 到 90° 的有效物理范围");
        m.put("BadPacketsE", "未发生位移却发送了过多无效的移动数据包");
        m.put("BadPacketsF", "发送了重复的冲刺状态数据包");
        m.put("BadPacketsG", "发送了重复的潜行状态数据包");
        m.put("BadPacketsH", "发送了异常或跳跃的序列号 ID (sequence id)");
        m.put("BadPacketsI", "在没有飞行权限的情况下声称处于飞行状态");
        m.put("BadPacketsJ", "使用物品包中的视角朝向与当前刻的视角朝向不一致");
        m.put("BadPacketsK", "在非旁观模式下发送了旁观者数据包");
        m.put("BadPacketsL", "发送了物理不可能发生的方块开采数据包");
        m.put("BadPacketsM", "在存活状态下尝试发送重生数据包");
        m.put("BadPacketsN", "忽略或拒绝确认服务端强制执行的传送数据包");
        m.put("BadPacketsO", "响应了并非由服务端发出的 KeepAlive 保活 ID");
        m.put("BadPacketsP", "窗口点击数据包参数非法或状态不匹配");
        m.put("BadPacketsQ", "发送了带有非法实体、动作或力度的马匹跳跃数据包");
        m.put("BadPacketsR", "停止发送位置更新却仍在响应事务确认包 (Freeze/假卡顿)");
        m.put("BadPacketsS", "窗口确认数据包被标记为未接受");
        m.put("BadPacketsU", "发送了物理不可能发生的物品使用数据包");
        m.put("BadPacketsV", "位移距离低于物理微量移动下限");
        m.put("BadPacketsX", "在下一个移动数据包之前发送了重复的潜行或冲刺动作");
        m.put("BadPacketsY", "发送的物品槽位 ID 超出了背包有效边界");
        m.put("BadPacketsZ", "在同一个客户端刻内发送了重复的玩家输入控制包");
        m.put("Baritone", "检测到类似 Baritone 自动寻路机器人的行为模式");
        m.put("ChatA", "发送了不含任何命令或文本的 Tab 自动补全请求");
        m.put("ChatB", "发送了格式非法或包含异常字符的聊天消息");
        m.put("ChatC", "在聊天输入过程中进行非正常的同步移动");
        m.put("ChatD", "在客户端设置隐藏聊天的情况下发送聊天消息");
        m.put("CombatAutoClicker", "战斗中超高 CPS、零延迟或过于机械稳定的自动连点");
        m.put("ComparativeAnomaly", "数据包延迟模式与同服同延迟群体存在统计学异常偏差");
        m.put("CrashA", "发送了超出合法世界边界的位置坐标 (试图崩溃服务端)");
        m.put("CrashB", "在非创造模式下发送创造模式专属物品交互包");
        m.put("CrashC", "发送了非有限数 (NaN/Infinity) 的位置或视角坐标");
        m.put("CrashD", "点击讲台界面中的非法槽位 (试图崩溃服务端)");
        m.put("CrashE", "客户端视距设置低于服务器允许的最小限制");
        m.put("CrashF", "发送了带有非法按键或槽位数值的背包点击包");
        m.put("CrashG", "发送了负数的序列号 ID (negative sequence id)");
        m.put("CrashH", "发送了超长或异常长度的 Tab 自动补全请求");
        m.put("CrashI", "收纳袋物品选择使用了非法的负数槽位索引");
        m.put("DuplicateRotPlace", "放置方块时重复完全相同的视角旋转增量");
        m.put("ElytraA", "在已处于滑翔状态下再次尝试激活滑翔");
        m.put("ElytraB", "在没有起跳的情况下直接开启鞘翅滑翔");
        m.put("ElytraC", "过高频率反复切换触发鞘翅滑翔");
        m.put("ElytraD", "在未装备鞘翅的情况下尝试开启滑翔");
        m.put("ElytraE", "在飞行模式下尝试激活鞘翅滑翔");
        m.put("ElytraF", "在双脚着地处于地面时开启滑翔");
        m.put("ElytraG", "在处于漂浮效果时尝试激活鞘翅滑翔");
        m.put("ElytraH", "在乘坐载具（矿车、船等）时尝试激活鞘翅滑翔");
        m.put("ElytraI", "在水中尝试激活鞘翅滑翔");
        m.put("EntitySpeed", "与实体碰撞或穿过实体时移动速度超过物理允许上限");
        m.put("ExploitA", "铁砧中物品重命名长度超出有效限制");
        m.put("ExploitB", "非法的书本内容编辑操作");
        m.put("FabricatedPlace", "发送了超出方块边界的光标放置坐标");
        m.put("FarBreak", "破坏方块的距离超过玩家正常可达距离");
        m.put("FarPlace", "放置方块的距离超过玩家正常可达距离");
        m.put("FastBreak", "方块破坏速度超过物理/附魔允许的极限 (FastBreak)");
        m.put("GroundSpoof", "数据包声称在地面上但实际处于腾空状态 (GroundSpoof)");
        m.put("Hitboxes", "攻击位置位于实体实际有效碰撞箱范围之外");
        m.put("InvalidBreak", "发送了不可能存在的方块破坏面 ID");
        m.put("InvalidInteractCursor", "与实体交互的点击向量位于目标实体碰撞箱之外");
        m.put("InvalidInteractTarget", "尝试与不存在或无效的实体进行交互");
        m.put("InvalidPlaceCursor", "放置方块时发送了无效的光标相对位置");
        m.put("InvalidPlaceFace", "放置方块时发送了不可能存在的方块面 ID");
        m.put("InventoryAction", "在操作背包界面的同时在世界中进行交互或发起攻击");
        m.put("InventoryAutoClicker", "背包界面中点击速度过快或呈现机械化规律 (InventoryAutoClicker)");
        m.put("KillAuraBot", "攻击了反作弊生成的隐形 KillAura 诱饵实体");
        m.put("MultiActionsA", "在使用物品（吃东西、拉弓、格挡等）的同时发起攻击");
        m.put("MultiActionsB", "在使用物品的同时破坏方块");
        m.put("MultiActionsC", "在跑动或正常移动时在背包中点击物品");
        m.put("MultiActionsD", "在移动的同时关闭背包界面");
        m.put("MultiActionsE", "在使用物品的同时进行挥手动作");
        m.put("MultiActionsF", "在同一个游戏刻内同时与方块和实体进行交互");
        m.put("MultiActionsG", "在划船的同时发起攻击或使用物品");
        m.put("MultiBreak", "在单个移动刻内尝试破坏多个不同的方块");
        m.put("MultiInteractA", "在同一个游戏刻内与多个实体进行交互");
        m.put("MultiInteractB", "在单个游戏刻内发送了带有不同击中坐标的多个实体交互包");
        m.put("MultiPlace", "在单个游戏刻内放置了多个方块");
        m.put("NegativeTimer", "移动数据包发送频率明显低于正常客户端刻速率 (NegativeTimer)");
        m.put("NoFall", "未接触地面却向服务器谎报在地面以规避摔落伤害 (NoFall)");
        m.put("NoSlow", "在使用物品（吃东西、拉弓、格挡）时未受到应有的减速 (NoSlow)");
        m.put("NoSwingBreak", "破坏方块时未发送手臂挥动动画数据包");
        m.put("PacketOrderA", "拾取与快捷移动物品的数据包顺序非法");
        m.put("PacketOrderB", "在发起实体攻击前未发送手臂挥动动画数据包 (NoSwing)");
        m.put("PacketOrderC", "实体交互包 INTERACT 与 INTERACT_AT 发送顺序颠倒");
        m.put("PacketOrderD", "副手实体交互包早于对应的主手交互包发送");
        m.put("PacketOrderE", "在执行冲突动作期间切换了手持物品槽位");
        m.put("PacketOrderF", "在潜行或冲刺输入后以非法顺序发送了动作数据包");
        m.put("PacketOrderG", "在执行冲突动作的同时进行快捷栏或背包管理");
        m.put("PacketOrderH", "冲刺与潜行状态变更数据包以非法的顺序交替发送");
        m.put("PacketOrderI", "战斗、使用物品、释放或开采数据包未按正常的刻周期顺序发送");
        m.put("PacketOrderJ", "攻击后直接发送使用物品包，缺少预期的实体交互数据包");
        m.put("PacketOrderK", "打开、点击或关闭背包界面的数据包顺序错误");
        m.put("PacketOrderL", "丢弃物品、打开背包或副手切换数据包顺序异常");
        m.put("PacketOrderM", "使用物品与实体交互的数据包发送顺序颠倒");
        m.put("PacketOrderN", "使用物品与方块放置的数据包发送顺序颠倒");
        m.put("PacketOrderO", "在客户端刻正常结束之前在移动后发送了异常数据包");
        m.put("PacketOrderP", "区块批处理确认包的事务响应顺序异常");
        m.put("Phase", "移动预测期间进入了实体方块内部 (Phase/穿墙)");
        m.put("PositionBreakA", "从不可能的视线起点位置尝试破坏方块表面");
        m.put("PositionBreakB", "以无效的方块表面标识取消破坏方块");
        m.put("PositionPlace", "尝试将方块放置在被遮挡完全不可见的方块面上");
        m.put("Post", "动作数据包发送顺序不符合正常的移动周期顺序");
        m.put("Reach", "攻击实体的有效距离超过最大允许攻击距离 (Reach)");
        m.put("RotationBreak", "在未正视方块的情况下尝试破坏该方块");
        m.put("RotationPlace", "在未正视方块目标的情况下放置方块");
        m.put("SelfInteract", "尝试与玩家自身实体进行交互");
        m.put("Simulation", "玩家实际移动轨迹与服务端完整物理模拟预测存在偏差");
        m.put("SprintA", "在饥饿值过低无法奔跑时进行冲刺");
        m.put("SprintB", "在潜行或匍匐状态下进行冲刺");
        m.put("SprintC", "在使用物品（吃东西、拉弓、格挡）的同时进行冲刺");
        m.put("SprintD", "在处于失明药水状态下开启冲刺");
        m.put("SprintE", "在正面与墙壁碰撞受阻时持续冲刺");
        m.put("SprintF", "在使用鞘翅滑翔的同时保持冲刺状态");
        m.put("SprintG", "在水中非正常保持陆地冲刺状态");
        m.put("TickTimer", "未发送客户端刻结束确认数据包 (TickEnd)");
        m.put("Timer", "游戏客户端运行频率快于正常刻速率 (Timer 加速)");
        m.put("TimerLimit", "在高延迟后突发发送了超出合理上限的移动数据包");
        m.put("TransactionOrder", "网络事务确认或 Ping 响应包的返回顺序错误");
        m.put("VehicleA", "载具控制输入参数非法超出物理范围");
        m.put("VehicleB", "未乘坐载具却向服务器发送处于载具中的数据包");
        m.put("VehicleC", "载具移动轨迹与服务端物理控制预测不相符");
        m.put("VehicleD", "在不可跳跃的载具（船、矿车等）上执行跳跃动作");
        m.put("VehicleE", "在未乘坐船只的情况下发送船桨划动状态包");
        m.put("VehicleF", "发送了物理不可能的船桨划水状态");
        m.put("VehicleTimer", "载具移动数据包发送速率快于正常的客户端刻频率 (VehicleTimer)");
        m.put("WrongBreak", "发送的挖掘进度对应的方块与当前正在破坏的方块不一致");
        TRANSLATIONS.put("zh", m);
    }

    private static void initDE() {
        Map<String, String> m = new HashMap<>(140);
        m.put("AimDuplicateLook", "Doppeltes Rotationspaket ohne Blickrichtungsänderung gesendet");
        m.put("AimModulo360", "Unerwartet abrupte Drehung der Blickrichtung (yaw snap)");
        m.put("AirLiquidBreak", "Versuch, einen unzerstörbaren Block oder eine Flüssigkeit abzubauen");
        m.put("AirLiquidPlace", "Platzieren eines Blocks an einer ungültigen Stütze");
        m.put("AntiExplosion", "Erwarteter Explosions-Rückstoß wurde nicht empfangen");
        m.put("AntiKB", "Erwarteter Einheiten-Rückstoß wurde ignoriert (AntiKB)");
        m.put("AntiXray", "Abbau verdeckter Erze oder unmögliches Erz-zu-Stein-Verhältnis");
        m.put("AutoTotem", "Automatisches oder unmöglich schnelles Ausrüsten eines Totems in der Zweithand");
        m.put("BadPacketsA", "Doppelte Slot-ID gesendet");
        m.put("BadPacketsB", "Vom Server vorgegebenes Rotationspaket ignoriert");
        m.put("BadPacketsC", "Versuch aufzuwachen, ohne zu schlafen");
        m.put("BadPacketsD", "Ungültiger Blickwinkel außerhalb des Bereichs von -90 bis 90 Grad");
        m.put("BadPacketsE", "Zu viele Bewegungspakete ohne Positionsänderung gesendet");
        m.put("BadPacketsF", "Doppelter Sprint-Status gesendet");
        m.put("BadPacketsG", "Doppelter Schleich-Status gesendet");
        m.put("BadPacketsH", "Unerwartete Sequenz-ID gesendet");
        m.put("BadPacketsI", "Flugstatus angegeben, obwohl Fliegen nicht erlaubt ist");
        m.put("BadPacketsJ", "Rotation im Item-Benutzungs-Paket stimmt nicht mit Tick-Rotation überein");
        m.put("BadPacketsK", "Zuschauer-Pakete gesendet, ohne im Zuschauermodus zu sein");
        m.put("BadPacketsL", "Unmögliches Blockabbau-Paket gesendet");
        m.put("BadPacketsM", "Versuch wiederzubeleben, während man noch am Leben ist");
        m.put("BadPacketsN", "Erforderliche Server-Teleportation ignoriert oder abgelehnt");
        m.put("BadPacketsO", "Keepalive-Antwort mit nicht vom Server gesendeter ID");
        m.put("BadPacketsP", "Ungültiges Fensterklick-Paket gesendet");
        m.put("BadPacketsQ", "Ungültiges Pferdesprung-Paket gesendet");
        m.put("BadPacketsR", "Positionspakete eingestellt, während Transaktionen weiter beantwortet werden");
        m.put("BadPacketsS", "Fenster-Bestätigungspaket als nicht akzeptiert markiert");
        m.put("BadPacketsU", "Unmögliches Gegenstandsbenutzungs-Paket gesendet");
        m.put("BadPacketsV", "Nicht weit genug bewegt für angegebenes Bewegungspaket");
        m.put("BadPacketsX", "Doppelte Schleich- oder Sprint-Eingaben vor nächstem Bewegungspaket");
        m.put("BadPacketsY", "Slot-ID außerhalb des gültigen Bereichs gesendet");
        m.put("BadPacketsZ", "Doppelte Eingabepakete im selben Client-Tick");
        m.put("Baritone", "Automatisierte Baritone-Bot-Bewegungsmuster erkannt");
        m.put("ChatA", "Tab-Vervollständigungspaket ohne Befehl oder Text gesendet");
        m.put("ChatB", "Ungültige Chatnachricht gesendet");
        m.put("ChatC", "Bewegung während des Chattens");
        m.put("ChatD", "Chatten bei ausgeblendetem Chat");
        m.put("CombatAutoClicker", "Hohe Klickrate (CPS), Nullverzögerung oder roboterhafte Klick-Konsistenz");
        m.put("ComparativeAnomaly", "Statistische Musterabweichung im Vergleich zu Spielern mit ähnlichem Ping");
        m.put("CrashA", "Position außerhalb der gültigen Weltgrenzen gesendet");
        m.put("CrashB", "Kreativmodus-Pakete ohne Kreativmodus gesendet");
        m.put("CrashC", "Nicht-endliche Position oder Rotation gesendet (NaN/Infinity)");
        m.put("CrashD", "Ungültige Slot-Klicks im Lesepult-Fenster");
        m.put("CrashE", "Sichtweite des Clients unter dem erlaubten Mindestwert");
        m.put("CrashF", "Inventarklick mit ungültigem Button- oder Slot-Wert");
        m.put("CrashG", "Negative Sequenz-ID gesendet");
        m.put("CrashH", "Tab-Vervollständigungsanfrage mit ungültiger oder übermäßiger Länge");
        m.put("CrashI", "Bündel-Gegenstandsauswahl mit ungültigem negativem Index");
        m.put("DuplicateRotPlace", "Wiederholung identischer Blickrotations-Deltas beim Blocksetzen");
        m.put("ElytraA", "Gleitflug gestartet, während man bereits gleitet");
        m.put("ElytraB", "Elytra-Flug ohne vorherigen Sprung gestartet");
        m.put("ElytraC", "Zu häufiges Ein- und Ausschalten des Elytra-Flugs");
        m.put("ElytraD", "Gleitflug gestartet, ohne eine Elytra ausgerüstet zu haben");
        m.put("ElytraE", "Elytra gestartet während normalem Flugmodus");
        m.put("ElytraF", "Elytra gestartet, während man auf dem Boden steht");
        m.put("ElytraG", "Elytra gestartet unter dem Einfluss von Schwebekraft (Levitation)");
        m.put("ElytraH", "Elytra gestartet, während man in einem Fahrzeug sitzt");
        m.put("ElytraI", "Elytra gestartet, während man sich im Wasser befindet");
        m.put("EntitySpeed", "Unerlaubt hohe Geschwindigkeit beim Zusammenstoß mit Einheiten");
        m.put("ExploitA", "Zu langer Gegenstandsname im Amboss");
        m.put("ExploitB", "Ungültige oder böswillige Buchbearbeitung");
        m.put("FabricatedPlace", "Mauszeigerposition beim Platzieren außerhalb der Blockgrenzen");
        m.put("FarBreak", "Block aus zu großer Entfernung abgebaut");
        m.put("FarPlace", "Block aus zu großer Entfernung platziert");
        m.put("FastBreak", "Block schneller abgebaut als physikalisch möglich (FastBreak)");
        m.put("GroundSpoof", "Paket meldet Bodenkontakt, obwohl der Spieler in der Luft ist");
        m.put("Hitboxes", "Angriff außerhalb der gültigen Hitbox der Einheit");
        m.put("InvalidBreak", "Ungültige Blockseiten-ID beim Abbauen gesendet");
        m.put("InvalidInteractCursor", "Interaktionsvektor liegt außerhalb der Hitbox des Ziels");
        m.put("InvalidInteractTarget", "Interaktion mit einer nicht existierenden Einheit");
        m.put("InvalidPlaceCursor", "Ungültige Cursor-Position beim Platzieren eines Blocks");
        m.put("InvalidPlaceFace", "Ungültige Blockseiten-ID beim Platzieren gesendet");
        m.put("InventoryAction", "Interaktion in der Welt oder Angriff während offener Inventarinteraktion");
        m.put("InventoryAutoClicker", "Zu hohe Klickrate oder roboterhafte Regelmäßigkeit im Inventar");
        m.put("KillAuraBot", "Unsichtbare Anti-KillAura-Bot-Einheit angegriffen");
        m.put("MultiActionsA", "Angriff während der Benutzung eines Gegenstands (Essen, Bogen, Schild)");
        m.put("MultiActionsB", "Blockabbau während der Benutzung eines Gegenstands");
        m.put("MultiActionsC", "Klick im Inventar während der Bewegung");
        m.put("MultiActionsD", "Inventar während der Bewegung geschlossen");
        m.put("MultiActionsE", "Arm geschwungen während der Benutzung eines Gegenstands");
        m.put("MultiActionsF", "Gleichzeitige Interaktion mit Block und Einheit im selben Tick");
        m.put("MultiActionsG", "Angriff oder Gegenstandsbenutzung beim Rudern eines Bootes");
        m.put("MultiBreak", "Versuch, mehrere verschiedene Blöcke in einem Tick abzubauen");
        m.put("MultiInteractA", "Interaktion mit mehreren Einheiten im selben Tick");
        m.put("MultiInteractB", "Mehrere Einheiten-Interaktionen mit unterschiedlichen Trefferpunkten in einem Tick");
        m.put("MultiPlace", "Mehrere Blöcke in einem einzelnen Tick platziert");
        m.put("NegativeTimer", "Bewegungspakete deutlich langsamer als die normale Tickrate gesendet");
        m.put("NoFall", "Bodenkontakt gemeldet, ohne tatsächlich den Boden zu berühren (NoFall)");
        m.put("NoSlow", "Keine Verlangsamung bei der Benutzung eines Gegenstands (NoSlow)");
        m.put("NoSwingBreak", "Block ohne Handschwung-Animation abgebaut");
        m.put("PacketOrderA", "Ungültige Reihenfolge von Aufnahme- und Schnellklick-Paketen");
        m.put("PacketOrderB", "Angriff ohne vorherigen Handschwung (NoSwing)");
        m.put("PacketOrderC", "INTERACT- und INTERACT_AT-Pakete in falscher Reihenfolge");
        m.put("PacketOrderD", "Zweithand-Interaktion vor Haupthand-Interaktion gesendet");
        m.put("PacketOrderE", "Slot gewechselt während einer konfliktbehafteten Aktion");
        m.put("PacketOrderF", "Aktionspakete nach Schleich- oder Sprint-Eingabe in falscher Reihenfolge");
        m.put("PacketOrderG", "Inventarverwaltung während einer konfliktbehafteten Aktion");
        m.put("PacketOrderH", "Sprint- und Schleich-Zustandsänderungen in ungültiger Reihenfolge");
        m.put("PacketOrderI", "Kampf-, Benutzungs- oder Abbaupakete in ungültiger Tick-Reihenfolge");
        m.put("PacketOrderJ", "Item-Benutzung nach Angriff ohne erwartetes Interaktionspaket");
        m.put("PacketOrderK", "Öffnen, Klicken oder Schließen des Inventars in falscher Reihenfolge");
        m.put("PacketOrderL", "Gegenstand abwerfen, Inventar öffnen oder Handtausch in falscher Reihenfolge");
        m.put("PacketOrderM", "Item-Benutzung und Einheiten-Interaktion in falscher Reihenfolge");
        m.put("PacketOrderN", "Item-Benutzung und Block-Platzierung in falscher Reihenfolge");
        m.put("PacketOrderO", "Pakete nach Bewegung vor dem erwarteten Client-Tick-Ende gesendet");
        m.put("PacketOrderP", "Antwort auf Chunk-Batch-Pakete in ungültiger Transaktionsreihenfolge");
        m.put("Phase", "Bewegung durch solide Blöcke (Phase)");
        m.put("PositionBreakA", "Versuch, eine Blockseite aus unmöglicher Augenposition abzubauen");
        m.put("PositionBreakB", "Blockabbau mit ungültiger Blockseite abgebrochen");
        m.put("PositionPlace", "Block an einer verdeckten Seite platziert");
        m.put("Post", "Pakete außerhalb der erwarteten Tick-Reihenfolge gesendet");
        m.put("Reach", "Einheit aus zu großer Reichweite angegriffen (Reach)");
        m.put("RotationBreak", "Versuch, einen Block abzubauen, ohne ihn anzusehen");
        m.put("RotationPlace", "Block platziert, ohne ihn anzusehen");
        m.put("SelfInteract", "Interaktion mit sich selbst");
        m.put("Simulation", "Bewegung weicht von der physikalischen Serversimulation ab");
        m.put("SprintA", "Sprinten trotz zu geringem Hungerwert");
        m.put("SprintB", "Sprinten beim Schleichen oder Kriechen");
        m.put("SprintC", "Sprinten während der Benutzung eines Gegenstands");
        m.put("SprintD", "Sprinten begonnen trotz Blindheitseffekt");
        m.put("SprintE", "Sprinten bei Kollision mit einer Wand");
        m.put("SprintF", "Sprinten während des Gleitens mit der Elytra");
        m.put("SprintG", "Sprinten im Wasser ohne Schwimmen");
        m.put("TickTimer", "Paket zum Ende des Client-Ticks nicht gesendet");
        m.put("Timer", "Das Spiel des Spielers läuft schneller als normal (Timer)");
        m.put("TimerLimit", "Zu viele gepufferte Pakete nach hoher Latenz gesendet");
        m.put("TransactionOrder", "Transaktions- oder Ping-Antworten in falscher Reihenfolge gesendet");
        m.put("VehicleA", "Unmögliche Eingabewerte bei der Fahrzeugsteuerung");
        m.put("VehicleB", "Fahrzeugstatus gemeldet, ohne sich in einem Fahrzeug zu befinden");
        m.put("VehicleC", "Fahrzeugbewegung stimmt nicht mit der physikalischen Steuerung überein");
        m.put("VehicleD", "Sprung in einem Fahrzeug ausgeführt, das nicht springen kann");
        m.put("VehicleE", "Bootspaddel-Status gesendet, ohne in einem Boot zu sitzen");
        m.put("VehicleF", "Falsche oder unmögliche Bootspaddel-Zustände gesendet");
        m.put("VehicleTimer", "Fahrzeug-Bewegungspakete schneller als die normale Tickrate gesendet");
        m.put("WrongBreak", "Abbaufortschritt für einen anderen als den anvisierten Block gesendet");
        TRANSLATIONS.put("de", m);
    }

    private static void initFR() {
        Map<String, String> m = new HashMap<>(140);
        m.put("AimDuplicateLook", "Envoi d'un paquet de rotation dupliqué sans changement de direction");
        m.put("AimModulo360", "Envoi d'un mouvement brusque et non naturel de l'angle de vue (yaw snap)");
        m.put("AirLiquidBreak", "Tentative de détruire un bloc indestructible ou un fluide");
        m.put("AirLiquidPlace", "Placement d'un bloc sur un support non valide");
        m.put("AntiExplosion", "Aucun recul d'explosion attendu n'a été subi");
        m.put("AntiKB", "Recul attendu d'une entité non appliqué (AntiKB)");
        m.put("AntiXray", "Minage de minerais cachés ou ratio de minerai anormal");
        m.put("AutoTotem", "Équipement d'un totem avec un timing ou une séquence impossible");
        m.put("BadPacketsA", "ID d'emplacement de barre rapide dupliqué");
        m.put("BadPacketsB", "Paquet de rotation forcée du serveur ignoré");
        m.put("BadPacketsC", "Tentative de se réveiller sans être endormi");
        m.put("BadPacketsD", "Angle de vue vertical hors de la plage autorisée (-90 à 90)");
        m.put("BadPacketsE", "Trop de paquets de mouvement sans changement de position");
        m.put("BadPacketsF", "Statut de sprint envoyé en double");
        m.put("BadPacketsG", "Statut d'accroupissement envoyé en double");
        m.put("BadPacketsH", "Numéro de séquence inattendu envoyé");
        m.put("BadPacketsI", "Prétend voler sans en avoir l'autorisation");
        m.put("BadPacketsJ", "La rotation lors de l'utilisation d'objet ne correspond pas à la rotation du tick");
        m.put("BadPacketsK", "Paquets de spectateur envoyés hors du mode spectateur");
        m.put("BadPacketsL", "Paquet de creusage impossible envoyé");
        m.put("BadPacketsM", "Tentative de réapparition en étant vivant");
        m.put("BadPacketsN", "Téléportation serveur requise ignorée ou non confirmée");
        m.put("BadPacketsO", "Réponse Keepalive avec un ID non envoyé par le serveur");
        m.put("BadPacketsP", "Paquet de clic d'inventaire non valide");
        m.put("BadPacketsQ", "Paquet de saut de cheval avec entité ou force non valide");
        m.put("BadPacketsR", "Arrêt des mises à jour de position tout en répondant aux transactions");
        m.put("BadPacketsS", "Paquet de confirmation de fenêtre marqué comme non accepté");
        m.put("BadPacketsU", "Paquet d'utilisation d'objet impossible envoyé");
        m.put("BadPacketsV", "Déplacement insuffisant par rapport au mouvement annoncé");
        m.put("BadPacketsX", "Actions d'accroupissement ou de sprint dupliquées avant le prochain mouvement");
        m.put("BadPacketsY", "ID d'emplacement hors limites envoyé");
        m.put("BadPacketsZ", "Paquets d'actions du joueur dupliqués dans le même tick client");
        m.put("Baritone", "Comportement automatisé de type bot Baritone détecté");
        m.put("ChatA", "Paquet d'autocomplétion Tab sans commande ni texte");
        m.put("ChatB", "Message de chat non valide");
        m.put("ChatC", "Déplacement tout en écrivant dans le chat");
        m.put("ChatD", "Message envoyé alors que le chat est masqué");
        m.put("CombatAutoClicker", "CPS élevé, délai nul ou régularité robotique des clics en combat");
        m.put("ComparativeAnomaly", "Anomalie statistique de paquets par rapport aux joueurs de même latence");
        m.put("CrashA", "Position envoyée hors des limites du monde (tentative de crash)");
        m.put("CrashB", "Paquets d'inventaire créatif envoyés hors du mode créatif");
        m.put("CrashC", "Position ou rotation non finie envoyée (NaN ou infini)");
        m.put("CrashD", "Clics sur des emplacements du pupitre non autorisés");
        m.put("CrashE", "Distance d'affichage du client inférieure au minimum autorisé");
        m.put("CrashF", "Clic d'inventaire avec bouton ou valeur d'emplacement non valide");
        m.put("CrashG", "Numéro de séquence négatif envoyé");
        m.put("CrashH", "Requête Tab avec une longueur excessive ou invalide");
        m.put("CrashI", "Sélection d'objet dans un baluchon avec un index négatif invalide");
        m.put("DuplicateRotPlace", "Répétition d'un delta de rotation identique lors du placement de blocs");
        m.put("ElytraA", "Début de vol en élytres alors que déjà en train de voler");
        m.put("ElytraB", "Vol en élytres démarré sans saut préalable");
        m.put("ElytraC", "Activation des élytres trop fréquente");
        m.put("ElytraD", "Vol démarré sans porter d'élytres");
        m.put("ElytraE", "Vol en élytres démarré pendant le vol créatif");
        m.put("ElytraF", "Vol en élytres démarré en étant au sol");
        m.put("ElytraG", "Vol en élytres démarré avec l'effet de lévitation");
        m.put("ElytraH", "Vol en élytres démarré assis dans un véhicule");
        m.put("ElytraI", "Vol en élytres démarré dans l'eau");
        m.put("EntitySpeed", "Déplacement plus rapide qu'autorisé lors de la collision avec des entités");
        m.put("ExploitA", "Nom d'objet trop long dans l'enclume");
        m.put("ExploitB", "Édition de livre non valide ou suspecte");
        m.put("FabricatedPlace", "Position du curseur hors des limites valides lors du placement");
        m.put("FarBreak", "Destruction de bloc à une distance trop éloignée");
        m.put("FarPlace", "Placement de bloc à une distance trop éloignée");
        m.put("FastBreak", "Destruction de bloc plus rapide que la vitesse normale (FastBreak)");
        m.put("GroundSpoof", "Prétend être au sol alors que la simulation prédit le contraire");
        m.put("Hitboxes", "Attaque en dehors de la boîte de collision réelle de l'entité");
        m.put("InvalidBreak", "ID de face de bloc impossible envoyé lors du minage");
        m.put("InvalidInteractCursor", "Vecteur d'interaction avec l'entité hors de sa hitbox");
        m.put("InvalidInteractTarget", "Interaction avec une entité inexistante");
        m.put("InvalidPlaceCursor", "Position du curseur non valide lors du placement d'un bloc");
        m.put("InvalidPlaceFace", "ID de face de bloc impossible envoyé lors du placement");
        m.put("InventoryAction", "Interaction dans le monde ou attaque pendant la manipulation de l'inventaire");
        m.put("InventoryAutoClicker", "Vitesse de clic excessive ou régularité mécanique dans l'inventaire");
        m.put("KillAuraBot", "Attaque d'une entité bot invisible anti-killaura");
        m.put("MultiActionsA", "Attaque effectuée pendant l'utilisation d'un objet (nourriture, arc, bouclier)");
        m.put("MultiActionsB", "Destruction de bloc pendant l'utilisation d'un objet");
        m.put("MultiActionsC", "Clic dans l'inventaire tout en se déplaçant");
        m.put("MultiActionsD", "Fermeture de l'inventaire tout en se déplaçant");
        m.put("MultiActionsE", "Coup de bras effectué pendant l'utilisation d'un objet");
        m.put("MultiActionsF", "Interaction simultanée avec un bloc et une entité dans le même tick");
        m.put("MultiActionsG", "Attaque ou utilisation d'objets en ramant en bateau");
        m.put("MultiBreak", "Tentative de détruire plusieurs blocs distincts dans le même tick");
        m.put("MultiInteractA", "Interaction avec plusieurs entités dans le même tick");
        m.put("MultiInteractB", "Multiples paquets d'interaction avec points d'impact différents dans un tick");
        m.put("MultiPlace", "Placement de plusieurs blocs dans un seul tick");
        m.put("NegativeTimer", "Envoi de paquets de mouvement plus lent que le taux de tick normal");
        m.put("NoFall", "Paquet au sol envoyé sans entrer en collision avec le sol (NoFall)");
        m.put("NoSlow", "Pas de ralentissement lors de l'utilisation d'un objet (NoSlow)");
        m.put("NoSwingBreak", "Destruction de bloc sans coup de bras");
        m.put("PacketOrderA", "Ordre non valide des paquets de clic et déplacement rapide");
        m.put("PacketOrderB", "Attaque effectuée sans coup de bras préalable (NoSwing)");
        m.put("PacketOrderC", "Paquets INTERACT et INTERACT_AT envoyés dans le mauvais ordre");
        m.put("PacketOrderD", "Interaction main gauche envoyée avant l'interaction main droite");
        m.put("PacketOrderE", "Changement de slot pendant une action conflictuelle");
        m.put("PacketOrderF", "Paquets d'action envoyés dans un ordre invalide après accroupissement/sprint");
        m.put("PacketOrderG", "Gestion de l'inventaire pendant une action conflictuelle");
        m.put("PacketOrderH", "Changements d'état de sprint et d'accroupissement dans le mauvais ordre");
        m.put("PacketOrderI", "Paquets de combat, d'utilisation ou de minage dans un ordre de tick non valide");
        m.put("PacketOrderJ", "Utilisation d'objet après une attaque sans le paquet d'interaction attendu");
        m.put("PacketOrderK", "Ouverture, clic ou fermeture d'inventaire dans le mauvais ordre");
        m.put("PacketOrderL", "Paquets de jet d'objet, d'ouverture d'inventaire ou d'échange dans le mauvais ordre");
        m.put("PacketOrderM", "Paquets d'utilisation d'objet et d'interaction avec entité dans le mauvais ordre");
        m.put("PacketOrderN", "Paquets d'utilisation d'objet et de placement de bloc dans le mauvais ordre");
        m.put("PacketOrderO", "Paquets envoyés après le mouvement avant la fin attendue du tick client");
        m.put("PacketOrderP", "Réponse aux paquets de batch de chunks dans un ordre invalide");
        m.put("Phase", "Déplacement à travers des blocs solides (Phase)");
        m.put("PositionBreakA", "Tentative de casser une face de bloc depuis une position d'yeux impossible");
        m.put("PositionBreakB", "Annulation du minage d'un bloc avec une face non valide");
        m.put("PositionPlace", "Placement d'un bloc contre une face cachée");
        m.put("Post", "Paquets envoyés hors de l'ordre normal du tick de mouvement");
        m.put("Reach", "Attaque d'une entité à une portée excessive (Reach)");
        m.put("RotationBreak", "Tentative de détruire un bloc sans le regarder");
        m.put("RotationPlace", "Placement d'un bloc sans le regarder");
        m.put("SelfInteract", "Interaction avec soi-même");
        m.put("Simulation", "Mouvement divergent de la simulation physique prédite par le serveur");
        m.put("SprintA", "Sprint avec un niveau de faim trop faible");
        m.put("SprintB", "Sprint en position accroupie ou en rampant");
        m.put("SprintC", "Sprint pendant l'utilisation d'un objet");
        m.put("SprintD", "Début de sprint avec l'effet de cécité");
        m.put("SprintE", "Sprint en collision frontale avec un mur");
        m.put("SprintF", "Sprint pendant le vol en élytres");
        m.put("SprintG", "Sprint dans l'eau de manière non autorisée");
        m.put("TickTimer", "Paquet de fin de tick client non envoyé");
        m.put("Timer", "Le jeu du joueur s'exécute plus rapidement que la normale (Timer)");
        m.put("TimerLimit", "Trop de paquets envoyés consécutivement après une latence élevée");
        m.put("TransactionOrder", "Réponses aux transactions ou au ping envoyées dans le désordre");
        m.put("VehicleA", "Valeurs d'entrée impossibles lors du contrôle d'un véhicule");
        m.put("VehicleB", "Prétend être dans un véhicule sans y être réellement");
        m.put("VehicleC", "Mouvement du véhicule divergent du contrôle physique prédit");
        m.put("VehicleD", "Saut effectué dans un véhicule ne pouvant pas sauter");
        m.put("VehicleE", "États de pagaie envoyés sans être dans un bateau");
        m.put("VehicleF", "États de pagaie de bateau incorrects envoyés");
        m.put("VehicleTimer", "Paquets de mouvement de véhicule envoyés plus vite que le tick rate");
        m.put("WrongBreak", "Progression de minage envoyée pour un bloc différent de celui miné");
        TRANSLATIONS.put("fr", m);
    }

    private static void initES() {
        Map<String, String> m = new HashMap<>(140);
        m.put("AimDuplicateLook", "Envió una rotación duplicada sin cambiar la dirección de la mirada");
        m.put("AimModulo360", "Giro brusco y no natural del ángulo de visión (yaw snap)");
        m.put("AirLiquidBreak", "Intento de romper un bloque indestructible o líquido");
        m.put("AirLiquidPlace", "Colocó un bloque sobre un soporte no válido");
        m.put("AntiExplosion", "No recibió el empuje de explosión esperado");
        m.put("AntiKB", "No recibió el empuje esperado de una entidad (AntiKB)");
        m.put("AntiXray", "Minó minerales ocultos o con una proporción imposible respecto a la piedra");
        m.put("AutoTotem", "Equipó un tótem con sincronización o secuencia imposible");
        m.put("BadPacketsA", "ID de ranura duplicada en la barra rápida");
        m.put("BadPacketsB", "Ignoró el paquete de rotación forzada del servidor");
        m.put("BadPacketsC", "Intentó despertarse sin estar durmiendo");
        m.put("BadPacketsD", "Ángulo de inclinación vertical fuera del rango -90 a 90");
        m.put("BadPacketsE", "Demasiados paquetes de movimiento sin actualizar posición");
        m.put("BadPacketsF", "Estado de sprint enviado por duplicado");
        m.put("BadPacketsG", "Estado de agachado enviado por duplicado");
        m.put("BadPacketsH", "Número de secuencia inesperado enviado");
        m.put("BadPacketsI", "Afirma estar volando sin tener permitido el vuelo");
        m.put("BadPacketsJ", "La rotación al usar objeto no coincide con la rotación del tick");
        m.put("BadPacketsK", "Paquetes de espectador enviados sin estar en modo espectador");
        m.put("BadPacketsL", "Paquete de excavación imposible enviado");
        m.put("BadPacketsM", "Intentó reaparecer estando vivo");
        m.put("BadPacketsN", "Ignoró o no confirmó una teletransportación requerida del servidor");
        m.put("BadPacketsO", "Respondió con un ID de keepalive no emitido por el servidor");
        m.put("BadPacketsP", "Paquete de clic en ventana no válido");
        m.put("BadPacketsQ", "Paquete de salto a caballo con entidad o fuerza no válida");
        m.put("BadPacketsR", "Detuvo el envío de posición mientras seguía respondiendo transacciones");
        m.put("BadPacketsS", "Paquete de confirmación de ventana marcado como no aceptado");
        m.put("BadPacketsU", "Paquete de uso de objeto imposible enviado");
        m.put("BadPacketsV", "No se desplazó la distancia requerida");
        m.put("BadPacketsX", "Acciones duplicadas de agacharse o esprintar antes del siguiente movimiento");
        m.put("BadPacketsY", "ID de ranura fuera de los límites permitidos");
        m.put("BadPacketsZ", "Paquetes duplicados de entrada del jugador en el mismo tick");
        m.put("Baritone", "Comportamiento automatizado estilo bot Baritone detectado");
        m.put("ChatA", "Paquete de autocompletado Tab sin comando ni texto");
        m.put("ChatB", "Mensaje de chat no válido");
        m.put("ChatC", "Movimiento mientras chatea");
        m.put("ChatD", "Mensaje enviado mientras el chat está oculto");
        m.put("CombatAutoClicker", "Alto CPS, retraso cero o consistencia robótica de clics en combate");
        m.put("ComparativeAnomaly", "Anomalía estadística de paquetes comparada con jugadores de ping similar");
        m.put("CrashA", "Posición enviada fuera de los límites del mundo (intento de crash)");
        m.put("CrashB", "Paquetes de modo creativo enviados sin estar en creativo");
        m.put("CrashC", "Posición o rotación no finita enviada (NaN o infinito)");
        m.put("CrashD", "Clics en ranuras de atril no válidas");
        m.put("CrashE", "Distancia de renderizado del cliente inferior al mínimo permitido");
        m.put("CrashF", "Clic en inventario con botón o ranura no válida");
        m.put("CrashG", "Número de secuencia negativo enviado");
        m.put("CrashH", "Solicitud Tab con longitud excesiva o no válida");
        m.put("CrashI", "Selección en saco con índice negativo no válido");
        m.put("DuplicateRotPlace", "Repitió exactamente el mismo delta de rotación al colocar bloques");
        m.put("ElytraA", "Comenzó a planear mientras ya estaba planeando");
        m.put("ElytraB", "Comenzó a planear sin haber saltado");
        m.put("ElytraC", "Activación de élitros con frecuencia excesiva");
        m.put("ElytraD", "Comenzó a planear sin tener élitros equipados");
        m.put("ElytraE", "Comenzó a planear mientras volaba en creativo");
        m.put("ElytraF", "Comenzó a planear estando en el suelo");
        m.put("ElytraG", "Comenzó a planear bajo efecto de levitación");
        m.put("ElytraH", "Comenzó a planear estando dentro de un vehículo");
        m.put("ElytraI", "Comenzó a planear dentro del agua");
        m.put("EntitySpeed", "Velocidad excesiva al colisionar o atravesar entidades");
        m.put("ExploitA", "Nombre de ítem demasiado largo en el yunque");
        m.put("ExploitB", "Edición de libro no válida");
        m.put("FabricatedPlace", "Posición del cursor fuera de los límites al colocar un bloque");
        m.put("FarBreak", "Rompió un bloque desde una distancia excesiva");
        m.put("FarPlace", "Colocó un bloque desde una distancia excesiva");
        m.put("FastBreak", "Rompió un bloque más rápido de lo permitido (FastBreak)");
        m.put("GroundSpoof", "Afirma estar en el suelo cuando la simulación predice lo contrario");
        m.put("Hitboxes", "Ataque fuera de la hitbox real de la entidad");
        m.put("InvalidBreak", "Identificador de cara de bloque imposible al romper");
        m.put("InvalidInteractCursor", "Vector de interacción situado fuera de la hitbox del objetivo");
        m.put("InvalidInteractTarget", "Interactuó con una entidad que no existe");
        m.put("InvalidPlaceCursor", "Posición de cursor no válida al colocar un bloque");
        m.put("InvalidPlaceFace", "Identificador de cara de bloque imposible al colocar");
        m.put("InventoryAction", "Interactuó en el mundo o atacó mientras gestionaba el inventario");
        m.put("InventoryAutoClicker", "Velocidad de clics excesiva o consistencia robótica en el inventario");
        m.put("KillAuraBot", "Atacó a una entidad bot invisible de detección de killaura");
        m.put("MultiActionsA", "Atacó mientras consumía o utilizaba un objeto");
        m.put("MultiActionsB", "Rompió bloques mientras utilizaba un objeto");
        m.put("MultiActionsC", "Clicó en el inventario mientras se movía");
        m.put("MultiActionsD", "Cerró el inventario mientras se movía");
        m.put("MultiActionsE", "Blandió la mano mientras utilizaba un objeto");
        m.put("MultiActionsF", "Interactuó con un bloque y una entidad en el mismo tick");
        m.put("MultiActionsG", "Atacó o utilizó objetos mientras remaba en un bote");
        m.put("MultiBreak", "Intentó romper múltiples bloques diferentes en un solo tick");
        m.put("MultiInteractA", "Interactuó con múltiples entidades en el mismo tick");
        m.put("MultiInteractB", "Múltiples paquetes de interacción con diferentes puntos de impacto en un tick");
        m.put("MultiPlace", "Colocó múltiples bloques en un solo tick");
        m.put("NegativeTimer", "Envió paquetes de movimiento a un ritmo inferior al normal");
        m.put("NoFall", "Envió paquete de suelo sin colisionar con el terreno (NoFall)");
        m.put("NoSlow", "No fue ralentizado al utilizar un objeto (NoSlow)");
        m.put("NoSwingBreak", "Rompió un bloque sin realizar la animación de brazo");
        m.put("PacketOrderA", "Orden no válido de paquetes de clic y movimiento rápido");
        m.put("PacketOrderB", "Ataque sin previa animación de brazo (NoSwing)");
        m.put("PacketOrderC", "Paquetes INTERACT e INTERACT_AT enviados en orden incorrecto");
        m.put("PacketOrderD", "Interacción de mano secundaria enviada antes de la principal");
        m.put("PacketOrderE", "Cambió de ranura durante una acción conflictiva");
        m.put("PacketOrderF", "Paquetes de acción enviados en orden no válido tras agacharse o esprintar");
        m.put("PacketOrderG", "Gestión del inventario durante una acción en conflicto");
        m.put("PacketOrderH", "Cambios de estado de sprint y agachado en orden no válido");
        m.put("PacketOrderI", "Paquetes de combate, uso o minado en orden de tick no válido");
        m.put("PacketOrderJ", "Uso de objeto tras ataque sin el paquete de interacción esperado");
        m.put("PacketOrderK", "Apertura, clic o cierre de inventario en orden incorrecto");
        m.put("PacketOrderL", "Paquetes de soltar objeto, abrir inventario o cambio de mano en orden no válido");
        m.put("PacketOrderM", "Paquetes de uso de objeto e interacción con entidad en orden incorrecto");
        m.put("PacketOrderN", "Paquetes de uso de objeto y colocación de bloque en orden incorrecto");
        m.put("PacketOrderO", "Paquetes enviados tras el movimiento antes del fin de tick esperado");
        m.put("PacketOrderP", "Respondió a paquetes de chunks en orden de transacción no válido");
        m.put("Phase", "Se movió atravesando bloques sólidos (Phase)");
        m.put("PositionBreakA", "Intentó romper una cara de bloque desde una posición de ojos imposible");
        m.put("PositionBreakB", "Canceló el minado de un bloque con una cara no válida");
        m.put("PositionPlace", "Colocó un bloque contra una cara oculta");
        m.put("Post", "Envió paquetes fuera del orden esperado en el tick");
        m.put("Reach", "Atacó a una entidad desde una distancia excesiva (Reach)");
        m.put("RotationBreak", "Intentó romper un bloque sin mirarlo");
        m.put("RotationPlace", "Colocó un bloque sin mirarlo");
        m.put("SelfInteract", "Interactuó consigo mismo");
        m.put("Simulation", "Movimiento diferente a la simulación física predicha por el servidor");
        m.put("SprintA", "Esprintando con un nivel de hambre insuficiente");
        m.put("SprintB", "Esprintando mientras se agacha o repta");
        m.put("SprintC", "Esprintando mientras utiliza un objeto");
        m.put("SprintD", "Comenzó a esprintar bajo el efecto de ceguera");
        m.put("SprintE", "Esprintando mientras colisiona contra una pared");
        m.put("SprintF", "Esprintando mientras planea con élitros");
        m.put("SprintG", "Esprintando en el agua sin nadar correctamente");
        m.put("TickTimer", "No envió el paquete de fin de tick del cliente");
        m.put("Timer", "El juego del jugador se ejecuta más rápido de lo normal (Timer)");
        m.put("TimerLimit", "Envió demasiados paquetes acumulados tras una alta latencia");
        m.put("TransactionOrder", "Respuestas a transacciones o ping en orden incorrecto");
        m.put("VehicleA", "Valores de control imposibles en el vehículo");
        m.put("VehicleB", "Afirma estar en un vehículo sin estar dentro de uno");
        m.put("VehicleC", "El movimiento del vehículo no coincide con el control predicho");
        m.put("VehicleD", "Saltó en un vehículo que no tiene capacidad de salto");
        m.put("VehicleE", "Estados de remo enviados sin estar en un bote");
        m.put("VehicleF", "Estados de remo de bote incorrectos enviados");
        m.put("VehicleTimer", "Paquetes de movimiento de vehículo enviados más rápido de lo permitido");
        m.put("WrongBreak", "Progreso de rotura enviado para un bloque distinto al que se mina");
        TRANSLATIONS.put("es", m);
    }

    private static void initPT() {
        Map<String, String> m = new HashMap<>(140);
        m.put("AimDuplicateLook", "Enviou uma rotação duplicada sem alterar a direção do olhar");
        m.put("AimModulo360", "Giro brusco e não natural do ângulo de visão (yaw snap)");
        m.put("AirLiquidBreak", "Tentativa de quebrar um bloco indestrutível ou líquido");
        m.put("AirLiquidPlace", "Colocou um bloco em um suporte inválido");
        m.put("AntiExplosion", "Não recebeu o repulsão de explosão esperada");
        m.put("AntiKB", "Não recebeu a repulsão esperada de entidade (AntiKB)");
        m.put("AntiXray", "Minerou minérios ocultos ou com proporção impossível em relação à pedra");
        m.put("AutoTotem", "Equipou totem com sincronização ou sequência impossível");
        m.put("BadPacketsA", "ID de slot duplicado na barra de atalhos");
        m.put("BadPacketsB", "Ignorou o pacote de rotação forçada do servidor");
        m.put("BadPacketsC", "Tentou acordar sem estar dormindo");
        m.put("BadPacketsD", "Ângulo de inclinação vertical fora do intervalo de -90 a 90");
        m.put("BadPacketsE", "Muitos pacotes de movimento sem atualizar a posição");
        m.put("BadPacketsF", "Status de corrida enviado em duplicidade");
        m.put("BadPacketsG", "Status de agachamento enviado em duplicidade");
        m.put("BadPacketsH", "ID de sequência inesperado enviado");
        m.put("BadPacketsI", "Afirma estar voando sem permissão para voar");
        m.put("BadPacketsJ", "Rotação ao usar item não condiz com a rotação do tick");
        m.put("BadPacketsK", "Pacotes de espectador enviados fora do modo espectador");
        m.put("BadPacketsL", "Pacote de escavação impossível enviado");
        m.put("BadPacketsM", "Tentou renascer estando vivo");
        m.put("BadPacketsN", "Ignorou ou recusou uma teletransportação obrigatória do servidor");
        m.put("BadPacketsO", "Respondeu com ID de keepalive não enviado pelo servidor");
        m.put("BadPacketsP", "Pacote de clique em janela inválido");
        m.put("BadPacketsQ", "Pacote de salto em cavalo com entidade ou força inválida");
        m.put("BadPacketsR", "Parou de enviar posições enquanto respondia a transações");
        m.put("BadPacketsS", "Pacote de confirmação de janela marcado como rejeitado");
        m.put("BadPacketsU", "Pacote de uso de item impossível enviado");
        m.put("BadPacketsV", "Não se moveu o suficiente para o movimento declarado");
        m.put("BadPacketsX", "Ações duplicadas de agachamento ou corrida antes do próximo movimento");
        m.put("BadPacketsY", "ID de slot fora dos limites permitidos");
        m.put("BadPacketsZ", "Pacotes duplicados de entrada do jogador no mesmo tick");
        m.put("Baritone", "Comportamento automatizado estilo bot Baritone detectado");
        m.put("ChatA", "Pacote de autocompletar Tab sem comando ou texto");
        m.put("ChatB", "Mensagem de chat inválida");
        m.put("ChatC", "Movimento enquanto digita no chat");
        m.put("ChatD", "Mensagem enviada enquanto o chat está oculto");
        m.put("CombatAutoClicker", "Alto CPS, atraso zero ou consistência robótica de cliques em combate");
        m.put("ComparativeAnomaly", "Anomalia estatística de pacotes comparada a jogadores com ping similar");
        m.put("CrashA", "Posição enviada fora dos limites do mundo (tentativa de crash)");
        m.put("CrashB", "Pacotes do modo criativo enviados fora do criativo");
        m.put("CrashC", "Posição ou rotação não finita enviada (NaN ou infinito)");
        m.put("CrashD", "Cliques em slots de atril inválidos");
        m.put("CrashE", "Distância de renderização do cliente inferior ao mínimo permitido");
        m.put("CrashF", "Clique no inventário com botão ou slot inválido");
        m.put("CrashG", "ID de sequência negativo enviado");
        m.put("CrashH", "Requisição Tab com tamanho excessivo ou inválido");
        m.put("CrashI", "Seleção no pacote com índice negativo inválido");
        m.put("DuplicateRotPlace", "Repetiu o mesmo delta de rotação exato ao colocar blocos");
        m.put("ElytraA", "Começou a planar enquanto já planava");
        m.put("ElytraB", "Começou a planar sem ter saltado");
        m.put("ElytraC", "Ativação de élitros com frequência excessiva");
        m.put("ElytraD", "Começou a planar sem estar com élitros equipados");
        m.put("ElytraE", "Começou a planar enquanto já voava");
        m.put("ElytraF", "Começou a planar estando no chão");
        m.put("ElytraG", "Começou a planar sob efeito de levitação");
        m.put("ElytraH", "Começou a planar montado em um veículo");
        m.put("ElytraI", "Começou a planar dentro da água");
        m.put("EntitySpeed", "Velocidade acima do permitido ao colidir ou passar por entidades");
        m.put("ExploitA", "Nome do item excessivamente longo na bigorna");
        m.put("ExploitB", "Edição de livro inválida");
        m.put("FabricatedPlace", "Posição do cursor fora dos limites ao colocar um bloco");
        m.put("FarBreak", "Quebrou bloco a uma distância excessiva");
        m.put("FarPlace", "Colocou bloco a uma distância excessiva");
        m.put("FastBreak", "Quebrou bloco mais rápido do que o permitido (FastBreak)");
        m.put("GroundSpoof", "Afirmou estar no chão quando a simulação previa o contrário");
        m.put("Hitboxes", "Ataque fora da hitbox real da entidade");
        m.put("InvalidBreak", "Identificador de face de bloco impossível ao quebrar");
        m.put("InvalidInteractCursor", "Vetor de interação situado fora da hitbox do alvo");
        m.put("InvalidInteractTarget", "Interagiu com uma entidade inexistente");
        m.put("InvalidPlaceCursor", "Posição de cursor inválida ao colocar um bloco");
        m.put("InvalidPlaceFace", "Identificador de face de bloco impossível ao colocar");
        m.put("InventoryAction", "Interagiu no mundo ou atacou enquanto geria o inventário");
        m.put("InventoryAutoClicker", "Velocidade de cliques excessiva ou consistência robótica no inventário");
        m.put("KillAuraBot", "Atacou uma entidade bot invisível de detecção de killaura");
        m.put("MultiActionsA", "Atacou enquanto consumia ou utilizava um item");
        m.put("MultiActionsB", "Quebrou blocos enquanto utilizava um item");
        m.put("MultiActionsC", "Clicou no inventário enquanto se movia");
        m.put("MultiActionsD", "Fechou o inventário enquanto se movia");
        m.put("MultiActionsE", "Balançou a mão enquanto utilizava um item");
        m.put("MultiActionsF", "Interagiu com um bloco e uma entidade no mesmo tick");
        m.put("MultiActionsG", "Atacou ou utilizou itens enquanto remava em um barco");
        m.put("MultiBreak", "Tentou quebrar múltiplos blocos diferentes em um único tick");
        m.put("MultiInteractA", "Interagiu com múltiplas entidades no mesmo tick");
        m.put("MultiInteractB", "Múltiplos pacotes de interação com diferentes pontos de impacto em um tick");
        m.put("MultiPlace", "Colocou múltiplos blocos em um único tick");
        m.put("NegativeTimer", "Enviou pacotes de movimento em taxa inferior à normal");
        m.put("NoFall", "Enviou pacote de solo sem colidir com o chão (NoFall)");
        m.put("NoSlow", "Não foi desacelerado ao utilizar um item (NoSlow)");
        m.put("NoSwingBreak", "Quebrou bloco sem realizar a animação de braço");
        m.put("PacketOrderA", "Ordem inválida de pacotes de clique e movimento rápido");
        m.put("PacketOrderB", "Ataque sem prévia animação de braço (NoSwing)");
        m.put("PacketOrderC", "Pacotes INTERACT e INTERACT_AT enviados em ordem incorreta");
        m.put("PacketOrderD", "Interação da mão secundária enviada antes da principal");
        m.put("PacketOrderE", "Mudou de slot durante uma ação conflitante");
        m.put("PacketOrderF", "Pacotes de ação enviados em ordem inválida após agachar ou correr");
        m.put("PacketOrderG", "Gestão de inventário durante uma ação conflitante");
        m.put("PacketOrderH", "Mudanças de estado de corrida e agachamento em ordem inválida");
        m.put("PacketOrderI", "Pacotes de combate, uso ou mineração em ordem de tick inválida");
        m.put("PacketOrderJ", "Uso de item após ataque sem o pacote de interação esperado");
        m.put("PacketOrderK", "Abertura, clique ou fechamento de inventário em ordem incorreta");
        m.put("PacketOrderL", "Pacotes de dropar item, abrir inventário ou trocar de mão em ordem inválida");
        m.put("PacketOrderM", "Pacotes de uso de item e interação com entidade em ordem incorreta");
        m.put("PacketOrderN", "Pacotes de uso de item e colocação de bloco em ordem incorreta");
        m.put("PacketOrderO", "Pacotes enviados após o movimento antes do fim do tick esperado");
        m.put("PacketOrderP", "Respondeu a pacotes de chunks em ordem de transação inválida");
        m.put("Phase", "Moveu-se através de blocos sólidos (Phase)");
        m.put("PositionBreakA", "Tentou quebrar uma face de bloco a partir de posição impossível");
        m.put("PositionBreakB", "Cancelou a quebra de bloco com uma face inválida");
        m.put("PositionPlace", "Colocou um bloco contra uma face oculta");
        m.put("Post", "Enviou pacotes fora da ordem esperada no tick");
        m.put("Reach", "Atacou uma entidade a uma distância excessiva (Reach)");
        m.put("RotationBreak", "Tentou quebrar um bloco sem olhar para ele");
        m.put("RotationPlace", "Colocou um bloco sem olhar para ele");
        m.put("SelfInteract", "Interagiu consigo mesmo");
        m.put("Simulation", "Movimento diferente da simulação física prevista pelo servidor");
        m.put("SprintA", "Correndo com nível de fome insuficiente");
        m.put("SprintB", "Correndo enquanto agachado ou rastejando");
        m.put("SprintC", "Correndo enquanto utiliza um item");
        m.put("SprintD", "Começou a correr sob o efeito de cegueira");
        m.put("SprintE", "Correndo enquanto colide contra uma parede");
        m.put("SprintF", "Correndo enquanto plana com élitros");
        m.put("SprintG", "Correndo na água de forma inválida");
        m.put("TickTimer", "Não enviou o pacote de término do tick do cliente");
        m.put("Timer", "O jogo do jogador está rodando mais rápido que o normal (Timer)");
        m.put("TimerLimit", "Enviou pacotes em excesso acumulados após alta latência");
        m.put("TransactionOrder", "Respostas a transações ou ping em ordem incorreta");
        m.put("VehicleA", "Valores de entrada impossíveis no controle do veículo");
        m.put("VehicleB", "Afirma estar em um veículo sem estar montado em um");
        m.put("VehicleC", "Movimento do veículo diferente do controle previsto");
        m.put("VehicleD", "Saltou em um veículo que não pula (barco/carrinho)");
        m.put("VehicleE", "Estados de remo de barco enviados sem estar em um barco");
        m.put("VehicleF", "Estados de remo de barco incorretos enviados");
        m.put("VehicleTimer", "Pacotes de movimento de veículo enviados mais rápido que o permitido");
        m.put("WrongBreak", "Progresso de quebra enviado para um bloco diferente do que está sendo minerado");
        TRANSLATIONS.put("pt", m);
    }

    private static void initJA() {
        Map<String, String> m = new HashMap<>(140);
        m.put("AimDuplicateLook", "視線方向を変更せずに重複した回転パケットを送信しました");
        m.put("AimModulo360", "不自然な急激な視点回転を送信しました (yaw snap)");
        m.put("AirLiquidBreak", "破壊できないブロックまたは液体を破壊しようとしました");
        m.put("AirLiquidPlace", "無効な支持面にブロックを設置しました");
        m.put("AntiExplosion", "爆発によるノックバックを受けませんでした");
        m.put("AntiKB", "エンティティからのノックバックを受けませんでした (AntiKB)");
        m.put("AntiXray", "露出していない鉱石の採掘または不自然な採掘比率を検出しました");
        m.put("AutoTotem", "不可能な速度や手順でトーテムをオフハンドに装備しました");
        m.put("BadPacketsA", "重複したホットバースロットIDを送信しました");
        m.put("BadPacketsB", "サーバーから指定された視線回転パケットを無視しました");
        m.put("BadPacketsC", "就寝状態でないのに起床パケットを送信しました");
        m.put("BadPacketsD", "ピッチ角（上下の視線角度）が-90〜90度の範囲外です");
        m.put("BadPacketsE", "位置座標の更新を伴わない移動パケットが過剰に送信されました");
        m.put("BadPacketsF", "重複したダッシュ状態パケットを送信しました");
        m.put("BadPacketsG", "重複したスニーク状態パケットを送信しました");
        m.put("BadPacketsH", "予期しないシーケンスIDを送信しました");
        m.put("BadPacketsI", "飛行権限がないのに飛行状態を主張しました");
        m.put("BadPacketsJ", "アイテム使用パケット内の回転がティック回転と一致しません");
        m.put("BadPacketsK", "スペクテイターモードでないのに観戦パケットを送信しました");
        m.put("BadPacketsL", "不可能なブロック採掘パケットを送信しました");
        m.put("BadPacketsM", "生存している状態でリスポーンパケットを送信しました");
        m.put("BadPacketsN", "サーバーからのテレポート要求を無視または受諾しませんでした");
        m.put("BadPacketsO", "サーバーから送信されていないKeepAlive IDで応答しました");
        m.put("BadPacketsP", "無効なウィンドウクリックパケットを送信しました");
        m.put("BadPacketsQ", "無効なエンティティまたはブースト値で馬のジャンプパケットを送信しました");
        m.put("BadPacketsR", "トランザクションには応答しつつ位置情報の送信を停止しました");
        m.put("BadPacketsS", "拒否フラグが付与されたウィンドウ確認パケットを送信しました");
        m.put("BadPacketsU", "不可能なアイテム使用パケットを送信しました");
        m.put("BadPacketsV", "申告された移動に対して移動距離が不十分です");
        m.put("BadPacketsX", "次の移動パケットの前に重複したスニーク/ダッシュ入力を送信しました");
        m.put("BadPacketsY", "有効範囲外のスロットIDを送信しました");
        m.put("BadPacketsZ", "同一クライアントティック内に重複したプレイヤー入力パケットを送信しました");
        m.put("Baritone", "Baritoneボット特有の自動化された挙動を検出しました");
        m.put("ChatA", "コマンドや入力文字列がないTab補完パケットを送信しました");
        m.put("ChatB", "不正なチャットメッセージを送信しました");
        m.put("ChatC", "チャット入力中に移動を行いました");
        m.put("ChatD", "チャット非表示設定中にチャットメッセージを送信しました");
        m.put("CombatAutoClicker", "高CPS、クリックスキップ、または機械的な一定クリックを検出しました");
        m.put("ComparativeAnomaly", "類似pingのプレイヤー群と比較して統計的なパケット異常を検出しました");
        m.put("CrashA", "ワールド境界外の不正な座標を送信しました (サーバークラッシュ試行)");
        m.put("CrashB", "クリエイティブモードでないのにクリエイティブインベントリパケットを送信しました");
        m.put("CrashC", "非有限値 (NaN / 無限大) の座標または回転を送信しました");
        m.put("CrashD", "書見台ウィンドウ内の不正なスロットをクリックしました");
        m.put("CrashE", "許容最小値を下回る描画距離を送信しました");
        m.put("CrashF", "無効なボタンまたはスロット番号でインベントリクリックを送信しました");
        m.put("CrashG", "負のシーケンスIDを送信しました");
        m.put("CrashH", "異常に長い文字列でTab補完リクエストを送信しました");
        m.put("CrashI", "バンドル内のアイテム選択で無効な負のインデックスを送信しました");
        m.put("DuplicateRotPlace", "ブロック設置時に全く同一の回転差分を繰り返しました");
        m.put("ElytraA", "既に滑空中であるにもかかわらず再度滑空を開始しました");
        m.put("ElytraB", "ジャンプせずにエリトラ滑空を開始しました");
        m.put("ElytraC", "過剰な頻度でエリトラ滑空を再起動しました");
        m.put("ElytraD", "エリトラを装備していない状態で滑空を開始しました");
        m.put("ElytraE", "飛行モード中にエリトラ滑空を開始しました");
        m.put("ElytraF", "地面に着地している状態で滑空を開始しました");
        m.put("ElytraG", "浮遊エフェクト適用中にエリトラ滑空を開始しました");
        m.put("ElytraH", "乗り物に乗った状態でエリトラ滑空を開始しました");
        m.put("ElytraI", "水中にいる状態でエリトラ滑空を開始しました");
        m.put("EntitySpeed", "エンティティとの衝突またはすり抜け時に許容速度を超過しました");
        m.put("ExploitA", "金床でのアイテム名が制限長を超過しています");
        m.put("ExploitB", "無効な本・羽根ペンの編集データを送信しました");
        m.put("FabricatedPlace", "ブロック境界外のカーソル位置で設置パケットを送信しました");
        m.put("FarBreak", "届かないはずの遠距離にあるブロックを破壊しました");
        m.put("FarPlace", "届かないはずの遠距離にブロックを設置しました");
        m.put("FastBreak", "計算上の最速破壊時間より速くブロックを破壊しました (FastBreak)");
        m.put("GroundSpoof", "実際には空中にいるのに地上にいるとパケットで偽装しました (GroundSpoof)");
        m.put("Hitboxes", "エンティティの有効ヒットボックス範囲外を攻撃しました");
        m.put("InvalidBreak", "ブロック破壊時に存在しないブロック面IDを送信しました");
        m.put("InvalidInteractCursor", "エンティティへのインタラクト位置が対象のヒットボックス外です");
        m.put("InvalidInteractTarget", "存在しないエンティティに対してインタラクトを試みました");
        m.put("InvalidPlaceCursor", "ブロック設置時に無効なカーソル座標を送信しました");
        m.put("InvalidPlaceFace", "ブロック設置時に存在しないブロック面IDを送信しました");
        m.put("InventoryAction", "インベントリ操作中にワールド内のオブジェクト操作や攻撃を行いました");
        m.put("InventoryAutoClicker", "インベントリ内で異常なクリック速度または機械的連打を検出しました");
        m.put("KillAuraBot", "不可視のアンチキルオーラ検証用ボットエンティティを攻撃しました");
        m.put("MultiActionsA", "アイテム使用中（飲食・弓・盾等）に攻撃を行いました");
        m.put("MultiActionsB", "アイテム使用中に同時にブロックを破壊しました");
        m.put("MultiActionsC", "移動中にインベントリ内のアイテムをクリックしました");
        m.put("MultiActionsD", "移動中にインベントリを閉じました");
        m.put("MultiActionsE", "アイテム使用中に腕を振りました");
        m.put("MultiActionsF", "同一ティック内にブロックとエンティティの両方と対話しました");
        m.put("MultiActionsG", "ボートを漕ぎながら攻撃またはアイテムを使用しました");
        m.put("MultiBreak", "1ティック内に複数の異なるブロックを破壊しようとしました");
        m.put("MultiInteractA", "同一ティック内に複数のエンティティと対話しました");
        m.put("MultiInteractB", "1ティック内に異なる打撃位置を持つ複数の対話パケットを送信しました");
        m.put("MultiPlace", "1ティック内に複数のブロックを設置しました");
        m.put("NegativeTimer", "正常なクライアント周期よりも遅いレートで移動パケットを送信しました");
        m.put("NoFall", "地面と接触していないのに着地フラグを偽装しました (NoFall)");
        m.put("NoSlow", "アイテム使用時に発生するはずの移動速度低下がありません (NoSlow)");
        m.put("NoSwingBreak", "腕振りアニメーションを送信せずにブロックを破壊しました");
        m.put("PacketOrderA", "アイテム回収とクイック移動パケットが不正な順序で送信されました");
        m.put("PacketOrderB", "攻撃前に腕振りアニメーションを送信しませんでした (NoSwing)");
        m.put("PacketOrderC", "INTERACTとINTERACT_ATパケットが逆の順序で送信されました");
        m.put("PacketOrderD", "メインハンドより先にオフハンドのインタラクトパケットが送信されました");
        m.put("PacketOrderE", "競合する別のアクションの最中に手持ちスロットを変更しました");
        m.put("PacketOrderF", "スニークやダッシュ入力後のアクションパケット順序が不正です");
        m.put("PacketOrderG", "競合するアクションの最中にホットバーやインベントリを操作しました");
        m.put("PacketOrderH", "ダッシュとスニークの状態変更パケットが不正な順序で送信されました");
        m.put("PacketOrderI", "戦闘・使用・解放・採掘パケットが不正なティック順序で送信されました");
        m.put("PacketOrderJ", "攻撃直後に必要なインタラクトパケットなしでアイテム使用を送信しました");
        m.put("PacketOrderK", "インベントリの開閉・クリックパケットが不正な順序で送信されました");
        m.put("PacketOrderL", "アイテム投棄・インベントリ開閉・持ち手切り替えパケットが不正な順序です");
        m.put("PacketOrderM", "アイテム使用とエンティティ対話パケットが不正な順序で送信されました");
        m.put("PacketOrderN", "アイテム使用とブロック設置パケットが不正な順序で送信されました");
        m.put("PacketOrderO", "クライアントティック終了前に移動後パケットが送信されました");
        m.put("PacketOrderP", "チャンクバッチパケットに対するトランザクション応答順序が不正です");
        m.put("Phase", "移動予測中に固体ブロックの内部に入り込みました (Phase)");
        m.put("PositionBreakA", "不可能な視線位置からブロックの面を破壊しようとしました");
        m.put("PositionBreakB", "無効なブロック面でブロック破壊をキャンセルしました");
        m.put("PositionPlace", "見えない隠れたブロック面に対してブロックを設置しました");
        m.put("Post", "移動ティックの想定順序から外れてパケットを送信しました");
        m.put("Reach", "許容攻撃射程を超える距離からエンティティを攻撃しました (Reach)");
        m.put("RotationBreak", "ブロックを見ずに破壊しようとしました");
        m.put("RotationPlace", "ブロックを見ずに設置しました");
        m.put("SelfInteract", "自分自身に対するインタラクトパケットを送信しました");
        m.put("Simulation", "サーバーの物理演算シミュレーションと異なる移動を行いました");
        m.put("SprintA", "満腹度が不足している状態でダッシュしました");
        m.put("SprintB", "スニークまたは匍匐状態でダッシュしました");
        m.put("SprintC", "アイテムを使用しながらダッシュしました");
        m.put("SprintD", "盲目エフェクト適用中にダッシュを開始しました");
        m.put("SprintE", "壁に正面衝突しながらダッシュを維持しました");
        m.put("SprintF", "エリトラで滑空中にダッシュ状態を維持しました");
        m.put("SprintG", "水中において不可能な陸上ダッシュを行いました");
        m.put("TickTimer", "クライアントティック終了パケットを送信しませんでした");
        m.put("Timer", "プレイヤーのゲームクロックが通常より高速で動作しています (Timer)");
        m.put("TimerLimit", "高レイテンシ後に許容上限を超えるパケットをまとめて送信しました");
        m.put("TransactionOrder", "トランザクションまたはPing応答が不正な順序で送信されました");
        m.put("VehicleA", "乗り物の操縦入力値が物理的に不可能な値です");
        m.put("VehicleB", "乗り物に乗っていないのに乗り物搭乗中と偽装しました");
        m.put("VehicleC", "乗り物の移動がサーバーの物理制御予測と一致しません");
        m.put("VehicleD", "ジャンプ不可能な乗り物（ボート・トロッコ等）でジャンプを行いました");
        m.put("VehicleE", "ボートに乗っていないのにパドル操作状態を送信しました");
        m.put("VehicleF", "不正なボートパドル状態を送信しました");
        m.put("VehicleTimer", "乗り物の移動パケットが通常のティックレートを超えて高速で送信されました");
        m.put("WrongBreak", "現在採掘中のものとは異なるブロックに対して採掘進行パケットを送信しました");
        TRANSLATIONS.put("ja", m);
    }

    private static void initIT() {
        Map<String, String> m = new HashMap<>(140);
        m.put("AimDuplicateLook", "Inviato pacchetto di rotazione duplicato senza cambiare direzione dello sguardo");
        m.put("AimModulo360", "Inviato uno scatto improvviso e non naturale dello sguardo (yaw snap)");
        m.put("AirLiquidBreak", "Tentativo di rompere un blocco indistruttibile o un liquido");
        m.put("AirLiquidPlace", "Piazzato un blocco su un supporto non valido");
        m.put("AntiExplosion", "Nessun contraccolpo da esplosione atteso ricevuto");
        m.put("AntiKB", "Non ha subito il knockback atteso dall'entità (AntiKB)");
        m.put("AntiXray", "Estrazione di minerali nascosti o proporzione anomala minerale/pietra");
        m.put("AutoTotem", "Equipaggiato un totem con tempistiche o sequenze impossibili");
        m.put("BadPacketsA", "ID slot della barra rapida duplicato");
        m.put("BadPacketsB", "Ignorato il pacchetto di rotazione impostato dal server");
        m.put("BadPacketsC", "Tentativo di svegliarsi senza essere nel letto");
        m.put("BadPacketsD", "Inclinazione dello sguardo non valida (fuori dall'intervallo -90, 90)");
        m.put("BadPacketsE", "Troppi pacchetti di movimento senza aggiornamento della posizione");
        m.put("BadPacketsF", "Stato di scatto inviato in duplicato");
        m.put("BadPacketsG", "Stato di accovacciamento inviato in duplicato");
        m.put("BadPacketsH", "ID di sequenza imprevisto inviato");
        m.put("BadPacketsI", "Dichiara di volare senza averne il permesso");
        m.put("BadPacketsJ", "La rotazione nell'uso dell'oggetto non coincide con quella del tick");
        m.put("BadPacketsK", "Inviati pacchetti da spettatore senza essere in modalità spettatore");
        m.put("BadPacketsL", "Inviato un pacchetto di scavo impossibile");
        m.put("BadPacketsM", "Tentativo di respawn mentre si è ancora vivi");
        m.put("BadPacketsN", "Teletrasporto obbligatorio del server ignorato o non accettato");
        m.put("BadPacketsO", "Risposto con un ID keepalive non inviato dal server");
        m.put("BadPacketsP", "Pacchetto di click nella finestra non valido");
        m.put("BadPacketsQ", "Pacchetto di salto da cavallo con entità o valore non valido");
        m.put("BadPacketsR", "Aggiornamenti di posizione interrotti mentre si risponde alle transazioni");
        m.put("BadPacketsS", "Pacchetto di conferma finestra contrassegnato come non accettato");
        m.put("BadPacketsU", "Inviato un pacchetto di uso oggetto impossibile");
        m.put("BadPacketsV", "Movimento insufficiente rispetto alla distanza minima");
        m.put("BadPacketsX", "Input duplicati di scatto o furtività prima del prossimo movimento");
        m.put("BadPacketsY", "ID slot fuori dai limiti consentiti");
        m.put("BadPacketsZ", "Pacchetti duplicati di input del giocatore nello stesso tick client");
        m.put("Baritone", "Rilevato comportamento automatizzato simile al bot Baritone");
        m.put("ChatA", "Pacchetto di autocompletamento Tab senza comando o testo");
        m.put("ChatB", "Messaggio di chat non valido");
        m.put("ChatC", "Movimento durante la scrittura in chat");
        m.put("ChatD", "Messaggio inviato mentre la chat è nascosta");
        m.put("CombatAutoClicker", "CPS elevato, ritardo zero o regolarità robotica dei click in combattimento");
        m.put("ComparativeAnomaly", "Anomalia statistica nei pacchetti rispetto a giocatori con ping simile");
        m.put("CrashA", "Posizione inviata fuori dai confini del mondo (tentativo di crash)");
        m.put("CrashB", "Pacchetti inventario creativo inviati senza essere in creativa");
        m.put("CrashC", "Inviata posizione o rotazione non finita (NaN o infinito)");
        m.put("CrashD", "Click su slot non validi nella finestra del leggio");
        m.put("CrashE", "Distanza di visualizzazione del client inferiore al minimo consentito");
        m.put("CrashF", "Click nell'inventario con pulsante o slot non valido");
        m.put("CrashG", "ID di sequenza negativo inviato");
        m.put("CrashH", "Richiesta Tab con lunghezza eccessiva o non valida");
        m.put("CrashI", "Selezione nel fagotto con indice negativo non valido");
        m.put("DuplicateRotPlace", "Ripetuto l'identico delta di rotazione durante il piazzamento di blocchi");
        m.put("ElytraA", "Iniziata la planata mentre si stava già planando");
        m.put("ElytraB", "Iniziata la planata senza aver saltato");
        m.put("ElytraC", "Attivazione delle elitre troppo frequente");
        m.put("ElytraD", "Iniziata la planata senza avere le elitre equipaggiate");
        m.put("ElytraE", "Iniziata la planata durante il volo");
        m.put("ElytraF", "Iniziata la planata trovandosi a terra");
        m.put("ElytraG", "Iniziata la planata sotto effetto di levitazione");
        m.put("ElytraH", "Iniziata la planata mentre si è su un veicolo");
        m.put("ElytraI", "Iniziata la planata mentre si è in acqua");
        m.put("EntitySpeed", "Velocità oltre il limite consentito durante la collisione con entità");
        m.put("ExploitA", "Nome dell'oggetto troppo lungo nell'incudine");
        m.put("ExploitB", "Modifica del libro non valida");
        m.put("FabricatedPlace", "Posizione del cursore fuori dai limiti consentiti al piazzamento");
        m.put("FarBreak", "Rottura di un blocco da distanza eccessiva");
        m.put("FarPlace", "Piazzamento di un blocco da distanza eccessiva");
        m.put("FastBreak", "Rottura del blocco più veloce rispetto ai limiti fisici (FastBreak)");
        m.put("GroundSpoof", "Dichiara di essere a terra mentre la simulazione fisica dice il contrario");
        m.put("Hitboxes", "Attacco al di fuori della hitbox reale dell'entità");
        m.put("InvalidBreak", "Inviato un ID faccia del blocco impossibile durante la rottura");
        m.put("InvalidInteractCursor", "Vettore di interazione esterno alla hitbox dell'entità bersaglio");
        m.put("InvalidInteractTarget", "Interazione con un'entità non esistente");
        m.put("InvalidPlaceCursor", "Posizione del cursore non valida durante il piazzamento del blocco");
        m.put("InvalidPlaceFace", "Inviato un ID faccia del blocco impossibile durante il piazzamento");
        m.put("InventoryAction", "Interazione nel mondo o attacco durante la gestione dell'inventario");
        m.put("InventoryAutoClicker", "Velocità di click eccessiva o regolarità robotica nell'inventario");
        m.put("KillAuraBot", "Attaccata l'entità bot invisibile per la rilevazione di killaura");
        m.put("MultiActionsA", "Attacco eseguito durante l'uso di un oggetto");
        m.put("MultiActionsB", "Rottura di blocchi durante l'uso di un oggetto");
        m.put("MultiActionsC", "Click nell'inventario durante il movimento");
        m.put("MultiActionsD", "Chiusura dell'inventario durante il movimento");
        m.put("MultiActionsE", "Braccio agitato durante l'uso di un oggetto");
        m.put("MultiActionsF", "Interazione simultanea con blocco ed entità nello stesso tick");
        m.put("MultiActionsG", "Attacco o uso di oggetti remando su una barca");
        m.put("MultiBreak", "Tentativo di rompere blocchi multipli differenti nello stesso tick");
        m.put("MultiInteractA", "Interazione con entità multiple nello stesso tick");
        m.put("MultiInteractB", "Inviati pacchetti multipli di interazione con punti di impatto diversi in un tick");
        m.put("MultiPlace", "Piazzati blocchi multipli in un singolo tick");
        m.put("NegativeTimer", "Invio di pacchetti di movimento più lento del normale tick rate");
        m.put("NoFall", "Inviato pacchetto a terra senza collisione effettiva col suolo (NoFall)");
        m.put("NoSlow", "Nessun rallentamento durante l'uso di un oggetto (NoSlow)");
        m.put("NoSwingBreak", "Rottura di un blocco senza animazione del braccio");
        m.put("PacketOrderA", "Ordine non valido dei pacchetti di raccolta e spostamento rapido");
        m.put("PacketOrderB", "Attacco eseguito senza precedente animazione del braccio (NoSwing)");
        m.put("PacketOrderC", "Pacchetti INTERACT e INTERACT_AT inviati nell'ordine errato");
        m.put("PacketOrderD", "Interazione con mano secondaria inviata prima di quella principale");
        m.put("PacketOrderE", "Cambiato slot durante un'altra azione in conflitto");
        m.put("PacketOrderF", "Pacchetti di azione inviati in ordine errato dopo furtività o scatto");
        m.put("PacketOrderG", "Gestione dell'inventario durante un'azione in conflitto");
        m.put("PacketOrderH", "Cambiamenti di stato di scatto e furtività inviati in ordine non valido");
        m.put("PacketOrderI", "Pacchetti di combattimento, uso o scavo in ordine di tick non valido");
        m.put("PacketOrderJ", "Uso di oggetto dopo attacco senza il pacchetto di interazione atteso");
        m.put("PacketOrderK", "Apertura, click o chiusura dell'inventario in ordine errato");
        m.put("PacketOrderL", "Gettare oggetti, apertura inventario o cambio mano in ordine errato");
        m.put("PacketOrderM", "Uso di oggetto e interazione con entità inviati in ordine errato");
        m.put("PacketOrderN", "Uso di oggetto e piazzamento blocco inviati in ordine errato");
        m.put("PacketOrderO", "Pacchetti inviati dopo il movimento prima della fine del tick client");
        m.put("PacketOrderP", "Risposta ai pacchetti di batch di chunk in ordine di transazione non valido");
        m.put("Phase", "Movimento attraverso blocchi solidi (Phase)");
        m.put("PositionBreakA", "Tentativo di rompere la faccia di un blocco da posizione impossibile");
        m.put("PositionBreakB", "Annullata la rottura di un blocco con faccia non valida");
        m.put("PositionPlace", "Piazzato un blocco contro una faccia nascosta");
        m.put("Post", "Pacchetti inviati al di fuori dell'ordine atteso nel tick");
        m.put("Reach", "Attacco a un'entità da una portata eccessiva (Reach)");
        m.put("RotationBreak", "Tentativo di rompere un blocco senza guardarlo");
        m.put("RotationPlace", "Piazzamento di un blocco senza guardarlo");
        m.put("SelfInteract", "Interazione con se stessi");
        m.put("Simulation", "Movimento discordante rispetto alla simulazione fisica del server");
        m.put("SprintA", "Scatto con livello di fame insufficiente");
        m.put("SprintB", "Scatto mentre è accovacciato o striscia");
        m.put("SprintC", "Scatto durante l'uso di un oggetto");
        m.put("SprintD", "Iniziato a scattare sotto l'effetto di cecità");
        m.put("SprintE", "Scatto mentre urta una parete");
        m.put("SprintF", "Scatto durante il volo con le elitre");
        m.put("SprintG", "Scatto nell'acqua in modo non consentito");
        m.put("TickTimer", "Pacchetto di fine tick client non inviato");
        m.put("Timer", "Il gioco sta girando più veloce del normale (Timer)");
        m.put("TimerLimit", "Troppi pacchetti inviati in sequenza dopo alta latenza");
        m.put("TransactionOrder", "Risposte a transazioni o ping inviate in ordine errato");
        m.put("VehicleA", "Valori di input impossibili nel controllo del veicolo");
        m.put("VehicleB", "Dichiara di essere su un veicolo senza esservi a bordo");
        m.put("VehicleC", "Movimento del veicolo non coerente con il controllo fisico previsto");
        m.put("VehicleD", "Salto eseguito su un veicolo incapace di saltare");
        m.put("VehicleE", "Stati dei remi inviati senza essere su una barca");
        m.put("VehicleF", "Stati dei remi errati inviati per la barca");
        m.put("VehicleTimer", "Pacchetti di movimento veicolo inviati più veloci del tick rate normale");
        m.put("WrongBreak", "Avanzamento di scavo inviato per un blocco diverso da quello minato");
        TRANSLATIONS.put("it", m);
    }

    private static void initPL() {
        Map<String, String> m = new HashMap<>(140);
        m.put("AimDuplicateLook", "Wysłano zduplikowany pakiet obrotu bez zmiany kierunku patrzenia");
        m.put("AimModulo360", "Wysłano gwałtowny, nienaturalny obrót kamery (yaw snap)");
        m.put("AirLiquidBreak", "Próba zniszczenia niezniszczalnego bloku lub cieczy");
        m.put("AirLiquidPlace", "Postawiono blok na nieprawidłowej powierzchni oparcia");
        m.put("AntiExplosion", "Brak oczekiwanego odrzutu od eksplozji");
        m.put("AntiKB", "Brak oczekiwanego odrzutu od uderzenia bytu (AntiKB)");
        m.put("AntiXray", "Wydobywanie ukrytych rud lub niemożliwy stosunek rudy do kamienia");
        m.put("AutoTotem", "Wyposażenie totemu w nienaturalnym tempie lub niemożliwej sekwencji");
        m.put("BadPacketsA", "Wysłano zduplikowany numer slotu paska podręcznego");
        m.put("BadPacketsB", "Zignorowano pakiet wymuszonego obrotu kamery od serwera");
        m.put("BadPacketsC", "Próba wybudzenia się bez bycia w łóżku");
        m.put("BadPacketsD", "Wysłano nieprawidłowy kąt pochylenia kamery poza zakresem -90 do 90");
        m.put("BadPacketsE", "Zbyt wiele pakietów ruchu bez zmiany pozycji");
        m.put("BadPacketsF", "Zduplikowany pakiet stanu sprintu");
        m.put("BadPacketsG", "Zduplikowany pakiet stanu kucania");
        m.put("BadPacketsH", "Wysłano nieoczekiwany identyfikator sekwencji");
        m.put("BadPacketsI", "Deklaracja latania bez uprawnień do lotu");
        m.put("BadPacketsJ", "Obrót w pakiecie użycia przedmiotu nie zgadza się z obrotem w ticku");
        m.put("BadPacketsK", "Wysłano pakiety obserwatora bez aktywnego trybu widza");
        m.put("BadPacketsL", "Wysłano niemożliwy pakiet rozpoczęcia/końca kopania");
        m.put("BadPacketsM", "Próba odrodzenia się, będąc przy życiu");
        m.put("BadPacketsN", "Zignorowano lub odrzucono wymaganą teleportację serwera");
        m.put("BadPacketsO", "Odpowiedź z identyfikatorem keepalive, którego serwer nie wysyłał");
        m.put("BadPacketsP", "Nieprawidłowy pakiet kliknięcia w oknie ekwipunku");
        m.put("BadPacketsQ", "Pakiet skoku na koniu z nieprawidłowym bytem lub siłą");
        m.put("BadPacketsR", "Zaprzestano wysyłania pozycji, wciąż odpowiadając na transakcje");
        m.put("BadPacketsS", "Pakiet potwierdzenia okna oznaczony jako niezaakceptowany");
        m.put("BadPacketsU", "Wysłano niemożliwy pakiet użycia przedmiotu");
        m.put("BadPacketsV", "Brak wystarczającego przemieszczenia w pakiecie ruchu");
        m.put("BadPacketsX", "Zduplikowane akcje kucania lub sprintu przed kolejnym pakietem ruchu");
        m.put("BadPacketsY", "Wysłano identyfikator slotu poza dozwolonym zakresem");
        m.put("BadPacketsZ", "Zduplikowany pakiet sterowania gracza w tym samym ticku klienta");
        m.put("Baritone", "Wykryto zautomatyzowane zachowanie charakterystyczne dla bota Baritone");
        m.put("ChatA", "Pakiet autouzupełniania Tab bez komendy lub tekstu");
        m.put("ChatB", "Nieprawidłowa wiadomość na czacie");
        m.put("ChatC", "Poruszanie się podczas pisania na czacie");
        m.put("ChatD", "Wysyłanie wiadomości przy wyłączonym lub ukrytym czacie");
        m.put("CombatAutoClicker", "Wysoki CPS, brak odstępów lub mechaniczna powtarzalność kliknięć w walce");
        m.put("ComparativeAnomaly", "Anomalia statystyczna pakietów w porównaniu do graczy o podobnym pingu");
        m.put("CrashA", "Wysłano pozycję poza granicami świata (próba scrashowania serwera)");
        m.put("CrashB", "Pakiety trybu kreatywnego wysłane bez aktywnego gamemode creative");
        m.put("CrashC", "Wysłano niefinitywną pozycję lub obrót (NaN lub Infinity)");
        m.put("CrashD", "Kliknięcia w nieprawidłowe sloty w oknie mównicy");
        m.put("CrashE", "Zasięg renderowania klienta poniżej dozwolonego minimum");
        m.put("CrashF", "Kliknięcie w ekwipunku z nieprawidłowym przyciskiem lub slotem");
        m.put("CrashG", "Wysłano ujemny identyfikator sekwencji");
        m.put("CrashH", "Żądanie Tab o nadmiernej lub nieprawidłowej długości");
        m.put("CrashI", "Wybór przedmiotu w sakwie z ujemnym indeksem slotu");
        m.put("DuplicateRotPlace", "Powtarzanie identycznego delty obrotu podczas stawiania bloków");
        m.put("ElytraA", "Rozpoczęto szybowanie podczas już aktywnego lotu na elytrze");
        m.put("ElytraB", "Rozpoczęcie lotu na elytrze bez wcześniejszego skoku");
        m.put("ElytraC", "Zbyt częsta ponowna aktywacja elytry");
        m.put("ElytraD", "Próba lotu bez założonej elytry");
        m.put("ElytraE", "Aktywacja elytry podczas normalnego latania");
        m.put("ElytraF", "Aktywacja elytry stojąc bezpośrednio na ziemi");
        m.put("ElytraG", "Aktywacja elytry pod wpływem efektu lewitacji");
        m.put("ElytraH", "Aktywacja elytry będąc w pojeździe (łódź, wagonik)");
        m.put("ElytraI", "Aktywacja elytry będąc zanurzonym w wodzie");
        m.put("EntitySpeed", "Przekroczenie dozwolonej prędkości przy kolizji lub przenikaniu przez byty");
        m.put("ExploitA", "Zbyt długa nazwa przedmiotu w kowadle");
        m.put("ExploitB", "Nieprawidłowa edycja książki");
        m.put("FabricatedPlace", "Wysłano pozycję kursora poza dopuszczalnymi granicami bloku");
        m.put("FarBreak", "Niszczenie bloku ze zbyt dużej odległości");
        m.put("FarPlace", "Stawianie bloku ze zbyt dużej odległości");
        m.put("FastBreak", "Zbyt szybkie niszczenie bloku wbrew fizyce gry (FastBreak)");
        m.put("GroundSpoof", "Pakiet wskazuje na kontakt z ziemią, gdy gracz w rzeczywistości jest w powietrzu");
        m.put("Hitboxes", "Atak w obszar znajdujący się poza rzeczywistym hitboxem bytu");
        m.put("InvalidBreak", "Wysłano nieprawidłowy identyfikator ścianki niszczonego bloku");
        m.put("InvalidInteractCursor", "Wektor interakcji z bytem znajduje się poza jego hitboxem");
        m.put("InvalidInteractTarget", "Interakcja z nieistniejącym bytem");
        m.put("InvalidPlaceCursor", "Nieprawidłowa pozycja kursora podczas stawiania bloku");
        m.put("InvalidPlaceFace", "Wysłano nieprawidłowy identyfikator ścianki stawianego bloku");
        m.put("InventoryAction", "Interakcja ze światem lub atak w trakcie manipulacji ekwipunkiem");
        m.put("InventoryAutoClicker", "Zbyt szybkie klikanie lub mechaniczna powtarzalność w oknie ekwipunku");
        m.put("KillAuraBot", "Zaatakowano niewidzialnego bota weryfikacyjnego antycheata (KillAura)");
        m.put("MultiActionsA", "Atakowanie w trakcie używania przedmiotu (jedzenie, łuk, tarcza)");
        m.put("MultiActionsB", "Niszczenie bloków w trakcie używania przedmiotu");
        m.put("MultiActionsC", "Kliknięcie w ekwipunku podczas biegu lub ruchu");
        m.put("MultiActionsD", "Zamknięcie ekwipunku podczas ruchu");
        m.put("MultiActionsE", "Machnięcie ręką w trakcie używania przedmiotu");
        m.put("MultiActionsF", "Interakcja z blokiem i bytem w tym samym ticku");
        m.put("MultiActionsG", "Atakowanie lub używanie przedmiotów podczas wiosłowania łódką");
        m.put("MultiBreak", "Próba zniszczenia wielu różnych bloków w jednym ticku");
        m.put("MultiInteractA", "Interakcja z wieloma bytami w jednym ticku");
        m.put("MultiInteractB", "Wysłano wiele pakietów interakcji z różnymi punktami uderzenia w jednym ticku");
        m.put("MultiPlace", "Postawiono wiele bloków w jednym ticku");
        m.put("NegativeTimer", "Wysyłanie pakietów ruchu wolniej niż normalna częstotliwość gry");
        m.put("NoFall", "Wysłano informację o byciu na ziemi bez kolizji z podłożem (NoFall)");
        m.put("NoSlow", "Brak spowolnienia podczas używania przedmiotu (NoSlow)");
        m.put("NoSwingBreak", "Niszczenie bloku bez wykonania animacji machnięcia ręką");
        m.put("PacketOrderA", "Nieprawidłowa kolejność pakietów podnoszenia i szybkiego przenoszenia");
        m.put("PacketOrderB", "Atak bez wykonania wcześniejszego machnięcia ręką (NoSwing)");
        m.put("PacketOrderC", "Pakiety INTERACT i INTERACT_AT wysłane w złej kolejności");
        m.put("PacketOrderD", "Interakcja drugą ręką wysłana przed interakcją ręką główną");
        m.put("PacketOrderE", "Zmiana slotu w trakcie wykonania innej sprzecznej akcji");
        m.put("PacketOrderF", "Pakiety akcji po kucnięciu lub sprincie wysłane w złej kolejności");
        m.put("PacketOrderG", "Zarządzanie ekwipunkiem podczas innej sprzecznej akcji");
        m.put("PacketOrderH", "Pakiety zmiany sprintu i kucania wysłane w nieprawidłowej kolejności");
        m.put("PacketOrderI", "Pakiety walki, użycia lub kopania w nieprawidłowej kolejności w ticku");
        m.put("PacketOrderJ", "Użycie przedmiotu po ataku bez wymaganego pakietu interakcji");
        m.put("PacketOrderK", "Otwarcie, kliknięcie lub zamknięcie ekwipunku w złej kolejności");
        m.put("PacketOrderL", "Wyrzucenie przedmiotu, otwarcie ekwipunku lub zmiana ręki w złej kolejności");
        m.put("PacketOrderM", "Pakiety użycia przedmiotu i interakcji z bytem w nieprawidłowej kolejności");
        m.put("PacketOrderN", "Pakiety użycia przedmiotu i stawiania bloku w nieprawidłowej kolejności");
        m.put("PacketOrderO", "Wysłano pakiety po ruchu przed oczekiwanym zakończeniem ticka");
        m.put("PacketOrderP", "Odpowiedź na pakiety paczek chunków w złej kolejności transakcji");
        m.put("Phase", "Przenikanie przez lite bloki w trakcie ruchu (Phase)");
        m.put("PositionBreakA", "Próba zniszczenia ścianki bloku z niemożliwej pozycji oczu");
        m.put("PositionBreakB", "Anulowano niszczenie bloku z nieprawidłową ścianką");
        m.put("PositionPlace", "Postawiono blok opierając go o ukrytą, niewidoczną ściankę");
        m.put("Post", "Wysłano pakiety w nieprawidłowej kolejności względem ticka ruchu");
        m.put("Reach", "Zaatakowano byt ze zbyt dużej odległości (Reach)");
        m.put("RotationBreak", "Próba zniszczenia bloku bez patrzenia w jego stronę");
        m.put("RotationPlace", "Stawianie bloku bez patrzenia w jego stronę");
        m.put("SelfInteract", "Interakcja z samym sobą");
        m.put("Simulation", "Ruch niezgodny z fizyczną symulacją serwera");
        m.put("SprintA", "Sprint przy zbyt niskim poziomie głodu");
        m.put("SprintB", "Sprint podczas kucania lub czołgania się");
        m.put("SprintC", "Sprint w trakcie używania przedmiotu");
        m.put("SprintD", "Rozpoczęto sprint mając nałożony efekt ślepoty");
        m.put("SprintE", "Sprint podczas bezpośredniej kolizji ze ścianą");
        m.put("SprintF", "Sprint podczas szybowania na elyttrze");
        m.put("SprintG", "Sprint w wodzie bez właściwego pływania");
        m.put("TickTimer", "Nie wysłano pakietu zakończenia ticka klienta");
        m.put("Timer", "Gra gracza działa szybciej niż powinna (Timer)");
        m.put("TimerLimit", "Wysłano zbyt wiele zbuforowanych pakietów po wysokim lagu sieci");
        m.put("TransactionOrder", "Odpowiedzi na transakcje lub ping wysłane w nieprawidłowej kolejności");
        m.put("VehicleA", "Niemożliwe wartości sterowania pojazdem");
        m.put("VehicleB", "Deklaracja bycia w pojeździe bez faktycznego siedzenia w nim");
        m.put("VehicleC", "Ruch pojazdu niezgodny z fizyczną symulacją serwera");
        m.put("VehicleD", "Skok w pojeździe nieposiadającym możliwości skoku");
        m.put("VehicleE", "Wysłano stan wioseł łodzi bez znajdowania się w łodzi");
        m.put("VehicleF", "Nieprawidłowe lub niemożliwe stany wioseł łodzi");
        m.put("VehicleTimer", "Pakiety ruchu pojazdu wysyłane szybciej niż częstotliwość ticków");
        m.put("WrongBreak", "Wysłano postęp niszczenia dla innego bloku niż aktualnie wydobywany");
        TRANSLATIONS.put("pl", m);
    }

    private static void initTR() {
        Map<String, String> m = new HashMap<>(140);
        m.put("AimDuplicateLook", "Bakış yönü değişmeden yinelenen dönüş paketi gönderildi");
        m.put("AimModulo360", "Anormal derecede ani bakış açısı dönüşü gönderildi (yaw snap)");
        m.put("AirLiquidBreak", "Kırılamaz bir bloğu veya sıvıyı kırma girişimi");
        m.put("AirLiquidPlace", "Geçersiz bir desteğe blok yerleştirildi");
        m.put("AntiExplosion", "Beklenen patlama geri tepmesi alınmadı");
        m.put("AntiKB", "Varlıktan beklenen geri tepme alınmadı (AntiKB)");
        m.put("AntiXray", "Görünmeyen gizli cevher kazıldı veya anormal cevher/taş oranı tespit edildi");
        m.put("AutoTotem", "İmkânsız hız veya otomasyonla sol ele totem yerleştirildi");
        m.put("BadPacketsA", "Yinelenen hızlı erişim slot kimliği gönderildi");
        m.put("BadPacketsB", "Sunucunun belirlediği zorunlu dönüş paketi yok sayıldı");
        m.put("BadPacketsC", "Uyumuyorken uyanma paketi gönderilmeye çalışıldı");
        m.put("BadPacketsD", "Dikey bakış açısı -90 ile 90 derece sınırları dışında gönderildi");
        m.put("BadPacketsE", "Konum değişmeden aşırı sayıda hareket paketi gönderildi");
        m.put("BadPacketsF", "Yinelenen depar durumu paketi gönderildi");
        m.put("BadPacketsG", "Yinelenen eğilme durumu paketi gönderildi");
        m.put("BadPacketsH", "Beklenmeyen sıra kimliği gönderildi (sequence id)");
        m.put("BadPacketsI", "Uçuş izni yokken uçtuğu bildirildi");
        m.put("BadPacketsJ", "Eşya kullanım paketindeki bakış açısı tick açısıyla uyuşmuyor");
        m.put("BadPacketsK", "İzleyici modunda değilken izleyici paketleri gönderildi");
        m.put("BadPacketsL", "İmkânsız blok kazma paketi gönderildi");
        m.put("BadPacketsM", "Hayattayken yeniden doğma (respawn) paketi gönderildi");
        m.put("BadPacketsN", "Sunucunun zorunlu ışınlanma paketi yok sayıldı veya reddedildi");
        m.put("BadPacketsO", "Sunucu tarafından gönderilmemiş bir keepalive kimliğiyle yanıt verildi");
        m.put("BadPacketsP", "Geçersiz pencere tıklama paketi gönderildi");
        m.put("BadPacketsQ", "Geçersiz varlık veya güç değeriyle at zıplama paketi gönderildi");
        m.put("BadPacketsR", "İşlemlere yanıt vermeye devam ederken konum güncellemeleri durduruldu");
        m.put("BadPacketsS", "Kabul edilmedi olarak işaretlenmiş pencere onay paketi gönderildi");
        m.put("BadPacketsU", "İmkânsız eşya kullanım paketi gönderildi");
        m.put("BadPacketsV", "Bildirilen harekete göre yeterince uzağa gidilmedi");
        m.put("BadPacketsX", "Sonraki hareket paketinden önce yinelenen eğilme/depar girişleri gönderildi");
        m.put("BadPacketsY", "Sınırların dışında slot kimliği gönderildi");
        m.put("BadPacketsZ", "Aynı istemci tick'inde yinelenen oyuncu girdi paketleri gönderildi");
        m.put("Baritone", "Baritone botuna benzer otomatik davranış tespit edildi");
        m.put("ChatA", "Komut veya metin içermeyen Tab tamamlama paketi gönderildi");
        m.put("ChatB", "Geçersiz sohbet mesajı gönderildi");
        m.put("ChatC", "Sohbet ederken hareket edildi");
        m.put("ChatD", "Sohbet gizli durumdayken sohbet mesajı gönderildi");
        m.put("CombatAutoClicker", "Yüksek CPS, sıfır gecikme veya robotik tıklama tutarlılığı");
        m.put("ComparativeAnomaly", "Benzer ping grubundaki oyunculara kıyasla istatistiksel paket anomalisi");
        m.put("CrashA", "Dünya sınırları dışında konum gönderildi (çökertme girişimi)");
        m.put("CrashB", "Yaratıcı modda değilken yaratıcı mod envanter paketleri gönderildi");
        m.put("CrashC", "Sonsuz (Infinity) veya tanımsız (NaN) konum veya dönüş gönderildi");
        m.put("CrashD", "Kürsü penceresindeki geçersiz slotlara tıklandı");
        m.put("CrashE", "İstemci görüş mesafesi izin verilen asgari değerin altında gönderildi");
        m.put("CrashF", "Geçersiz buton veya slot değeriyle envanter tıklaması gönderildi");
        m.put("CrashG", "Negatif sıra kimliği gönderildi (negative sequence id)");
        m.put("CrashH", "Aşırı uzun veya geçersiz Tab tamamlama isteği gönderildi");
        m.put("CrashI", "Çıkın (bundle) içinde negatif slot indeksiyle eşya seçimi yapıldı");
        m.put("DuplicateRotPlace", "Blok yerleştirirken birebir aynı bakış açısı farkı tekrarlandı");
        m.put("ElytraA", "Zaten süzülüyorken tekrar süzülme başlatıldı");
        m.put("ElytraB", "Zıplamadan elitra süzülmesi başlatıldı");
        m.put("ElytraC", "Aşırı sıklıkla elitra süzülmesi tetiklendi");
        m.put("ElytraD", "Elitra takılı değilken süzülme başlatıldı");
        m.put("ElytraE", "Uçuş modundayken elitra başlatıldı");
        m.put("ElytraF", "Yerdeyken elitra süzülmesi başlatıldı");
        m.put("ElytraG", "Yükselme (levitation) etkisi altındayken elitra başlatıldı");
        m.put("ElytraH", "Bir araç içerisindeyken elitra başlatıldı");
        m.put("ElytraI", "Su içindeyken elitra süzülmesi başlatıldı");
        m.put("EntitySpeed", "Varlıklarla çarpışırken veya içinden geçerken izin verilen hız aşıldı");
        m.put("ExploitA", "Örs içinde aşırı uzun eşya adı gönderildi");
        m.put("ExploitB", "Geçersiz veya zararlı kitap düzenleme işlemi");
        m.put("FabricatedPlace", "Blok sınırları dışındaki imleç konumuyla yerleştirme paketi gönderildi");
        m.put("FarBreak", "İzin verilen azami mesafenin ötesinden blok kırıldı");
        m.put("FarPlace", "İzin verilen azami mesafenin ötesine blok yerleştirildi");
        m.put("FastBreak", "Fiziksel hız limitlerini aşacak şekilde çok hızlı blok kırıldı (FastBreak)");
        m.put("GroundSpoof", "Oyuncu havadayken pakette yerde olduğu iddia edildi (GroundSpoof)");
        m.put("Hitboxes", "Varlığın geçerli hitbox sınırlarının dışından saldırı yapıldı");
        m.put("InvalidBreak", "Blok kırarken imkânsız bir blok yüzey kimliği gönderildi");
        m.put("InvalidInteractCursor", "Varlık etkileşim vektörü hedef varlığın hitbox sınırları dışındadır");
        m.put("InvalidInteractTarget", "Sunucuda var olmayan bir varlıkla etkileşime girildi");
        m.put("InvalidPlaceCursor", "Blok yerleştirirken geçersiz imleç pozisyonu gönderildi");
        m.put("InvalidPlaceFace", "Blok yerleştirirken imkânsız bir blok yüzey kimliği gönderildi");
        m.put("InventoryAction", "Envanter işlemi sırasında dünyayla etkileşime girildi veya saldırıldı");
        m.put("InventoryAutoClicker", "Envanter içinde aşırı hızlı tıklama veya robotik tıklama düzeni tespit edildi");
        m.put("KillAuraBot", "Görünmez anti-killaura bot varlığına saldırı yapıldı");
        m.put("MultiActionsA", "Eşya kullanırken (yemek, yay, kalkan) aynı anda saldırı yapıldı");
        m.put("MultiActionsB", "Eşya kullanılırken aynı anda blok kırıldı");
        m.put("MultiActionsC", "Hareket halindeyken envanterde eşyalara tıklandı");
        m.put("MultiActionsD", "Hareket halindeyken envanter penceresi kapatıldı");
        m.put("MultiActionsE", "Eşya kullanılırken el sallama eylemi yapıldı");
        m.put("MultiActionsF", "Aynı tick içinde hem blok hem varlıkla etkileşime girildi");
        m.put("MultiActionsG", "Bot küreği çekerken aynı anda saldırı yapıldı veya eşya kullanıldı");
        m.put("MultiBreak", "Tek bir hareket tick'i içinde birden fazla farklı blok kırılmaya çalışıldı");
        m.put("MultiInteractA", "Aynı tick içinde birden fazla varlıkla etkileşime girildi");
        m.put("MultiInteractB", "Tek bir tick içinde farklı temas konumlarına sahip çoklu etkileşim paketi gönderildi");
        m.put("MultiPlace", "Tek bir tick içinde birden fazla blok yerleştirildi");
        m.put("NegativeTimer", "Hareket paketleri normal istemci tick hızından anormal şekilde yavaş gönderildi");
        m.put("NoFall", "Zemine temas etmediği halde düşme hasarı almamak için yerde bildirimi yapıldı (NoFall)");
        m.put("NoSlow", "Eşya kullanılırken olması gereken hareket yavaşlaması görülmedi (NoSlow)");
        m.put("NoSwingBreak", "El sallama animasyonu paketi gönderilmeden blok kırıldı");
        m.put("PacketOrderA", "Eşya alma ve hızlı taşıma tıklama paketleri geçersiz sırada gönderildi");
        m.put("PacketOrderB", "Saldırı öncesinde el sallama paketi gönderilmedi (NoSwing)");
        m.put("PacketOrderC", "INTERACT ve INTERACT_AT varlık paketleri yanlış sırada gönderildi");
        m.put("PacketOrderD", "Sol el etkileşimi ana el etkileşiminden önce gönderildi");
        m.put("PacketOrderE", "Çakışan başka bir eylem sırasında tutulan slot değiştirildi");
        m.put("PacketOrderF", "Eğilme veya depar girişinden sonra eylem paketleri geçersiz sırada gönderildi");
        m.put("PacketOrderG", "Çakışan bir eylem sırasında envanter veya hotbar yönetimi yapıldı");
        m.put("PacketOrderH", "Depar ve eğilme durum değişiklikleri geçersiz paket sırasında gönderildi");
        m.put("PacketOrderI", "Saldırı, kullanım, bırakma veya kazma paketleri geçersiz tick sırasında gönderildi");
        m.put("PacketOrderJ", "Saldırı sonrasında beklenen etkileşim paketi olmadan eşya kullanıldı");
        m.put("PacketOrderK", "Envanter açma, tıklama veya kapatma paketleri yanlış sırada gönderildi");
        m.put("PacketOrderL", "Eşya atma, envanter açma veya el değiştirme paketleri geçersiz sırada gönderildi");
        m.put("PacketOrderM", "Eşya kullanımı ve varlık etkileşim paketleri geçersiz sırada gönderildi");
        m.put("PacketOrderN", "Eşya kullanımı ve blok yerleştirme paketleri geçersiz sırada gönderildi");
        m.put("PacketOrderO", "İstemci tick sonundan önce hareket sonrası paketler gönderildi");
        m.put("PacketOrderP", "Chunk toplu paketlerine geçersiz işlem sırasında yanıt verildi");
        m.put("Phase", "Katı blokların içinden geçerek hareket edildi (Phase)");
        m.put("PositionBreakA", "İmkânsız bir göz açısından blok yüzeyi kırılmaya çalışıldı");
        m.put("PositionBreakB", "Geçersiz bir yüzey kimliğiyle blok kırma işlemi iptal edildi");
        m.put("PositionPlace", "Görünmeyen gizli bir yüzeye blok yerleştirildi");
        m.put("Post", "Paketler hareket tick'inin beklenen sırasının dışında gönderildi");
        m.put("Reach", "İzin verilen azami vuruş mesafesinin ötesinden varlığa saldırıldı (Reach)");
        m.put("RotationBreak", "Bloğa bakmadan bloğu kırma girişimi");
        m.put("RotationPlace", "Hedefe bakmadan blok yerleştirildi");
        m.put("SelfInteract", "Oyuncunun kendi varlığıyla etkileşime girme girişimi");
        m.put("Simulation", "Hareket sunucunun öngördüğü fizik simülasyonundan farklı gerçekleşti");
        m.put("SprintA", "Açlık seviyesi yetersizken depar atıldı");
        m.put("SprintB", "Eğilirken veya sürünürken depar atıldı");
        m.put("SprintC", "Eşya kullanılırken depar atıldı");
        m.put("SprintD", "Körlük etkisi altındayken depara başlandı");
        m.put("SprintE", "Duvara çarpışma halindeyken depar sürdürüldü");
        m.put("SprintF", "Elitra ile süzülürken depar atıldı");
        m.put("SprintG", "Su içindeyken geçersiz depar hareketi yapıldı");
        m.put("TickTimer", "İstemci tick sonu paketi gönderilmedi");
        m.put("Timer", "Oyuncunun oyunu normalden daha hızlı çalışıyor (Timer)");
        m.put("TimerLimit", "Yüksek gecikmenin ardından aşırı miktarda hareket paketi gönderildi");
        m.put("TransactionOrder", "İşlem veya ping yanıtları geçersiz sırada gönderildi");
        m.put("VehicleA", "Araç kontrolünde imkânsız girdi değerleri gönderildi");
        m.put("VehicleB", "Bir araçta değilken araçta olduğu iddia edildi");
        m.put("VehicleC", "Araç hareketi öngörülen fiziksel araç kontrolüyle uyuşmuyor");
        m.put("VehicleD", "Zıplayamayan bir araç içindeyken (bot/vagon) zıplama yapıldı");
        m.put("VehicleE", "Botta değilken bot kürek durumu paketi gönderildi");
        m.put("VehicleF", "Hatalı veya imkânsız bot kürek durumları gönderildi");
        m.put("VehicleTimer", "Araç hareket paketleri normal tick hızından daha hızlı gönderildi");
        m.put("WrongBreak", "Kazılmakta olan bloktan farklı bir blok için kırma ilerlemesi gönderildi");
        TRANSLATIONS.put("tr", m);
    }

    private static void initKO() {
        Map<String, String> m = new HashMap<>(140);
        m.put("AimDuplicateLook", "시선 방향 변경 없이 중복 회전 패킷을 전송했습니다");
        m.put("AimModulo360", "비정상적으로 급격한 시야 회전을 전송했습니다 (yaw snap)");
        m.put("AirLiquidBreak", "파괴할 수 없는 블록이나 액체를 파괴하려고 시도했습니다");
        m.put("AirLiquidPlace", "유효하지 않은 지지대에 블록을 설치했습니다");
        m.put("AntiExplosion", "예상된 폭발 넉백을 받지 않았습니다");
        m.put("AntiKB", "엔티티 피격 시 예상된 넉백을 받지 않았습니다 (AntiKB)");
        m.put("AntiXray", "가려진 광물을 채굴했거나 비정상적인 광물 채굴 비율을 보였습니다");
        m.put("AutoTotem", "비정상적인 속도나 시퀀스로 토템을 왼손에 장착했습니다");
        m.put("BadPacketsA", "중복된 핫바 슬롯 번호를 전송했습니다");
        m.put("BadPacketsB", "서버가 지정한 강제 회전 패킷을 무시했습니다");
        m.put("BadPacketsC", "잠들어 있지 않은 상태에서 기상 패킷을 전송했습니다");
        m.put("BadPacketsD", "시야 수직 각도(pitch)가 -90도에서 90도 범위를 벗어났습니다");
        m.put("BadPacketsE", "위치 갱신 없이 과도한 수의 이동 패킷을 전송했습니다");
        m.put("BadPacketsF", "중복된 질주(스프린트) 상태 패킷을 전송했습니다");
        m.put("BadPacketsG", "중복된 웅크리기(스니크) 상태 패킷을 전송했습니다");
        m.put("BadPacketsH", "예상치 못한 시퀀스 ID 번호를 전송했습니다");
        m.put("BadPacketsI", "비행 권한이 없는 상태에서 비행 중이라고 패킷을 전송했습니다");
        m.put("BadPacketsJ", "아이템 사용 패킷의 시선 회전이 현재 틱의 회전과 일치하지 않습니다");
        m.put("BadPacketsK", "관전자 모드가 아님에도 관전자 패킷을 전송했습니다");
        m.put("BadPacketsL", "물리적으로 불가능한 블록 채굴 패킷을 전송했습니다");
        m.put("BadPacketsM", "살아있는 상태에서 리스폰 패킷을 전송했습니다");
        m.put("BadPacketsN", "서버가 요구한 강제 텔레포트를 무시하거나 승인하지 않았습니다");
        m.put("BadPacketsO", "서버가 보내지 않은 가짜 KeepAlive ID로 응답했습니다");
        m.put("BadPacketsP", "유효하지 않은 인벤토리 윈도우 클릭 패킷을 전송했습니다");
        m.put("BadPacketsQ", "유효하지 않은 엔티티나 부스트 수치로 말 점프 패킷을 전송했습니다");
        m.put("BadPacketsR", "트랜잭션에 계속 응답하면서도 위치 갱신 패킷 전송을 중단했습니다");
        m.put("BadPacketsS", "수락되지 않음으로 표시된 윈도우 확인 패킷을 전송했습니다");
        m.put("BadPacketsU", "물리적으로 불가능한 아이템 사용 패킷을 전송했습니다");
        m.put("BadPacketsV", "신고된 이동 패킷에 비해 이동 거리가 지나치게 부족합니다");
        m.put("BadPacketsX", "다음 이동 패킷 전송 전에 중복된 웅크리기 또는 질주 입력을 전송했습니다");
        m.put("BadPacketsY", "허용 범위를 벗어난 슬롯 번호 ID를 전송했습니다");
        m.put("BadPacketsZ", "동일한 클라이언트 틱 내에 중복된 플레이어 입력 패킷을 전송했습니다");
        m.put("Baritone", "Baritone 봇과 유사한 자동화된 행동 패턴을 감지했습니다");
        m.put("ChatA", "명령어나 텍스트 입력이 없는 상태에서 Tab 자동완성 패킷을 전송했습니다");
        m.put("ChatB", "유효하지 않거나 손상된 채팅 메시지를 전송했습니다");
        m.put("ChatC", "채팅을 입력하는 도중에 이동했습니다");
        m.put("ChatD", "채팅 숨김 설정 상태에서 채팅 메시지를 전송했습니다");
        m.put("CombatAutoClicker", "전투 중 비정상적인 높은 CPS, 0ms 딜레이 또는 기계적인 오토클릭 감지");
        m.put("ComparativeAnomaly", "유사한 핑 대역의 다른 플레이어들과 비교하여 통계적 패킷 이상을 감지했습니다");
        m.put("CrashA", "유효 월드 경계를 벗어난 비정상 좌표를 전송했습니다 (서버 크래시 유도)");
        m.put("CrashB", "크리에이티브 모드가 아님에도 크리에이티브 인벤토리 패킷을 전송했습니다");
        m.put("CrashC", "유한하지 않은 수치(NaN 또는 무한대)의 좌표나 각도를 전송했습니다");
        m.put("CrashD", "독서대 창에서 유효하지 않은 슬롯을 클릭했습니다");
        m.put("CrashE", "클라이언트 시야 거리가 허용 최소치보다 낮게 설정되어 전송되었습니다");
        m.put("CrashF", "유효하지 않은 버튼이나 슬롯 값으로 인벤토리 클릭 패킷을 전송했습니다");
        m.put("CrashG", "음수 시퀀스 ID 번호를 전송했습니다");
        m.put("CrashH", "허용 길이를 초과하는 비정상적인 길이의 Tab 자동완성을 요청했습니다");
        m.put("CrashI", "꾸러미(bundle)에서 유효하지 않은 음수 슬롯 인덱스를 선택했습니다");
        m.put("DuplicateRotPlace", "블록을 배치하는 동안 완전히 동일한 회전 델타를 반복했습니다");
        m.put("ElytraA", "이미 활공 중인 상태에서 다시 활공을 시작했습니다");
        m.put("ElytraB", "점프하지 않고 지상에서 바로 겉날개 활공을 시작했습니다");
        m.put("ElytraC", "겉날개 활공을 비정상적으로 잦은 빈도로 활성화했습니다");
        m.put("ElytraD", "겉날개를 착용하지 않은 상태에서 활공을 시작했습니다");
        m.put("ElytraE", "비행 모드 상태에서 겉날개 활공을 시작했습니다");
        m.put("ElytraF", "지면에 서 있는 상태에서 겉날개 활공을 시작했습니다");
        m.put("ElytraG", "공중부양(levitation) 효과가 걸린 상태에서 겉날개 활공을 시작했습니다");
        m.put("ElytraH", "탈것(보트, 마인카트 등)에 탑승한 상태에서 겉날개 활공을 시작했습니다");
        m.put("ElytraI", "물속에 있는 상태에서 겉날개 활공을 시작했습니다");
        m.put("EntitySpeed", "엔티티와 충돌하거나 통과할 때 허용된 물리 속도를 초과하여 이동했습니다");
        m.put("ExploitA", "모루에서 허용 길이를 초과하는 아이템 이름을 전송했습니다");
        m.put("ExploitB", "유효하지 않거나 악의적인 책 편집 패킷을 전송했습니다");
        m.put("FabricatedPlace", "블록 경계를 벗어난 커서 좌표로 블록 배치 패킷을 전송했습니다");
        m.put("FarBreak", "정상 도달 범위를 초과하는 먼 거리에서 블록을 파괴했습니다");
        m.put("FarPlace", "정상 도달 범위를 초과하는 먼 거리에 블록을 설치했습니다");
        m.put("FastBreak", "정상적인 채굴 속도를 초과하여 너무 빠르게 블록을 파괴했습니다 (FastBreak)");
        m.put("GroundSpoof", "공중에 떠 있는 상태에서 지면에 닿아 있다고 패킷을 위조했습니다 (GroundSpoof)");
        m.put("Hitboxes", "엔티티의 유효 피격 범위(히트박스) 바깥을 공격했습니다");
        m.put("InvalidBreak", "블록 파괴 시 존재할 수 없는 블록 면(face) ID를 전송했습니다");
        m.put("InvalidInteractCursor", "엔티티 상호작용 지점이 대상의 히트박스 범위를 벗어났습니다");
        m.put("InvalidInteractTarget", "존재하지 않는 엔티티와 상호작용을 시도했습니다");
        m.put("InvalidPlaceCursor", "블록 설치 시 유효하지 않은 커서 위치를 전송했습니다");
        m.put("InvalidPlaceFace", "블록 설치 시 존재할 수 없는 블록 면(face) ID를 전송했습니다");
        m.put("InventoryAction", "인벤토리를 조작하는 도중 월드와 상호작용하거나 대상을 공격했습니다");
        m.put("InventoryAutoClicker", "인벤토리 내에서 비정상적인 클릭 속도 또는 기계적 규칙성을 감지했습니다");
        m.put("KillAuraBot", "안티치트의 투명 킬오라 감지용 봇 엔티티를 공격했습니다");
        m.put("MultiActionsA", "아이템을 사용하는 도중(음식 섭취, 활 조준, 방패 등) 대상을 공격했습니다");
        m.put("MultiActionsB", "아이템을 사용하는 도중 동시에 블록을 파괴했습니다");
        m.put("MultiActionsC", "이동하거나 달리는 도중 인벤토리 슬롯을 클릭했습니다");
        m.put("MultiActionsD", "이동하는 도중 인벤토리 창을 닫았습니다");
        m.put("MultiActionsE", "아이템을 사용하는 도중 손을 휘둘렀습니다");
        m.put("MultiActionsF", "동일한 틱 내에서 블록과 엔티티 모두와 상호작용했습니다");
        m.put("MultiActionsG", "보트의 노를 젓는 도중 대상을 공격하거나 아이템을 사용했습니다");
        m.put("MultiBreak", "단일 이동 틱 내에 여러 개의 서로 다른 블록을 파괴하려고 시도했습니다");
        m.put("MultiInteractA", "동일한 틱 내에 여러 엔티티와 상호작용했습니다");
        m.put("MultiInteractB", "단일 틱 내에 서로 다른 타격 좌표를 가진 다중 상호작용 패킷을 전송했습니다");
        m.put("MultiPlace", "단일 틱 내에 여러 개의 블록을 설치했습니다");
        m.put("NegativeTimer", "정상적인 틱 주기보다 비정상적으로 느린 빈도로 이동 패킷을 전송했습니다");
        m.put("NoFall", "실제 지면에 닿지 않았음에도 낙하 피해를 피하기 위해 접지 패킷을 전송했습니다 (NoFall)");
        m.put("NoSlow", "아이템을 사용하는 동안 정상적으로 이동 속도가 감소하지 않았습니다 (NoSlow)");
        m.put("NoSwingBreak", "팔 휘두르기 애니메이션 패킷을 전송하지 않고 블록을 파괴했습니다");
        m.put("PacketOrderA", "아이템 줍기 및 빠른 이동 인벤토리 패킷이 비정상적인 순서로 전송되었습니다");
        m.put("PacketOrderB", "대상을 공격하기 전에 팔 휘두르기 패킷을 전송하지 않았습니다 (NoSwing)");
        m.put("PacketOrderC", "엔티티의 INTERACT 및 INTERACT_AT 패킷이 잘못된 순서로 전송되었습니다");
        m.put("PacketOrderD", "주손(오른손) 상호작용보다 왼손 상호작용 패킷이 먼저 전송되었습니다");
        m.put("PacketOrderE", "충돌하는 다른 행동을 수행하는 도중에 핫바 슬롯을 변경했습니다");
        m.put("PacketOrderF", "웅크리기나 질주 입력 이후 행동 패킷이 잘못된 순서로 전송되었습니다");
        m.put("PacketOrderG", "충돌하는 다른 행동 도중에 핫바나 인벤토리를 조작했습니다");
        m.put("PacketOrderH", "질주 및 웅크리기 상태 변경 패킷이 비정상적인 순서로 전송되었습니다");
        m.put("PacketOrderI", "공격, 사용, 해제, 채굴 패킷이 올바르지 않은 틱 순서로 전송되었습니다");
        m.put("PacketOrderJ", "공격 직후 필수 상호작용 패킷 없이 즉시 아이템을 사용했습니다");
        m.put("PacketOrderK", "인벤토리 열기, 클릭, 닫기 패킷이 잘못된 순서로 전송되었습니다");
        m.put("PacketOrderL", "아이템 버리기, 인벤토리 열기, 양손 교체 패킷이 잘못된 순서로 전송되었습니다");
        m.put("PacketOrderM", "아이템 사용 및 엔티티 상호작용 패킷이 올바르지 않은 순서로 전송되었습니다");
        m.put("PacketOrderN", "아이템 사용 및 블록 배치 패킷이 올바르지 않은 순서로 전송되었습니다");
        m.put("PacketOrderO", "클라이언트 틱 종료 이전에 이동 후 패킷을 잘못 전송했습니다");
        m.put("PacketOrderP", "청크 배치 패킷에 대한 트랜잭션 응답 순서가 잘못되었습니다");
        m.put("Phase", "이동 예측 시 고체 블록 내부로 파고들거나 통과했습니다 (Phase)");
        m.put("PositionBreakA", "시야 각도상 불가능한 눈 위치에서 블록 면을 파괴하려고 시도했습니다");
        m.put("PositionBreakB", "유효하지 않은 블록 면 지정으로 블록 채굴을 취소했습니다");
        m.put("PositionPlace", "시야에서 완전히 가려진 면에 블록을 설치했습니다");
        m.put("Post", "이동 틱의 예상 처리 순서를 벗어나 패킷을 전송했습니다");
        m.put("Reach", "허용된 최대 공격 사거리를 초과하여 엔티티를 공격했습니다 (Reach)");
        m.put("RotationBreak", "블록을 바라보지 않은 상태에서 파괴하려고 시도했습니다");
        m.put("RotationPlace", "설치 위치를 바라보지 않은 상태에서 블록을 설치했습니다");
        m.put("SelfInteract", "자기 자신과 상호작용하는 비정상 패킷을 전송했습니다");
        m.put("Simulation", "서버의 물리 시뮬레이션 예측값과 일치하지 않게 이동했습니다");
        m.put("SprintA", "질주 불가능한 낮은 배고픔 수치에서 질주(스프린트)했습니다");
        m.put("SprintB", "웅크리기(스니크) 또는 기어가는 도중 질주했습니다");
        m.put("SprintC", "아이템을 사용하는 도중에 질주했습니다");
        m.put("SprintD", "실명 디버프가 적용된 상태에서 질주를 시작했습니다");
        m.put("SprintE", "벽에 정면 충돌하여 막혀 있는 상태에서 계속 질주했습니다");
        m.put("SprintF", "겉날개로 활공하는 도중 질주 상태를 유지했습니다");
        m.put("SprintG", "물속에서 헤엄치지 않고 비정상적으로 질주했습니다");
        m.put("TickTimer", "클라이언트 틱 종료 패킷을 전송하지 않았습니다");
        m.put("Timer", "게임 클라이언트가 정상 주기보다 빠르게 동작하고 있습니다 (Timer)");
        m.put("TimerLimit", "높은 핑(지연) 이후 허용치를 초과하는 누적 패킷을 일시에 전송했습니다");
        m.put("TransactionOrder", "트랜잭션 또는 핑 응답 패킷이 비정상적인 순서로 전송되었습니다");
        m.put("VehicleA", "탈것 조종 패킷의 입력 수치가 물리적으로 불가능합니다");
        m.put("VehicleB", "탈것에 탑승하지 않은 상태에서 탑승 중이라고 패킷을 전송했습니다");
        m.put("VehicleC", "탈것의 이동이 서버의 물리 조종 예측과 일치하지 않습니다");
        m.put("VehicleD", "점프가 불가능한 탈것(보트, 마인카트 등)에서 점프했습니다");
        m.put("VehicleE", "보트에 타지 않은 상태에서 보트 노 젓기 상태 패킷을 전송했습니다");
        m.put("VehicleF", "잘못되거나 불가능한 보트 노 상태를 전송했습니다");
        m.put("VehicleTimer", "탈것 이동 패킷이 정상적인 틱 주기보다 빠르게 전송되었습니다");
        m.put("WrongBreak", "현재 채굴 중인 대상과 다른 블록에 대한 파괴 진행 패킷을 전송했습니다");
        TRANSLATIONS.put("ko", m);
    }

    public static String getDescription(String checkName, String fallback) {
        if (checkName == null) return fallback != null ? fallback : "";
        String lang = "en";
        try {
            if (GrimAPI.INSTANCE != null && GrimAPI.INSTANCE.getConfigManager() != null) {
                lang = GrimAPI.INSTANCE.getConfigManager().getLanguage();
            }
        } catch (Throwable ignored) {}
        if (lang == null || lang.isEmpty() || "en".equalsIgnoreCase(lang)) {
            return fallback != null ? fallback : "";
        }
        Map<String, String> langMap = TRANSLATIONS.get(lang.toLowerCase(java.util.Locale.ROOT));
        if (langMap != null) {
            String desc = langMap.get(checkName);
            if (desc != null && !desc.isEmpty()) {
                return desc;
            }
        }
        return fallback != null ? fallback : "";
    }
}
