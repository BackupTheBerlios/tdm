// $Id: XMLElementNode.java,v 1.4 2001/04/19 13:59:04 ctl Exp $

import org.xml.sax.Attributes;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Map;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.Attributes;
import java.security.MessageDigest;
//import java.io.PrintWriter;

public class XMLElementNode extends XMLNode {

//Probably not needed  public String nameSpace = null;
  private String name = null;
  private AttributesImpl attributes = null;
  private int nHashCode = -1;
  private byte[] attrHash = null;

//PROTO CODE
  public XMLElementNode( String aname,  Map attr ) {
    name = aname;
    attributes = new AttributesImpl();
    if( attr ==null )
      return;
    java.util.Iterator iter = attr.keySet().iterator();
    while( iter.hasNext() ) {
      String key = (String) iter.next();
      attributes.addAttribute("","",key,"",(String) attr.get(key));
    }
  }
//PROTO CODE ENDS
  public XMLElementNode( String aname, Attributes attr ) {
    name = aname;
    attributes = new AttributesImpl( attr );
    makeHash();
  }

  private void makeHash() {
    nHashCode = name.hashCode();
    MessageDigest md = getMD();
    for( int i=0;i<attributes.getLength();i++) {
      md.update( calculateHash( attributes.getQName(i) ) );
      md.update( calculateHash( attributes.getValue(i) ) );
    }
    attrHash = md.digest();
  }

  //DUMMY!
  public String getNamespaceURI() {
    return "";
  }

  //DUMMY!
  public String getLocalName() {
    return "";
  }

  public String getQName() {
    return name;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(name);
    sb.append(" {");
    if( attributes != null && attributes.getLength() > 0) {
      for( int i = 0;i<attributes.getLength();i++) {
        sb.append(' ');
        sb.append(attributes.getQName(i) );
        sb.append('=');
        sb.append(attributes.getValue(i));
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

  public boolean compareAttributes( Attributes a, Attributes b ) {
    if( a==b )
      return true; // Either both are null, or point to same obj
    if( a==null || b== null)
      return false; // either is null, the other not
    if( a.getLength() != b.getLength() )
      return false; // Not equally many
    for( int i = 0; i<a.getLength(); i ++ ) {
      if( !a.getURI(i).equals(b.getURI(i)) ||
          !a.getLocalName(i).equals(b.getLocalName(i)) ||
          !a.getQName(i).equals(b.getQName(i)) ||
          !a.getType(i).equals(b.getType(i)) ||
          !a.getValue(i).equals(b.getValue(i)) )
        return false;
    }
    return true;
  }

}