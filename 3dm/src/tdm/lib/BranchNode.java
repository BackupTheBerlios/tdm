// $Id

public class BranchNode extends Node {

  MatchedNodes partners = null;


  public BranchNode( Node aParent, int achildPos ) {
    super( aParent,achildPos);
  }


  public BranchNode getChild( int ix ) {
    return (BranchNode) children.elementAt(ix);
  }

  public BranchNode getParent() {
    return (BranchNode) parent;
  }

  // Possibly not needed in final version!
  public void setPartners(MatchedNodes p) {
    partners = p;
  }

  public void debug( java.io.PrintWriter pw, int indent ) {
    super.debug(pw, indent);
    String ind = "                                                   ".substring(0,indent+1);
    pw.println(ind+"Partners are:");
    if(partners != null ) {
      partners.debug(pw,indent+1);
      pw.println(ind+"---");
    }
  }

}