package life.qbic.expdesign;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import life.qbic.xml.loci.GeneLocus;
import life.qbic.xml.manager.LociParser;
import life.qbic.xml.manager.XMLParser;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;
import life.qbic.xml.properties.Unit;

/**
 * Helper functions used for sample creation
 * 
 * @author Andreas Friedrich
 * 
 */
public class ParserHelpers {

  public static void fixXMLProps(Map<String, Object> metadata) {
    final Pattern colon = Pattern.compile(":");
    final Pattern semicolon = Pattern.compile(";");
    final Pattern whitespace = Pattern.compile(" ");
    
    XMLParser p = new XMLParser();
    LociParser lp = new LociParser();
    List<Property> factors = new ArrayList<Property>();
    if (metadata.get("XML_FACTORS") != null) {
      String[] fStrings = semicolon.split((String) metadata.get("XML_FACTORS"));
      for (String factor : fStrings) {
        if (factor.length() > 1) {
          String[] fields = colon.split(factor);
          for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
          Matcher matcher = whitespace.matcher(fields[0]);
          String lab = matcher.replaceAll("");
          String val = fields[1];
          if (fields.length > 2)
            factors.add(new Property(lab, val, Unit.valueOf(fields[2]), PropertyType.Factor));
          else
            factors.add(new Property(lab, val, PropertyType.Factor));
        }
      }
      try {
        metadata.put("Q_PROPERTIES", p.toString(p.createXMLFromProperties(factors)));
      } catch (JAXBException e) {
        e.printStackTrace();
      }
    }
    metadata.remove("XML_FACTORS");

    List<GeneLocus> loci = new ArrayList<GeneLocus>();
    if (metadata.get("XML_LOCI") != null) {
      String[] lStrings = ((String) metadata.get("XML_LOCI")).split(";");
      for (String locus : lStrings) {
        if (locus.length() > 1) {
          String[] fields = locus.split(":");
          for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
          String lab = fields[0];
          String[] alleles = fields[1].split("/");
          loci.add(new GeneLocus(lab, new ArrayList<String>(Arrays.asList(alleles))));
        }
      }
      try {
        metadata.put("Q_LOCI", lp.toString(lp.createXMLFromLoci(loci)));
      } catch (JAXBException e) {
        e.printStackTrace();
      }
    }
    metadata.remove("XML_LOCI");
  }
  
  /**
   * Checks if a String can be parsed to an Integer
   * 
   * @param s a String
   * @return true, if the String can be parsed to an Integer successfully, false otherwise
   */
  public static boolean isInteger(String s) {
    try {
      Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  /**
   * Increments the value of an upper case char. When at "Z" restarts with "A".
   * 
   * @param c the char to be incremented
   * @return the next letter in the alphabet relative to the input char
   */
  public static char incrementUppercase(char c) {
    if (c == 'Z')
      return 'A';
    else {
      int charValue = c;
      return (char) (charValue + 1);
    }
  }

  /**
   * Creates a string with leading zeroes from a number
   * 
   * @param id number
   * @param length of the final string
   * @return the completed String with leading zeroes
   */
  public static String createCountString(int id, int length) {
    String res = Integer.toString(id);
    while (res.length() < length) {
      res = "0" + res;
    }
    return res;
  }

  /**
   * Checks which of two Strings can be parsed to a larger Integer and returns it.
   * 
   * @param a a String
   * @param b another String
   * @return the String that represents the larger number.
   */
  public static String max(String a, String b) {
    int a1 = Integer.parseInt(a);
    int b1 = Integer.parseInt(b);
    if (Math.max(a1, b1) == a1)
      return a;
    else
      return b;
  }

  /**
   * Maps an integer to a char representation. This can be used for computing the checksum.
   * 
   * @param i number to be mapped
   * @return char representing the input number
   */
  public static char mapToChar(int i) {
    i += 48;
    if (i > 57) {
      i += 7;
    }
    return (char) i;
  }

  public static float getPercentageStep(int max) {
    return new Float(1.0 / max);
  }

  /**
   * Computes a checksum digit for a given String. This checksum is weighted position-specific,
   * meaning it will also most likely fail a check if there is a typo of the String resulting in a
   * swapping of two numbers.
   * 
   * @param s String for which a checksum should be computed.
   * @return Character representing the checksum of the input String.
   */
  public static char checksum(String s) {
    int i = 1;
    int sum = 0;
    for (int idx = 0; idx <= s.length() - 1; idx++) {
      sum += (((int) s.charAt(idx))) * i;
      i += 1;
    }
    return mapToChar(sum % 34);
  }

  /**
   * Parses a whole String list to integers and returns them in another list.
   * 
   * @param strings List of Strings
   * @return list of integer representations of the input list
   */
  public static List<Integer> strArrToInt(List<String> strings) {
    List<Integer> res = new ArrayList<Integer>();
    for (String s : strings) {
      res.add(Integer.parseInt(s));
    }
    return res;
  }

  /**
   * Returns the 4 or 5 character project prefix used for samples in openBIS.
   * 
   * @param sample sample ID starting with a standard project prefix.
   * @return Project prefix of the sample
   */
  public static String getProjectPrefix(String sample) {
    if (isInteger("" + sample.charAt(4)))
      return sample.substring(0, 4);
    else
      return sample.substring(0, 5);
  }
}
