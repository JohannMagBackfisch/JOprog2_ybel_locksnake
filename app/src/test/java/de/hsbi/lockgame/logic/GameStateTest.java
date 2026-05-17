package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.*;
import org.junit.jupiter.api.Test;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class GameStateTest {



    private static Level emptyLevel5x5() {
        int w = 5, h = 5;
        var cells = new CellType[w][h];
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                cells[x][y] = CellType.EMPTY;
        return new Level(w, h, cells, List.of(), new Position(2, 2));
    }


    private static Level levelWithWall() {
        int w = 5, h = 5;
        var cells = new CellType[w][h];
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                cells[x][y] = CellType.EMPTY;
        cells[3][2] = CellType.WALL;
        return new Level(w, h, cells, List.of(), new Position(1, 2));
    }


    private static Level levelWithPin(Pin.State pinState) {
        int w = 5, h = 5;
        var cells = new CellType[w][h];
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                cells[x][y] = CellType.EMPTY;
        cells[3][2] = CellType.PIN_SLOT;
        var pin = new Pin(new Position(3, 2), pinState, Direction.RIGHT);
        return new Level(w, h, cells, List.of(pin), new Position(1, 2));
    }


    private static GameState runningState(Level level, Snake snake, List<Pin> pins, Direction dir) {
        return new GameState(level, snake, pins, GameState.Status.RUNNING, dir);
    }


    @Test
    void initialState_hasCorrectFields() {
        var level = emptyLevel5x5();
        var snake = new Snake(List.of(level.snakeStart()));
        var state = runningState(level, snake, List.of(), Direction.NONE);

        assertAll(
            () -> assertEquals(GameState.Status.RUNNING, state.status()),
            () -> assertEquals(Direction.NONE, state.pendingDirection()),
            () -> assertEquals(new Position(2, 2), state.snake().head()),
            () -> assertTrue(state.pins().isEmpty())
        );
    }


    @Test
    void tick_withNoDirection_doesNotMove() {
        var level = emptyLevel5x5();
        var snake = new Snake(List.of(level.snakeStart()));
        var state = runningState(level, snake, List.of(), Direction.NONE);

        var next = state.tick();

        assertSame(state, next, "Early-Exit: dasselbe Objekt erwartet");
    }


    @Test
    void tick_normalMove_snakeGrows() {
        var level = emptyLevel5x5();
        var snake = new Snake(List.of(new Position(2, 2)));
        var state = runningState(level, snake, List.of(), Direction.RIGHT);

        var next = state.tick();

        assertAll(
            () -> assertEquals(new Position(3, 2), next.snake().head()),
            () -> assertEquals(2, next.snake().body().size()),
            () -> assertEquals(GameState.Status.RUNNING, next.status())
        );
    }


    @Test
    void tick_wall_blocksMovement() {
        var level = levelWithWall();
        var snake = new Snake(List.of(new Position(2, 2)));
        var state = runningState(level, snake, List.of(), Direction.RIGHT);

        var next = state.tick();

        assertAll(
            () -> assertEquals(GameState.Status.RUNNING, next.status()),
            () -> assertEquals(Direction.NONE, next.pendingDirection()),
            () -> assertEquals(new Position(2, 2), next.snake().head())
        );
    }


    @Test
    void tick_outOfBounds_setsLostStatus() {
        var level = emptyLevel5x5();
        var snake = new Snake(List.of(new Position(0, 2)));
        var state = runningState(level, snake, List.of(), Direction.LEFT);

        var next = state.tick();

        assertEquals(GameState.Status.LOST_OUT_OF_BOUNDS, next.status());
    }


    @Test
    void tick_selfCollision_setsLostStatus() {
        var level = emptyLevel5x5();

        var snake = new Snake(List.of(
            new Position(2, 2),
            new Position(3, 2),
            new Position(3, 1),
            new Position(2, 1)
        ));
        var state = runningState(level, snake, List.of(), Direction.UP);

        var next = state.tick();

        assertEquals(GameState.Status.LOST_SELF_COLLISION, next.status());
    }


    @Test
    void tick_pinWrongDirection_blocks() {
        var level = emptyLevel5x5();
        var pin = new Pin(new Position(3, 2), Pin.State.LOW, Direction.RIGHT);
        var snake = new Snake(List.of(new Position(3, 1)));
        var state = runningState(level, snake, List.of(pin), Direction.DOWN);

        var next = state.tick();

        assertAll(
            () -> assertEquals(GameState.Status.RUNNING, next.status()),
            () -> assertEquals(Direction.NONE, next.pendingDirection()),
            () -> assertFalse(next.pins().getFirst().state().isSet(), "Pin muss LOW bleiben")
        );
    }


    @Test
    void tick_pinAlreadyHigh_blocks() {
        var level = emptyLevel5x5();
        var pin = new Pin(new Position(3, 2), Pin.State.HIGH, Direction.RIGHT);
        var snake = new Snake(List.of(new Position(2, 2)));
        var state = runningState(level, snake, List.of(pin), Direction.RIGHT);

        var next = state.tick();

        assertAll(
            () -> assertEquals(GameState.Status.RUNNING, next.status()),
            () -> assertEquals(Direction.NONE, next.pendingDirection()),
            () -> assertEquals(new Position(2, 2), next.snake().head(), "Schlange darf sich nicht bewegen")
        );
    }


    @Test
    void tick_pinActivated_pinBecomesHighSnakeStays() {
        var level = emptyLevel5x5();
        var pin1 = new Pin(new Position(3, 2), Pin.State.LOW, Direction.RIGHT);
        var pin2 = new Pin(new Position(1, 2), Pin.State.LOW, Direction.LEFT);
        var snake = new Snake(List.of(new Position(2, 2)));
        var state = runningState(level, snake, List.of(pin1, pin2), Direction.RIGHT);

        var next = state.tick();

        assertAll(
            () -> assertTrue(next.pins().get(0).state().isSet(), "Pin1 muss HIGH sein"),
            () -> assertFalse(next.pins().get(1).state().isSet(), "Pin2 muss noch LOW sein"),
            () -> assertEquals(new Position(2, 2), next.snake().head(), "Kopf darf sich nicht bewegen"),
            () -> assertEquals(GameState.Status.RUNNING, next.status())
        );
    }


    @Test
    void tick_lastPinActivated_statusWon() {
        var level = emptyLevel5x5();
        var pin = new Pin(new Position(3, 2), Pin.State.LOW, Direction.RIGHT);
        var snake = new Snake(List.of(new Position(2, 2)));
        var state = runningState(level, snake, List.of(pin), Direction.RIGHT);

        var next = state.tick();

        assertEquals(GameState.Status.WON, next.status());
    }


    @Test
    void tick_afterGameOver_noChange() {
        var level = emptyLevel5x5();
        var snake = new Snake(List.of(new Position(2, 2)));
        var state = new GameState(level, snake, List.of(), GameState.Status.WON, Direction.RIGHT);

        var next = state.tick();

        assertSame(state, next, "Nach Spielende muss dasselbe Objekt zurückgegeben werden");
    }


    @Test
    void tick_twoPins_firstActivated_notWonYet() {
        var level = emptyLevel5x5();
        var pin1 = new Pin(new Position(3, 2), Pin.State.LOW, Direction.RIGHT);
        var pin2 = new Pin(new Position(1, 2), Pin.State.LOW, Direction.LEFT);
        var snake = new Snake(List.of(new Position(2, 2)));
        var state = runningState(level, snake, List.of(pin1, pin2), Direction.RIGHT);

        var next = state.tick();

        assertAll(
            () -> assertEquals(GameState.Status.RUNNING, next.status()),
            () -> assertTrue(next.pins().get(0).state().isSet(),  "Pin1 muss HIGH sein"),
            () -> assertFalse(next.pins().get(1).state().isSet(), "Pin2 muss LOW bleiben")
        );
    }


    @Test
    void tick_threeSteps_snakeHasCorrectLength() {
        var level = emptyLevel5x5();
        var snake = new Snake(List.of(new Position(0, 2)));
        var state = runningState(level, snake, List.of(), Direction.RIGHT);

        var s1 = state.tick();
        var s2 = s1.tick();
        var s3 = s2.tick();

        assertAll(
            () -> assertEquals(new Position(3, 2), s3.snake().head()),
            () -> assertEquals(4, s3.snake().body().size())
        );
    }
}
