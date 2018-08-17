package life.qbic.expdesign;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.xml.loci.GeneLocus;
import life.qbic.xml.manager.LociParser;
import life.qbic.xml.manager.NewXMLParser;
import life.qbic.xml.manager.XMLParser;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;
import life.qbic.xml.properties.Unit;
import life.qbic.xml.study.Qexperiment;

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
  
  /**
   * collects experimental factors and properties from preliminary sample objects and creates an experiment-wide xml string. removes preliminary information from samples.
   * @param samples
   * @param omicsTypes
   * @return
   * @throws JAXBException
   */
  public static String samplesWithMetadataToDesignXML(List<ISampleBean> samples, List<String> omicsTypes) throws JAXBException {
    Map<String, Map<Pair<String,String>, List<String>>> expDesign =
        new HashMap<String, Map<Pair<String,String>, List<String>>>();
    Map<String, List<Property>> otherProps = new HashMap<String, List<Property>>();
    //TODO all types?
    Set<String> types = new HashSet<String>(Arrays.asList("Q_BIOLOGICAL_SAMPLE",
        "Q_BIOLOGICAL_ENTITY", "Q_TEST_SAMPLE", "Q_MHC_LIGAND_EXTRACT"));
    for (ISampleBean s : samples) {
      if (types.contains(s.getType())) {
        String code = s.getCode();
        List<Property> props = new ArrayList<Property>();
        Map<String, Object> metadata = s.getMetadata();
      
        // collect properties from metadata map
        // isa-tab format
        if (metadata.get("Factors") != null) {
          for (Property f : (List<Property>) metadata.get("Factors")) {
            String factorLabel = factorNameForXML(f.getLabel());
            if (f.hasUnit())
              props.add(new Property(factorLabel, f.getValue(), f.getUnit(), f.getType()));
            else
              props.add(new Property(factorLabel, f.getValue(), f.getType()));
          }
        }
        metadata.remove("Factors");
        
        // other parsers
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
                props.add(new Property(lab, val, Unit.valueOf(fields[2]), PropertyType.Factor));
              else
                props.add(new Property(lab, val, PropertyType.Factor));
            }
          }
        }
        metadata.remove("XML_FACTORS");
        
        for (Property p : props) {
          if (p.getType().equals(PropertyType.Factor)) {
            String lab = p.getLabel();
            String val = p.getValue();
            String unit = "";
            if (p.hasUnit())
              unit = p.getUnit().getValue();
            Pair<String,String> valunit = new ImmutablePair<String,String>(val, unit);
            if (expDesign.containsKey(lab)) {
              Map<Pair<String,String>, List<String>> levels = expDesign.get(lab);
              if (levels.containsKey(valunit)) {
                levels.get(valunit).add(code);
              } else {
                levels.put(valunit, new ArrayList<String>(Arrays.asList(code)));
              }
            } else {
              Map<Pair<String,String>, List<String>> newLevel = new HashMap<Pair<String,String>, List<String>>();
              newLevel.put(valunit, new ArrayList<String>(Arrays.asList(code)));
              expDesign.put(lab, newLevel);
            }

          } else {
            if (otherProps.containsKey(code)) {
              otherProps.get(code).add(p);
            } else {
              otherProps.put(code, new ArrayList<Property>(Arrays.asList(p)));
            }
          }
        }
      }
    }
    NewXMLParser p = new NewXMLParser();
    //TODO test for empty design
    JAXBElement<Qexperiment> res = p.createNewDesign(
        omicsTypes, expDesign, otherProps);
    String xml = p.toString(res);
    return xml;
  }

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
  
  public static List<Property> getPropsFromString(Map<String, Object> metadata) {
    List<Property> props = new ArrayList<Property>();
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
            props.add(new Property(lab, val, Unit.valueOf(fields[2]), PropertyType.Factor));
          else
            props.add(new Property(lab, val, PropertyType.Factor));
        }
      }
    }
    return props;
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
