package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.Direction;
import de.hsbi.lockgame.model.Level;
import de.hsbi.lockgame.model.Snake;
import de.hsbi.lockgame.ui.GamePanel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public final class GameEngine {
    private GameState state;
    private List<Consumer<GameState>> observers = new ArrayList<>();

  public GameEngine(Level level) {
    var snake = new Snake(List.of(level.snakeStart()));
    this.state = new GameState(level, snake, level.pins(), GameState.Status.RUNNING,Direction.NONE);
  }

  public GameState state() {
    return state;
  }

    public void addObserver(Consumer<GameState> observer) {
        observers.add(observer);
    }

    public void notifyObservers(){
      observers.forEach(observer -> observer.accept(state));
    }

  public void setGamePanel(GamePanel panel) {
    addObserver(panel::update);
  }

  public void update(Direction d) {
    state = new GameState(state.level(), state.snake(), state.pins(), state.status(), d);
    notifyObservers();
  }

  public void tick() {
      state = state.tick();
      notifyObservers();
  }
}
