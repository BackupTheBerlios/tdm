//$Id: TreeDM.java,v 1.19 2001/04/27 16:59:12 ctl Exp $
// PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE

/**
 * Title:        Tree Diff and Merge quick proto 1
 * Description:
 * Copyright:    Copyright (c) 2000
 * Company:      HUT
 * @author Tancred Lindholm
 * @version 1.0
 */

import java.io.FileReader;
import java.util.*;
import java.io.*;

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;

public class TreeDM {

  static final char PSEP = java.io.File.separatorChar;

  public TreeDM() {
  }

  public static void main(String[] args) throws Exception {
    // NOTE: When running mergecases, check that the parameters are set as follows:
    // COPY_TRESHOLD = 0 (otherwise cases with copies won't work) (normal value = 18)
    //
    (new TreeDM()).runDiff( args );
  }

  public void runHarness( String[] args ) {
   if( args.length < 1 ) {
      System.out.println("Usage: TreeDM case_dir");
      System.exit(0);
   }
   System.out.println("3DM test harness..Starting test");
   File topDir = new File(args[0]);
   File[] caseDirs = topDir.listFiles();
   if( caseDirs == null ) {
     System.out.println("No cases found in " + topDir.getName());
     System.exit(0);
   } else
     Arrays.sort(caseDirs);
   int casesRun=0;
   for(int ifile=0;ifile<caseDirs.length;ifile++) {
    if( caseDirs[ifile].isDirectory() && Character.isDigit( caseDirs[ifile].getName().charAt(1) ) ) {
      runCase(caseDirs[ifile] );
      casesRun++;
    }
   }
   if( casesRun == 0) {
     runCase( topDir );
   } else
     System.out.println(casesRun + " cases run.");
  }

  private NodeFactory baseNodeFactory =  new NodeFactory() {
            public Node makeNode(  XMLNode content ) {
              return new BaseNode( content  );
            }
        };

  private NodeFactory branchNodeFactory =  new NodeFactory() {
            public Node makeNode(  XMLNode content ) {
              return new BranchNode(  content  );
            }
        };


