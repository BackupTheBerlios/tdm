// $Id: AreaNode.java,v 1.2 2001/03/31 15:32:08 ctl Exp $
// PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE

import Node;
import java.util.*;

public class AreaNode /* extends Node*/ {

  public Vector children = new Vector();
  public Vector bottomNodes = new Vector();
  Vector candidates = new Vector();
  public double matchCount=0;
  private ONode srcNode;

  public AreaNode(ONode asrcNode) {
    srcNode = asrcNode;
  }

  public void addChild( AreaNode n ) {
    children.add( n );
  }

  public ONode getNode() {
    return srcNode;
  }

  public int getChildCount() {
    return children.size();
  }

  public AreaNode getChild(int ix) {
    return (AreaNode) children.elementAt(ix);
  }


  public void clearCandidates() {
    candidates.clear();
  }

  public void addCandidate( ONode dstNode ) {
    candidates.add( dstNode );
  }

  public int getCandidateCount() {
    return candidates.size();
  }

  public ONode getCandidate(int ix) {
    return (ONode) candidates.elementAt(ix);
  }

  public String toString() {
    //return srcNode.toString();
    if( candidates.size() != 1 )
      return candidates.toString()+" (" +matchCount +")";
    else
      return "* (" +matchCount +")";
  }
}

