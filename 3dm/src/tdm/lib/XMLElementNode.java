// $Id: XMLElementNode.java,v 1.1 2001/03/14 08:23:55 ctl Exp $

import org.xml.sax.Attributes;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Map;
//import java.io.PrintWriter;

public class XMLElementNode extends XMLNode {

//Probably not needed  public String nameSpace = null;
  public String name = null;
  public Map attributes = null;
//PROTO CODE
  public XMLElementNode( String aname, Map attr ) {
    name = aname;
    attributes = attr;
  }
//PROTO CODE ENDS
  public XMLElementNode( String aname, Attributes attr ) {
//    System.out.println("EN " +ename +"; "+ (attr == null ? "(null)" : attr.toString() ));

//    nameSpace = ns;
    name = aname;

    if( attr != null ) {
      attributes = new java.util.Hashtable();
      for( int i=0;i<attr.getLength();i++) {
        attributes.put(attr.getQName(i) /*.getLocalName(i)*/,attr.getValue(i));
      }
    }

  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(name);
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

    }
    sb.append('}');
    return sb.toString();
  }

  public boolean contentEquals( Object a ) {
    if( a instanceof XMLElementNode )
      return ((XMLElementNode) a).name.equals(name) &&
//             ((XMLElementNode) a).nameSpace.equals(nameSpace) &&
             compareAttributes( ((XMLElementNode) a).attributes, attributes
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
  }

/*
  public void printXML1( PrintWriter pw, int level, boolean assumeNoChildren ) {
    StringBuffer tagopen = new StringBuffer();
    tagopen.append('<');
    tagopen.append( name );
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


  public void printTree(int level) {
    super.printTree(level);
    for(int i=0;i<children.size();i++)
      ((Node) children.elementAt(i)).printTree(level+1);
  } */
}