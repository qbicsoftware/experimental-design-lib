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
import net.bytebuddy.description.modifier.SynchronizationState;

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
    
    if(metadata.get("Factors")!=null) {
      factors = (List<Property>) metadata.get("Factors");
      try {
        metadata.put("Q_PROPERTIES", p.toString(p.createXMLFromProperties(factors)));
      } catch (JAXBException e) {
        e.printStackTrace();
      }
      factors = new ArrayList<Property>();
    }
    
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
}
