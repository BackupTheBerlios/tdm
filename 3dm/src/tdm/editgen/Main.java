// $Id: Main.java,v 1.1 2002/10/30 15:12:26 ctl Exp $
package editgen;
import gnu.getopt.*;

public class Main {

  private static Editgen gen = new Editgen();

  public static void main(String[] args) {
    System.err.println("Editgen $Revision: 1.1 $" );
    // Get command line options
    int firstFileIx = parseOpts( args );
    if( args.length - firstFileIx >= 3 ) {
      String branches[] = new String[args.length-(firstFileIx+2)];
      System.arraycopy(args,firstFileIx+1,branches,0,branches.length);
      gen.editGen(args[firstFileIx],args[args.length-1],branches);
    } else {
      System.err.println("Usage:");
    }

  }

  private static int parseOpts( String args[] ) {
    LongOpt lopts[] = {
      new LongOpt("editlog",LongOpt.OPTIONAL_ARGUMENT,null,'e'),
      new LongOpt("edits",LongOpt.REQUIRED_ARGUMENT,null,'n'),
      new LongOpt("probability",LongOpt.NO_ARGUMENT,null,'p'),
      new LongOpt("operations",LongOpt.NO_ARGUMENT,null,'o'),
    };
    Getopt g = new Getopt("editgen", args, "n:p:o:e::", lopts);
    int c;
    String arg;
    while ((c = g.getopt()) != -1) {
      try {
        switch(c) {
          case 'e':
            gen.setEditLogPrefix(getStringArg(g,Editgen.EDIT_PREFIX));
            break;
          case 'p':
            gen.setProbability( getDoubleArg(g,Double.MAX_VALUE) );
            break;
          case 'n':
            gen.setEditCount(getIntArg(g,-1));
            break;
          case 'o':
            gen.setOperations(getStringArg(g,""));
            break;
        }
      } catch (IllegalArgumentException x ) {
        System.err.println("Option error: "+x.getMessage());
      }
    }
    return g.getOptind();
  }

  static String getStringArg( Getopt g, String defval ) {
    String arg = g.getOptarg();
    if( arg == null || "?".equals(arg) )
      return defval;
    else
      return arg;
  }

  static int getIntArg( Getopt g, int defval ) {
    String arg = g.getOptarg();
    if( arg == null || "?".equals(arg) )
      return defval;
    else {
      try {
        return Integer.parseInt(arg);
      } catch (Exception e) {
        return defval;
      }
    }
  }

  static double getDoubleArg( Getopt g, double defval ) {
    String arg = g.getOptarg();
    if( arg == null || "?".equals(arg) )
      return defval;
    else {
      try {
        return Double.parseDouble(arg);
      } catch (Exception e) {
        return defval;
      }
    }
  }


}