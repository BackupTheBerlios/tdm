// $Id: Matching.java,v 1.21 2002/10/25 11:35:06 ctl Exp $

public interface Matching {

  public void buildMatching( BaseNode base, BranchNode branch );
  public BaseNode getBaseRoot();
  public NodeFactory getBaseNodeFactory();
  public NodeFactory getBranchNodeFactory();
}