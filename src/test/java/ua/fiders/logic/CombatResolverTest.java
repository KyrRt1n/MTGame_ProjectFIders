package ua.fiders.logic;

import org.junit.jupiter.api.*;
import ua.fiders.model.*;
import ua.fiders.model.cards.CreatureCard;
import ua.fiders.model.enums.CardKeywords;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 тести для CombatResolver.
 *
 * Покриваємо:
 *  1. Атака без блокування → шкода гравцю
 *  2. Атака з блокуванням → шкода між істотами
 *  3. Perk Flying: наземний блокер ігнорується (Reach — виняток)
 *  4. Perk Trample: надлишок шкоди проходить гравцю
 *  5. Perk Lifelink: власник відновлює HP
 *  6. Загибель істоти → переміщення на кладовище
 *  7. Взаємне знищення атакуючого і блокуючого
 */
@DisplayName("CombatResolver Tests")
class CombatResolverTest {

    private CombatResolver resolver;
    private Player attackingPlayer;
    private Player defendingPlayer;
    private GameState state;

    // ---------- setup ----------

    @BeforeEach
    void setUp() {
        resolver        = new CombatResolver();
        attackingPlayer = new Player("Attacker");
        defendingPlayer = new Player("Defender");
        state           = new GameState(attackingPlayer, defendingPlayer);
        // currentPlayer = attackingPlayer за замовчуванням
    }

    // ---------- helpers ----------

    /** Створює Permanent-Істоту на полі для заданого гравця. */
    private Permanent makeCreature(Player owner, String name,
                                   int attack, int hp,
                                   CardKeywords... keywords) {
        Set<CardKeywords> kwSet = keywords.length > 0
                ? new HashSet<>(Arrays.asList(keywords))
                : Collections.emptySet();
        CreatureCard card = new CreatureCard(name, 1, kwSet, "img", attack, hp);
        Permanent p = new Permanent(card, owner);
        p.untap();
        state.getBattlefield().add(p);
        return p;
    }

    // ================================================================
    //  1. Атака без блокування → шкода гравцю
    // ================================================================

    @Test
    @DisplayName("Атака без блокування: шкода йде захиснику")
    void unblocked_attack_deals_damage_to_defending_player() {
        Permanent attacker = makeCreature(attackingPlayer, "Wolf", 3, 3);

        resolver.resolveCombat(List.of(attacker), Collections.emptyMap(), state);

        assertEquals(17, defendingPlayer.getHp(),
                "Захисник повинен отримати 3 шкоди (20 - 3 = 17)");
        assertEquals(20, attackingPlayer.getHp(),
                "Атакуючий гравець не отримує шкоди");
    }

    // ================================================================
    //  2. Атака з блокуванням → шкода між істотами
    // ================================================================

    @Test
    @DisplayName("Бій 2/2 vs 2/2 → обидві гинуть, гравець не отримує шкоди")
    void blocked_attack_creatures_deal_each_other_damage() {
        Permanent attacker = makeCreature(attackingPlayer, "Attacker", 2, 2);
        Permanent blocker  = makeCreature(defendingPlayer, "Blocker",  2, 2);

        resolver.resolveCombat(List.of(attacker), Map.of(attacker, blocker), state);

        // Обидві мертві — видалені з поля
        assertFalse(state.getBattlefield().contains(attacker),
                "Атакуючий має бути мертвим і видаленим з поля");
        assertFalse(state.getBattlefield().contains(blocker),
                "Блокуючий має бути мертвим і видаленим з поля");

        // Гравець не отримує шкоди
        assertEquals(20, defendingPlayer.getHp(),
                "Гравець-захисник не отримує шкоди, бо атака заблокована");
    }

    // ================================================================
    //  3. Perk Flying: наземний блокер ігнорується
    // ================================================================

    @Test
    @DisplayName("Flying: наземний блокер не може блокувати — шкода йде гравцю")
    void flying_attacker_ignores_ground_blocker() {
        Permanent attacker = makeCreature(attackingPlayer, "Dragon", 4, 4, CardKeywords.FLYING);
        Permanent blocker  = makeCreature(defendingPlayer, "Knight",  3, 3); // без Flying

        // Блокер призначений, але не має Flying — CombatResolver скасовує блок
        resolver.resolveCombat(List.of(attacker), Map.of(attacker, blocker), state);

        // Захисник отримує повну шкоду
        assertEquals(16, defendingPlayer.getHp(),
                "Наземний блокер не може блокувати Flying — шкода 4 йде гравцю (20-4=16)");

        // Блокер живий
        assertTrue(state.getBattlefield().contains(blocker),
                "Наземний блокер не бере участі у бою — має залишитися живим");
    }

    @Test
    @DisplayName("Flying vs Flying: блок дійсний")
    void flying_vs_flying_block_is_valid() {
        Permanent attacker = makeCreature(attackingPlayer, "Gryphon", 3, 3, CardKeywords.FLYING);
        Permanent blocker  = makeCreature(defendingPlayer, "Harpy",   3, 3, CardKeywords.FLYING);

        resolver.resolveCombat(List.of(attacker), Map.of(attacker, blocker), state);

        // Обидві гинуть від рівних сил
        assertFalse(state.getBattlefield().contains(attacker));
        assertFalse(state.getBattlefield().contains(blocker));

        // Гравець не отримує шкоди
        assertEquals(20, defendingPlayer.getHp());
    }

