// $Id: ElementNode.java,v 1.1 2001/03/14 08:23:54 ctl Exp $
// PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE

import org.xml.sax.Attributes;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Map;
import java.io.PrintWriter;

public class ElementNode extends ONode {
  public Vector children = new Vector();
  public String nameSpace = null;
  public String name = null;
  public Map attributes = null;

  public ElementNode( String ns, String ename, Attributes attr ) {
//    System.out.println("EN " +ename +"; "+ (attr == null ? "(null)" : attr.toString() ));

    nameSpace = ns;
    name = ename;

    if( attr != null ) {
      attributes = new java.util.Hashtable();
      for( int i=0;i<attr.getLength();i++) {
        attributes.put(attr.getQName(i) /*.getLocalName(i)*/,attr.getValue(i));
      }
    }

  }

  public int getChildCount() {
    return children.size();
  }

  public ONode getChild( int ix ) {
    return (ONode) children.elementAt(ix);
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
/*    if( !"".equals(nameSpace) ) {
      sb.append(nameSpace);
      sb.append(':');
    }*/
    sb.append(name);
/*    sb.append('(');
    sb.append(super.toString());
    sb.append(')');
*/
    sb.append(" {");
    if( attributes != null && !attributes.isEmpty() ) {
      java.util.Iterator iter = attributes.keySet().iterator();
      while( iter.hasNext() ) {
        sb.append(' ');
        Object key = iter.next();
        sb.append(key.toString());
        sb.append('=');
        sb.append(attributes.get(key).toString());
      }

    /*      sb.append('!');
      for( int i=0;i<attributes.getLength();i++) {
        if( i > 0 )
          sb.append(',');
        sb.append(attributes.getLocalName(i));
        sb.append('=');
        sb.append(attributes.getValue(i));
      }
*/
    }
    sb.append('}');
    return sb.toString();
  }

  public void printXML1( PrintWriter pw, int level, boolean assumeNoChildren ) {
    StringBuffer tagopen = new StringBuffer();
    tagopen.append('<');
    tagopen.append( name );
//    tagopen.append(' ');
    if( attributes != null && !attributes.isEmpty() ) {
      java.util.Iterator iter = attributes.keySet().iterator();
      while( iter.hasNext() ) {
        tagopen.append(' ');
        Object key = iter.next();
        tagopen.append(key.toString());
        tagopen.append('=');
        tagopen.append('"');
        tagopen.append(attributes.get(key).toString());
        tagopen.append('"');
      }
    }
    if( assumeNoChildren )
      tagopen.append("/>");
    else
      tagopen.append('>');
    pw.println("                                               ".substring(0,2*level) + tagopen.toString());
  }

  public void printXML2( PrintWriter pw, int level ) {
    pw.println("                                               ".substring(0,2*level) + "</"+name+">");
  }


  public boolean contentEquals( Object a ) {
    if( a instanceof ElementNode )
      return ((ElementNode) a).name.equals(name) &&
             ((ElementNode) a).nameSpace.equals(nameSpace) &&
             compareAttributes( ((ElementNode) a).attributes, attributes
             );
    else
      return false;
  }

  public boolean compareAttributes( Map a, Map b ) {
    if( a==b )
      return true; // Either both are null, or point to same obj
    if( a==null || b== null)
      return false; // either is null, the other not
    return a.equals(b);

    /*
    if( a.getLength() != b.getLength() )
      return false;
    for( int i=0; i < a.getLength(); i ++ ) {
      int matchIx = b.getIndex( a.getURI(i), a.getLocalName(i));
      if( matchIx == -1 )
        return false; // The attribute could not be found
      if( !a.getValue(i).equals(b.getValue(matchIx)) ||
          !a.getType(i).equals( b.getType(matchIx)) )
        return false;
    }
    return true;*/
  }

  public void addChild( ONode child ) {
    children.add(child);
    child.parent = this;
    child.childNo = children.size()-1;
  }

  public void printTree(int level) {
    super.printTree(level);
    for(int i=0;i<children.size();i++)
      ((ONode) children.elementAt(i)).printTree(level+1);
  }
}