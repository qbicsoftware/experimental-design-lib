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

  private static final Pattern COLON = Pattern.compile(":");
  private static final Pattern SEMICOLON = Pattern.compile(";");
  private static final Pattern WHITESPACE = Pattern.compile(" ");

  public static void fixXMLProps(Map<String, Object> metadata) {
    XMLParser p = new XMLParser();
    LociParser lp = new LociParser();
    List<Property> factors = new ArrayList<Property>();

    if (metadata.get("Factors") != null) {
      for (Property f : (List<Property>) metadata.get("Factors")) {
        String factorLabel = factorNameForXML(f.getLabel());
        if (f.hasUnit())
          factors.add(new Property(factorLabel, f.getValue(), f.getUnit(), f.getType()));
        else
          factors.add(new Property(factorLabel, f.getValue(), f.getType()));
      }
      try {
        metadata.put("Q_PROPERTIES", p.toString(p.createXMLFromProperties(factors)));
      } catch (JAXBException e) {
        e.printStackTrace();
      }
      factors = new ArrayList<Property>();
    }
    metadata.remove("Factors");

    if (metadata.get("XML_FACTORS") != null) {
      String[] fStrings = SEMICOLON.split((String) metadata.get("XML_FACTORS"));
      for (String factor : fStrings) {
        if (factor.length() > 1) {
          String[] fields = COLON.split(factor);
          for (int i = 0; i < fields.length; i++)
            fields[i] = fields[i].trim();
          Matcher matcher = WHITESPACE.matcher(fields[0]);
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
   * checks if a string matches the xml schema for properties factor labels, changes it otherwise
   * 
   * @param label A String to be used as factor label in the properties xml
   * @return the label if it matches, a similar, matching label, otherwise
   */
  public static String factorNameForXML(String label) {
    Pattern p = Pattern.compile("([a-z]+_?[a-z]*)+([a-z]|[0-9]*)");
    if (p.matcher(label).matches())
      return label;

    label = label.trim();
    label = label.replace(" ", "_");
    char first = label.charAt(0);
    if (Character.isDigit(first))
      label = label.replaceFirst(Character.toString(first), "factor_" + first);
    return label;
  }
}