    // ================================================================
    //  4. Perk Trample: надлишок шкоди проходить гравцю
    // ================================================================

    @Test
    @DisplayName("Trample: 5 атаки vs 2 hp блокера → 3 шкоди гравцю")
    void trample_excess_damage_hits_player() {
        Permanent attacker = makeCreature(attackingPlayer, "Rhino", 5, 5, CardKeywords.TRAMPLE);
        Permanent blocker  = makeCreature(defendingPlayer, "Rat",   1, 2); // hp = 2

        resolver.resolveCombat(List.of(attacker), Map.of(attacker, blocker), state);

        // Блокер мертвий
        assertFalse(state.getBattlefield().contains(blocker),
                "Блокер загинув (2hp < 5 atk)");

        // 5 - 2 = 3 шкоди пройшло гравцю
        assertEquals(17, defendingPlayer.getHp(),
                "Гравець отримує 3 надлишкової шкоди від Trample (20-3=17)");
    }

    @Test
    @DisplayName("Trample без надлишку: шкода = hp блокера → гравець не отримує шкоди")
    void trample_no_excess_when_equal() {
        Permanent attacker = makeCreature(attackingPlayer, "Boar", 3, 4, CardKeywords.TRAMPLE);
        Permanent blocker  = makeCreature(defendingPlayer, "Guard", 1, 3); // hp = 3

        resolver.resolveCombat(List.of(attacker), Map.of(attacker, blocker), state);

        // Blocker hp = 3 = attacker atk 3 → точно вистачає, надлишку нема
        assertEquals(20, defendingPlayer.getHp(),
                "Рівно вистачило — надлишку немає, гравець не отримує шкоди");
    }

    // ================================================================
    //  5. Perk Lifelink
    // ================================================================

    @Test
    @DisplayName("Lifelink: атакуючий лікує власника при нанесенні шкоди гравцю")
    void lifelink_heals_owner_on_unblocked_attack() {
        attackingPlayer.setHp(15); // Починаємо з 15 для наочності
        Permanent attacker = makeCreature(attackingPlayer, "Vampire", 3, 3, CardKeywords.LIFELINK);

        resolver.resolveCombat(List.of(attacker), Collections.emptyMap(), state);

        // Захисник отримує 3 шкоди
        assertEquals(17, defendingPlayer.getHp());
        // Власник Lifelink-Vampire лікується на 3
        assertEquals(18, attackingPlayer.getHp(),
                "Lifelink повинен вилікувати власника на кількість нанесеної шкоди (15+3=18)");
    }

    @Test
    @DisplayName("Lifelink у бою: блокер з Lifelink лікує свого власника")
    void lifelink_heals_owner_when_blocking() {
        defendingPlayer.setHp(14);
        Permanent attacker = makeCreature(attackingPlayer, "Warrior", 3, 3);
        Permanent blocker  = makeCreature(defendingPlayer, "HolyGuard", 2, 4, CardKeywords.LIFELINK);

        resolver.resolveCombat(List.of(attacker), Map.of(attacker, blocker), state);

        // blocker нанів 2 шкоди attacker — власник (defendingPlayer) лікується на 2
        assertEquals(16, defendingPlayer.getHp(),
                "Defender (власник блокера з Lifelink) лікується на 2 (14+2=16)");
    }

    // ================================================================
    //  6. Загибель → кладовище
    // ================================================================

    @Test
    @DisplayName("Мертва істота переміщується на кладовище власника")
    void dead_creature_goes_to_graveyard() {
        Permanent attacker = makeCreature(attackingPlayer, "Giant", 5, 5);
        Permanent blocker  = makeCreature(defendingPlayer, "Squire", 1, 2);

        resolver.resolveCombat(List.of(attacker), Map.of(attacker, blocker), state);

        // Blocker з 2 hp від 5 atk — мертвий
        assertTrue(defendingPlayer.getGraveyard().contains(blocker.getBaseCard()),
                "Мертва картка повинна опинитися в кладовищі власника");
        assertFalse(state.getBattlefield().contains(blocker));
    }

    // ================================================================
    //  7. Взаємне знищення
    // ================================================================

    @Test
    @DisplayName("3/3 атакує 3/3 → обидва гинуть одночасно")
    void mutual_destruction_both_die() {
        Permanent attacker = makeCreature(attackingPlayer, "A", 3, 3);
        Permanent blocker  = makeCreature(defendingPlayer, "B", 3, 3);

        resolver.resolveCombat(List.of(attacker), Map.of(attacker, blocker), state);

        assertFalse(state.getBattlefield().contains(attacker));
        assertFalse(state.getBattlefield().contains(blocker));
        assertTrue(attackingPlayer.getGraveyard().contains(attacker.getBaseCard()));
        assertTrue(defendingPlayer.getGraveyard().contains(blocker.getBaseCard()));
    }

    // ================================================================
    //  8. Кілька атакуючих
    // ================================================================

    @Test
    @DisplayName("Декілька атакуючих: кожен незаблокований — шкода додається")
    void multiple_unblocked_attackers_deal_combined_damage() {
        Permanent a1 = makeCreature(attackingPlayer, "Goblin1", 2, 2);
        Permanent a2 = makeCreature(attackingPlayer, "Goblin2", 3, 3);

        resolver.resolveCombat(List.of(a1, a2), Collections.emptyMap(), state);

        assertEquals(15, defendingPlayer.getHp(),
                "Два незаблоковані атакуючі (2+3=5) зменшують HP з 20 до 15");
    }
}