package de.hsbi.lockgame.model;

import java.util.Objects;

public final class Position {
  private final int x;
  private final int y;

  public Position(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public int x() {
    return x;
  }

  public int y() {
    return y;
  }

  @Override
    public boolean equals(Object o){
      if(this == o) return true;

      if(!(o instanceof Position p)) return false;

      return x == p.x && y ==p.y;
  }

  @Override
    public int hashCode(){
      return Objects.hash(x,y);
  }
  @Override
    public String toString(){
      return "(" + x + ", " + y + ")";
  }

}
