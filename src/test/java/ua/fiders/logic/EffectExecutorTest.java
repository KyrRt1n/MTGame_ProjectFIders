package ua.fiders.logic;

import org.junit.jupiter.api.*;
import ua.fiders.model.*;
import ua.fiders.model.cards.Card;
import ua.fiders.model.cards.CreatureCard;
import ua.fiders.model.effects.*;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 тести для EffectExecutor.
 *
 * Покриваємо кожен тип ефекту:
 *  1. DrawCardEffect  — бере карти з колоди в руку
 *  2. HealPlayerEffect — додає HP заклинателю
 *  3. DamageEnemyEffect — знімає HP ворогу
 *  4. DrawCardEffect при порожній колоді — не падає
 *  5. HealPlayerEffect не перевищує обмежень (якщо буде)
 *  6. DamageEnemyEffect: вороже HP може стати від'ємним (фіксуємо поточну поведінку)
 */
@DisplayName("EffectExecutor Tests")
class EffectExecutorTest {

    private EffectExecutor executor;
    private Player caster;
    private Player enemy;
    private GameState state;

    // ---------- setup ----------

    @BeforeEach
    void setUp() {
        executor = new EffectExecutor();
        caster   = new Player("Caster");
        enemy    = new Player("Enemy");
        state    = new GameState(caster, enemy);
        // GameState.currentPlayer = caster за замовчуванням
    }

    // ---------- helpers ----------

    private Card dummyCard(String name) {
        return new CreatureCard(name, 1, Collections.emptySet(), "img", 1, 1);
    }

    // ================================================================
    //  1. DrawCardEffect: бере N карт з колоди в руку
    // ================================================================

    @Test
    @DisplayName("DrawCardEffect(2): 2 карти переходять з колоди в руку")
    void draw_2_cards_moves_them_to_hand() {
        // Кладемо 3 карти в колоду
        caster.getDeck().add(dummyCard("C1"));
        caster.getDeck().add(dummyCard("C2"));
        caster.getDeck().add(dummyCard("C3"));

        executor.execute(new DrawCardEffect(2), caster, state);

        assertEquals(2, caster.getHand().size(),
                "Після DrawCardEffect(2) в руці повинно бути 2 картки");
        assertEquals(1, caster.getDeck().size(),
                "В колоді повинна залишитися 1 картка");
    }

    @Test
    @DisplayName("DrawCardEffect(1): одна карта переходить в руку")
    void draw_1_card_moves_one_card_to_hand() {
        caster.getDeck().add(dummyCard("Card"));
        int handBefore = caster.getHand().size();

        executor.execute(new DrawCardEffect(1), caster, state);

        assertEquals(handBefore + 1, caster.getHand().size());
        assertTrue(caster.getDeck().isEmpty());
    }

    // ================================================================
    //  2. DrawCardEffect при порожній колоді — не падає з NPE
    // ================================================================

    @Test
    @DisplayName("DrawCardEffect при порожній колоді: не кидає виняток")
    void draw_card_empty_deck_no_exception() {
        assertTrue(caster.getDeck().isEmpty(), "Колода порожня");

        assertDoesNotThrow(() ->
                executor.execute(new DrawCardEffect(3), caster, state),
                "Не повинно бути виключення при спробі взяти карту з порожньої колоди"
        );

        assertEquals(0, caster.getHand().size(),
                "Рука повинна залишитися порожньою — брати нічого");
    }

    // ================================================================
    //  3. HealPlayerEffect: збільшує HP заклинателю
    // ================================================================

    @Test
    @DisplayName("HealPlayerEffect(5): HP заклинателя зростає на 5")
    void heal_increases_caster_hp() {
        caster.setHp(10);

        executor.execute(new HealPlayerEffect(5), caster, state);

        assertEquals(15, caster.getHp(),
                "HP повинно збільшитися з 10 до 15");
    }

    @Test
    @DisplayName("HealPlayerEffect(0): HP не змінюється")
    void heal_zero_does_not_change_hp() {
        caster.setHp(12);

        executor.execute(new HealPlayerEffect(0), caster, state);

        assertEquals(12, caster.getHp(),
                "HP не повинно змінюватись при лікуванні на 0");
    }

    @Test
    @DisplayName("HealPlayerEffect не впливає на HP ворога")
    void heal_does_not_affect_enemy_hp() {
        enemy.setHp(20);

        executor.execute(new HealPlayerEffect(10), caster, state);

        assertEquals(20, enemy.getHp(),
                "HP ворога не повинно змінюватись від лікування заклинателя");
    }

    // ================================================================
    //  4. DamageEnemyEffect: знімає HP ворогу
    // ================================================================

    @Test
    @DisplayName("DamageEnemyEffect(5): HP ворога зменшується на 5")
    void damage_enemy_decreases_enemy_hp() {
        enemy.setHp(20);

        executor.execute(new DamageEnemyEffect(5), caster, state);

        assertEquals(15, enemy.getHp(),
                "HP ворога повинно зменшитись з 20 до 15");
    }

    @Test
    @DisplayName("DamageEnemyEffect(1): мінімальна шкода 1")
    void damage_enemy_by_one() {
        enemy.setHp(8);

        executor.execute(new DamageEnemyEffect(1), caster, state);

        assertEquals(7, enemy.getHp());
    }

    @Test
    @DisplayName("DamageEnemyEffect не впливає на HP заклинателя")
    void damage_enemy_does_not_affect_caster_hp() {
        caster.setHp(20);

        executor.execute(new DamageEnemyEffect(7), caster, state);

        assertEquals(20, caster.getHp(),
                "HP заклинателя не повинно змінюватись від DamageEnemyEffect");
    }

    @Test
    @DisplayName("DamageEnemyEffect(20): HP ворога стає 0 або менше — ігрова умова перемоги")
    void damage_enemy_lethal() {
        enemy.setHp(20);

        executor.execute(new DamageEnemyEffect(20), caster, state);

        assertTrue(enemy.getHp() <= 0,
                "Смертельна шкода повинна знизити HP ворога до 0 або менше");
    }

    // ================================================================
    //  5. Комбо-ефект: кілька ефектів підряд
    // ================================================================

    @Test
    @DisplayName("Комбо: Heal + Damage незалежні один від одного")
    void combo_heal_and_damage_are_independent() {
        caster.setHp(10);
        enemy.setHp(20);

        executor.execute(new HealPlayerEffect(5), caster, state);
        executor.execute(new DamageEnemyEffect(8), caster, state);

        assertEquals(15, caster.getHp(), "Заклинатель вилікований на 5");
        assertEquals(12, enemy.getHp(),  "Ворог отримав 8 шкоди");
    }

    // ================================================================
    //  6. Симетрія: caster та enemy залежать від GameState.getOpponent()
    // ================================================================

    @Test
    @DisplayName("Коли caster = player2, ворог = player1")
    void enemy_is_opponent_of_caster_in_state() {
        // state: player1=caster, player2=enemy
        // getOpponent(caster) = enemy → все вірно
        enemy.setHp(20);

        executor.execute(new DamageEnemyEffect(3), caster, state);

        assertEquals(17, enemy.getHp(),
                "state.getOpponent(caster) повертає enemy — шкода правильно направлена");
        assertEquals(20, caster.getHp());
    }
}
