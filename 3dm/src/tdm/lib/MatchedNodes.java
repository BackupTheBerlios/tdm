// $Id: MatchedNodes.java,v 1.1 2001/03/14 08:23:54 ctl Exp $
import java.util.Set;
import java.util.HashSet;

public class MatchedNodes {

  BaseNode owner=null;
  Set matches=new HashSet();

  public MatchedNodes(BaseNode aowner) {
    owner = aowner;
  }

  public void addMatch(BranchNode n) {
    matches.add(n);
  }

  public void debug( java.io.PrintWriter pw, int indent ) {
    String ind = "                                                   ".substring(0,indent+1);
    java.util.Iterator i = matches.iterator();
    while( i.hasNext() ) {
      XMLNode n = ((Node) i.next()).content;
      pw.println(ind+ (n  == null ? "(null)" : n.toString() ) );
    }
  }

}