  private void runCase( File dir ) {
    final boolean SHOWDIFF = true;
    File base = new File( dir, "b.xml");
    File b1 = new File( dir, "2.xml");
    File b2 = new File( dir, "1.xml");
    File merged = new File( dir, "m.xml");
    File notes = new File( dir,"notes" );
    BranchNode docA=null, docB=null;
    BaseNode docBase=null;
    System.out.println("Running case " + dir.getName() );
    if( notes.exists() ) {
      try {
        BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream( notes ) ) );
        String s="";
        while((s=in.readLine())!=null)
          System.out.println("NOTES: "+s);
      } catch (Exception e ) {
          System.out.println("NOTES: READ EXCEPTED ");
      }
    }
    try {
      XMLParser p = new XMLParser();
//      System.out.println("Parsing " + base.getCanonicalPath() );
      docBase = (BaseNode) p.parse( base.getCanonicalPath(),baseNodeFactory);
//      System.out.println("Parsing " + base.getCanonicalPath() );
      docA = (BranchNode) p.parse( b1.getCanonicalPath(), branchNodeFactory);
//      System.out.println("Parsing " + base.getCanonicalPath() );
      docB = (BranchNode) p.parse( b2.getCanonicalPath(),branchNodeFactory);

    } catch ( Exception e ) {
      System.out.println("PARSE ERROR IN CASE, giving up." );
      //e.printStackTrace();
      return;
    }
    PrintStream sink = new PrintStream( new ByteArrayOutputStream() );
    PrintStream oldout = System.out;

    Merge merge1 = new Merge( new TriMatching( docA, docBase, docB ) );
    Merge merge2 = new Merge( new TriMatching( docB, docBase, docA ) );
    PrintWriter p1 = null, p2=null;
    try {
      p1 = new PrintWriter( new FileOutputStream( "m1.xml" ) );
      p2 = new PrintWriter( new FileOutputStream( "m2.xml" ) );
    } catch (java.io.FileNotFoundException e ) {
      System.out.println("ERROR: Can't write merged files");
      return;
    }
    try {
      merge1.merge( new MergePrinter(p1) );
      System.setOut(sink); // keep symmtry merge quiet..
      merge2.merge( new MergePrinter(p2) );
      System.setOut(oldout);
    } catch ( org.xml.sax.SAXException e ) {
      System.setOut(oldout);
      System.out.println("SAXException while merging.. and it was yoyr lousy content handler that threw it");
    }
    p1.close();p2.close();
    System.out.print("Checking merges... " );
    BaseNode facit=null,mr1=null,mr2=null;
    try {
      XMLParser p = new XMLParser();
      facit = (BaseNode) p.parse( merged.getCanonicalPath(),baseNodeFactory);
      mr1 = (BaseNode) p.parse( "m1.xml",baseNodeFactory );
      mr2 = (BaseNode) p.parse( "m2.xml",baseNodeFactory );
    } catch ( Exception e ) {
      System.out.println("PARSE ERROR IN CASE when parsing merged files. Giving up." +e.getMessage() );
      //e.printStackTrace();
      return;
    }
    boolean symmetry = treesIdentical(mr1,mr2,false),
            likefacit = treesIdentical(mr1,facit,SHOWDIFF);
    if( !symmetry )
      System.out.print("SYMMETRY failed ");
    if( ((XMLElementNode) facit.getChild(0).getContent()).getQName().equalsIgnoreCase("conflict") ) {
      System.out.println("Facit says this was a CONFLICT" );
      return;
    }
    if( !likefacit )
      System.out.print("FACIT failed " + ( symmetry || (!symmetry && !treesIdentical(facit,mr2,false)) ?
        "(on both)" : "(on one only)" ) );
    if( likefacit && symmetry )
      System.out.println("Ok ");
    else {
      PrintWriter pw = new PrintWriter( System.out );
      pw.println( !symmetry ? "\nMerge 1: " : "\nMerge" );
      try {
        System.setOut(sink);
        merge1.merge( new MergePrinter(pw) );
        if( !symmetry ) {
          pw.println( "Merge 2:"  );
          merge2.merge( new MergePrinter(pw) );
        }
        System.setOut( oldout );
      } catch ( Exception e ) {
        System.setOut(oldout);
        System.out.println("PARSE ERROR..." );
      }
      pw.flush();
    }
    return;
  }

  private boolean treesIdentical( Node a, Node b, boolean searchAll ) {
    boolean result = true;
    if( !a.getContent().contentEquals( b.getContent()) ) {
      if(searchAll) {
        System.out.println("IDENTITY Failed");
        System.out.println(a.getContent().toString());
        System.out.println(b.getContent().toString());
      }
     result = false;
    }
    if( a.getChildCount() != b.getChildCount() ) {
      if(searchAll){
        PrintWriter pw = new PrintWriter( System.out );

       pw.println("IDENTITY failed: childcount failed below :");
       pw.println("Merged Tree------------------------------------------");
       printTree(a, new MergePrinter(pw));
       pw.println("Facit Tree-------------------------------------------");
       printTree(b, new MergePrinter(pw));
       pw.println("-----------------------------------------------------");
        pw.flush();
      }
       result = false;
    } else if( result || searchAll )
      for( int i=0;i<a.getChildCount();i++)
        if( !treesIdentical( a.getChildAsNode(i), b.getChildAsNode(i), searchAll ) )
          result = false;
    return result;
  }

  // Run Best Matcher
  public void runBM( String[] args ) {
   BranchNode docA=null;
   BaseNode docBase=null;
   final String OTHER = "2";
    if( args.length < 2 ) {
      System.out.println("Usage: TreeDM base.xml deriv.xml");
      System.exit(0);
   }
   try {
      XMLParser p = new XMLParser();
      System.out.println("Parsing " + args [0]);
      docBase = (BaseNode) p.parse(args[0] + PSEP + "b.xml",baseNodeFactory);
      System.out.println("Parsing " + OTHER + ".xml");
      docA =  (BranchNode) p.parse(args[0] + PSEP+ OTHER+".xml",branchNodeFactory);
      System.out.println("OK.");
   } catch ( Exception e ) {
    e.printStackTrace();
    System.exit(0);
   }
//   System.exit(0);
  System.out.println("Building matching...");
   Matching m1 = new Matching( docBase, docA );
  System.out.println("done.");
   java.io.File mf = new java.io.File( args[0] + PSEP+ "match"+OTHER );
   if( mf.exists() ) {
    System.out.println("Additional matchings from match"+OTHER);
//    m1.matchFromFile(mf);
   }

   //   System.out.println("Showing area tree..." );
//   java.awt.Frame treeView = new TreeView(m1.atRoot , null, m1, null );
   System.out.println("Showing mapping..." );
   java.awt.Frame treeView = new TreeView((BaseNode) docBase , (BranchNode) docA  );

   treeView.setVisible(true);
  }

  // Run diff
  public void runDiff( String[] args ) {
   BranchNode docA=null,docDiff=null;
   BaseNode docBase=null;
   final String OTHER = "2";
    if( args.length < 2 ) {
      System.out.println("Usage: TreeDM base.xml deriv.xml");
      System.exit(0);
   }
   try {
      XMLParser p = new XMLParser();
      System.out.println("Parsing " + args [0]);
      docBase = (BaseNode) p.parse(args[0] + PSEP + "b.xml",baseNodeFactory);
      System.out.println("Parsing " + OTHER + ".xml");
      docA =  (BranchNode) p.parse(args[0] + PSEP+ OTHER+".xml",branchNodeFactory);
      System.out.println("OK.");
   } catch ( Exception e ) {
    e.printStackTrace();
    System.exit(0);
   }
//   System.exit(0);
  System.out.println("Building matching...");
   Matching m1 = new DiffMatching( docBase, docA );
  System.out.println("done.");

  System.out.println("Diffing...");
  Diff diff = new Diff( m1 );

  PrintWriter pw = null;
  try {
     pw = new PrintWriter( new FileOutputStream( "diff.xml" ) );
  } catch (java.io.FileNotFoundException e ) {
    System.out.println("Error opening output diff file...");
    System.exit(0);
  }

  MergePrinter mp = new MergePrinter(pw) ;
  mp.startDocument();
  try {
    diff.diff( mp );
   } catch ( SAXException e ) {
    e.printStackTrace();
    System.exit(0);
   }

  mp.endDocument();
  pw.flush();
// Re-read diff & apply
   try {
      XMLParser p = new XMLParser();
      System.out.println("Parsing diff " + args [0]);
      docDiff = (BranchNode) p.parse("diff.xml",branchNodeFactory);
      System.out.println("OK.");
   } catch ( Exception e ) {
    e.printStackTrace();
    System.exit(0);
   }
   Patch patch = new Patch();
   BranchNode patchDoc = null;
    try {
      patchDoc = patch.patch(docBase,docDiff);
    } catch (ParseException e ) {
      System.out.println("DIFF PARSE FAILURE");
    System.exit(0);
    }
  if( treesIdentical(docA,patchDoc,true) )
    System.out.println("DIFF/PATCH worked!");
  }




  class MergePrinter extends DefaultHandler {


    int indent = 0;
    private static final String IND = "                                                      ";
    private java.io.PrintWriter pw = null;

    MergePrinter( java.io.PrintWriter apw ) {
      pw=apw;
    }
     ////////////////////////////////////////////////////////////////////
     // Event handlers.
     ////////////////////////////////////////////////////////////////////


     public void startDocument ()
     {
        childcounter =HAS_CONTENT;
     }


     public void endDocument ()
     {
         //System.out.println("End document");
     }

      java.util.Stack csstack = new java.util.Stack();
     Integer childcounter = null;
     public void startElement (String uri, String name,
                               String qName, Attributes atts)

     {
      if( childcounter == null ) {
         pw.println(">");
        childcounter =HAS_CONTENT;
      }
      StringBuffer tagopen = new StringBuffer();
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
      pw.print(IND.substring(0,indent)  + tagopen.toString());
      indent ++;
     }


     public void endElement (String uri, String name, String qName)
     {
        indent--;
          if( childcounter == null )
            pw.println(" />");
          else
            pw.println(IND.substring(0,indent)+ "</"+qName+">");
        childcounter = (Integer) csstack.pop();
     }

     final Integer HAS_CONTENT = new Integer(0);

     public void characters (char ch[], int startpos, int length)
     {
        if(childcounter!=HAS_CONTENT)
           pw.println(">");
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
        pw.println(chars);
     }


  }

  void printTree( Node n, org.xml.sax.ContentHandler h ) {
    XMLNode c = n.getContent();
    try {
      if( c instanceof XMLTextNode ) {
        XMLTextNode ct = (XMLTextNode) c;
        h.characters(ct.getText(),0,ct.getText().length);
      } else {
        XMLElementNode ce = (XMLElementNode) c;
        h.startElement(ce.getNamespaceURI(),ce.getLocalName(),ce.getQName(),ce.getAttributes());
        for( int i=0;i<n.getChildCount();i++)
          printTree(n.getChildAsNode(i),h);
        h.endElement(ce.getNamespaceURI(),ce.getLocalName(),ce.getQName());
      }
    } catch (org.xml.sax.SAXException x ) {
      System.err.println("FATAL::::::::::::::::::::SAXEX");
    }
  }

}