// $Id: NodeFactory.java,v 1.5 2001/04/27 16:59:10 ctl Exp $

public abstract class NodeFactory {

  public NodeFactory() {
  }

  public abstract Node makeNode( XMLNode content );
}