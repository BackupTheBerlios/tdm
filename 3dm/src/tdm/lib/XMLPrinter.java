// $Id: XMLPrinter.java,v 1.5 2003/01/09 13:01:43 ctl Exp $

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

/**
 * Class for outputting XML. The class has two modes: prettyprint and not. In prettyprint
 * mode it indents tags and contents according to level, most likely introducing
 * additional whitespace in content. In no-prettyprint no new whitespace is introduced in
 * the content, but the output is still quite readable (i.e. not a single line).
 * The algorithm is to not introduce any linebreaks if there is any content between tags (open as
 * well as close).
 */
public class XMLPrinter extends DefaultHandler {

  private static final int STATE_CHARS = 0;
  private static final int STATE_TAG = 1;
  private int state = -1;

  int indent = 0;
  private boolean prettyPrint = false;
  private static final String IND =
  "                                                                              ";
  private java.io.PrintWriter pw = null;

/*  public XMLPrinter( java.io.PrintWriter apw ) {
    pw=apw;
  }

  public XMLPrinter( java.io.PrintWriter apw, boolean aPrettyPrint ) {
    pw=apw;
    prettyPrint = aPrettyPrint;
  }
*/

  public XMLPrinter( java.io.OutputStream out ) {
    this(out,false);
  }

  public XMLPrinter(  java.io.OutputStream out, boolean aPrettyPrint ) {
    try {
      pw=new java.io.PrintWriter( new java.io.OutputStreamWriter( out, "utf-8" ));
    } catch (java.io.UnsupportedEncodingException x ) {
      System.err.println("Internal error: unknow encoding: utf-8");
      System.exit(-1);
    }
    prettyPrint = aPrettyPrint;
  }

   ////////////////////////////////////////////////////////////////////
   // Event handlers.
   ////////////////////////////////////////////////////////////////////


   public void startDocument ()
   {
      childcounter =HAS_CONTENT;
      pw.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      state = STATE_TAG;
   }


   public void endDocument ()
   {
       //System.out.println("End document");
     if(!prettyPrint)
       pw.println();
      pw.flush();
   }

    java.util.Stack csstack = new java.util.Stack();
   Integer childcounter = null;
   public void startElement (String uri, String name,
                             String qName, Attributes atts)

   {
    if( childcounter == null ) {
       printWithNL(">",prettyPrint );
      childcounter =HAS_CONTENT;
    }
    StringBuffer tagopen = new StringBuffer();
    if( state == STATE_TAG && !prettyPrint)
      tagopen.append("\n");
    tagopen.append('<');
    tagopen.append( qName );
  //    tagopen.append(' ');
    if( atts != null && atts.getLength() != 0 ) {
      for( int i = 0;i<atts.getLength();i++ ) {
        tagopen.append(' ');
        tagopen.append(atts.getQName(i));
        tagopen.append('=');
        tagopen.append('"');
        tagopen.append(atts.getValue(i));
        tagopen.append('"');
      }
    }
    csstack.push( childcounter );
    childcounter = null;
//      if( assumeNoChildren )
//        tagopen.append("/>");
//      else
//        tagopen.append('>');
    pw.print((prettyPrint ? IND.substring(0,indent) : "")  + tagopen.toString());
    indent ++;
    state = STATE_TAG;
   }


   public void endElement (String uri, String name, String qName)
   {
      indent--;
        if( childcounter == null )
          printWithNL(" />",prettyPrint);
        else {
          if( state == STATE_TAG && !prettyPrint)
            pw.println();
          printWithNL((prettyPrint ? IND.substring(0,indent) : "") + "</"+qName+">",
                      prettyPrint );
        }
      childcounter = (Integer) csstack.pop();
      state = STATE_TAG;
   }

   protected void printWithNL( String s, boolean appendNL ) {
     if( appendNL )
       pw.println(s);
      else
      pw.print(s);
   }

   final Integer HAS_CONTENT = new Integer(0);

   public void characters (char ch[], int startpos, int length)
   {
     state = STATE_CHARS;
      if(childcounter!=HAS_CONTENT)
         printWithNL(">",prettyPrint);
      childcounter = HAS_CONTENT;
      StringBuffer sb = new StringBuffer();
      for(int i=startpos;i<startpos+length;i++) {
        switch( ch[i] ) {
          case '<': sb.append("&lt;");
                break;
          case '>': sb.append(">");
                break;
          case '\'': sb.append("&apos;");
                break;
          case '&': sb.append("&amp;");
                break;
          case '"': sb.append("&quot;");
                break;
          default:
              sb.append(ch[i]);
        }
      }
      String chars = sb.toString();
//        String chars = new String( ch, startpos, length ).trim();
      if( chars.length() == 0 )
        return;/*
      int start=0,next=-1;
      do {
        next=chars.indexOf("\n",start);
        if( next==-1)
          pw.println(chars.substring(start));
        else {
          pw.println(chars.substring(start,next));
          start=next+1;
        }
      } while( next != -1 );*/
      printWithNL(chars,prettyPrint);
      //System.err.println("OUT:"+chars);
   }


}
