// $Id: NodeFactory.java,v 1.6 2001/09/05 21:22:29 ctl Exp $ D

/** Node factory. Used to build trees, whose node type is not known at
 *  compile time.
 */
public abstract class NodeFactory {

  public NodeFactory() {
  }

  public abstract Node makeNode( XMLNode content );
}