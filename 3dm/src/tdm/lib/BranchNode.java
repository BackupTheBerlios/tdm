// $Id

public class BranchNode extends Node {

  private MatchedNodes partners = null;
  private BaseNode baseMatch = null;

  public BranchNode( Node aParent, int achildPos, XMLNode aContent ) {
    super( aParent,achildPos);
    content = aContent;
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

  public MatchedNodes getPartners() {
    return partners;
  }

  // Possibly not needed in final version!
  public void setBaseMatch(BaseNode p) {
    baseMatch = p;
  }

  public BaseNode getBaseMatch() {
    return baseMatch;
  }


  public void debug( java.io.PrintWriter pw, int indent ) {
    super.debug(pw, indent);
    String ind = "                                                   ".substring(0,indent+1);
    pw.println(ind+(partners != null ? "Partners are:" : "(no partners)"));
    if(partners != null ) {
      partners.debug(pw,indent+1);
      pw.println(ind+"---");
    }
  }

}