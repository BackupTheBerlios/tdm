// $Id: MatchedNodes.java,v 1.5 2001/06/08 08:40:38 ctl Exp $
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class MatchedNodes {

  private BaseNode owner=null;
  private Set matches=new HashSet();

  public MatchedNodes(BaseNode aowner) {
    owner = aowner;
  }

  public void addMatch(BranchNode n) {
    matches.add(n);
  }

  public void delMatch(BranchNode n) {
    matches.remove(n);
  }

  public Set getMatches() {
    return matches;
  }

  public int getMatchCount() {
    return matches.size();
  }

  public BranchNode getFullMatch() {
    for( Iterator i=matches.iterator();i.hasNext();) {
      BranchNode fmatch = (BranchNode) i.next();
      if( fmatch.isMatch(BranchNode.MATCH_FULL))
        return fmatch;
    }
    return null;
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