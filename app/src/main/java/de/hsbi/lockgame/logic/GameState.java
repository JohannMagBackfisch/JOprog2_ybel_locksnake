package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.*;
import java.util.List;

public final class GameState {
    private Level level;
    private Snake snake;
    private List<Pin> pins;
    private Status status;
    private Direction pendingDirection;


  public GameState(
      Level level, Snake snake, List<Pin> pins, Status status, Direction pendingDirection) {
    this.level = level;
    this.snake = snake;
    this.pins = List.copyOf(pins);
    this.status = status;
    this.pendingDirection = pendingDirection;
  }

  public Level level() {
    return level;
  }

  public Snake snake() {
    return snake;
  }

  public List<Pin> pins() {
    return pins;
  }

  public Status status() {

    return status;
  }

  public Direction pendingDirection() {
    return pendingDirection;
  }

  public GameState tick() {

      //Early exit
      if (!status.isRunning() || pendingDirection == Direction.NONE) {
          return this;
      }

      var nextHead = snake.nextHead(pendingDirection);

      // Out of bounds
      if (!level.isInside(nextHead)) {
          return new GameState(level, snake, pins, Status.LOST_OUT_OF_BOUNDS, Direction.NONE);
      }

      // Wand
      if (level.cellAt(nextHead) == CellType.WALL) {
          return new GameState(level, snake, pins, Status.RUNNING, Direction.NONE);
      }

      // Selbstkollision
      if (snake.occupies(nextHead)) {
          return new GameState(level, snake, pins, Status.LOST_SELF_COLLISION, Direction.NONE);
      }

      var maybePin = pins.stream()
          .filter(p -> p.position().equals(nextHead))
          .findFirst();

      if (maybePin.isPresent()) {
          var pin = maybePin.get();

          // Pin blockiert
          if (pin.state().isSet() || !pin.activationDirection().equals(pendingDirection)) {
              return new GameState(level, snake, pins, Status.RUNNING, Direction.NONE);
          }

          // Pin aktivieren
          var updatedPins = pins.stream()
              .map(p -> p.position().equals(nextHead) ? p.withState(Pin.State.HIGH) : p)
              .toList();

          var newStatus = updatedPins.stream().allMatch(p -> p.state().isSet())
              ? Status.WON
              : Status.RUNNING;

          return new GameState(level, snake, updatedPins, newStatus, Direction.NONE);

      }

      return new GameState(level, snake.grow(pendingDirection), pins, Status.RUNNING, pendingDirection);
  }




      public enum Status {
    RUNNING,
    WON,
    LOST_SELF_COLLISION,
    LOST_OUT_OF_BOUNDS;

    public boolean isRunning() {
      return this == RUNNING;
    }
  }
}
