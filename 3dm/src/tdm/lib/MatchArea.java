//$Id: MatchArea.java,v 1.1 2001/04/27 16:59:10 ctl Exp $

public class MatchArea {

  private int infoBytes = 0;

  public MatchArea() {
  }

  public void addInfoBytes( int i) {
    infoBytes+=i;
  }

  public int getInfoBytes() {
    return infoBytes;
  }
}