// $Id: Matching.java,v 1.22 2003/01/09 13:38:45 ctl Exp $
package tdm.lib;

public interface Matching {

  public void buildMatching( BaseNode base, BranchNode branch );
  public BaseNode getBaseRoot();
  public NodeFactory getBaseNodeFactory();
  public NodeFactory getBranchNodeFactory();
}