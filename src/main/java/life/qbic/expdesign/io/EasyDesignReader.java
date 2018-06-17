package life.qbic.expdesign.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleSummary;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.ParserHelpers;
import life.qbic.expdesign.SamplePreparator;
import life.qbic.expdesign.model.ExperimentalDesignType;
import life.qbic.xml.manager.XMLParser;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;
import life.qbic.xml.properties.Unit;

public class EasyDesignReader implements IExperimentalDesignReader {

  private static final Logger logger = LogManager.getLogger(EasyDesignReader.class);
  private List<String> mandatory;
  private boolean analytesIncluded;
  private Map<String, Map<String, String>> headersToTypeCodePerSampletype;

  private String error;
  private Map<String, List<Map<String, Object>>> experimentInfos;
  private Set<String> speciesSet;
  private Set<String> tissueSet;
  private Set<String> analyteSet;
  private List<String> tsvByRows;

  private Map<String, Set<SampleSummary>> nodesForFactorPerLabel;

  public EasyDesignReader() {
    // this.designType = type;
    Set<String> mandatory =
        new HashSet<String>(Arrays.asList("Organism", "Organism ID", "Tissue", "Extract ID"));
    // "Analyte", "Analyte ID"));
    this.mandatory = new ArrayList<String>(mandatory);
    Map<String, String> sourceMetadata = new HashMap<String, String>();
    sourceMetadata.put("Organism", "Q_NCBI_ORGANISM");
    sourceMetadata.put("Organism ID", "Q_EXTERNALDB_ID");
    sourceMetadata.put("Source Comment", "Q_ADDITIONAL_INFO");
    Map<String, String> extractMetadata = new HashMap<String, String>();
    extractMetadata.put("Tissue", "Q_PRIMARY_TISSUE");
    extractMetadata.put("Extract ID", "Q_EXTERNALDB_ID");
    extractMetadata.put("Tissue Comment", "Q_ADDITIONAL_INFO");
    extractMetadata.put("Detailed Tissue", "Q_TISSUE_DETAILED");
    Map<String, String> prepMetadata = new HashMap<String, String>();
    prepMetadata.put("Analyte", "Q_SAMPLE_TYPE");
    prepMetadata.put("Analyte ID", "Q_EXTERNALDB_ID");
    prepMetadata.put("Preparation Comment", "Q_ADDITIONAL_INFO");
    headersToTypeCodePerSampletype = new HashMap<String, Map<String, String>>();
    headersToTypeCodePerSampletype.put("Q_BIOLOGICAL_ENTITY", sourceMetadata);
    headersToTypeCodePerSampletype.put("Q_BIOLOGICAL_SAMPLE", extractMetadata);
    headersToTypeCodePerSampletype.put("Q_TEST_SAMPLE", prepMetadata);
  }

  public Map<String, List<Map<String, Object>>> getExperimentInfos() {
    return experimentInfos;
  }

  public static void main(String[] args) throws JAXBException {
    try {
      SamplePreparator p = new SamplePreparator();
      p.processTSV(new File("/Users/frieda/Downloads/BiglistTest.tsv"),
          ExperimentalDesignType.Standard, false);
      System.out.println(p.getSummary());
      // System.out.println(p.getProcessed());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static final String UTF8_BOM = "\uFEFF";

  private static String removeUTF8BOM(String s) {
    if (s.startsWith(UTF8_BOM)) {
      s = s.substring(1);
    }
    return s;
  }

  public int countEntities(File file) throws IOException {
    Set<String> ids = new HashSet<String>();


    nodesForFactorPerLabel = new HashMap<String, Set<SampleSummary>>();

    tsvByRows = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new FileReader(file));
    ArrayList<String[]> data = new ArrayList<String[]>();
    String next;
    int i = 0;
    // isPilot = false;
    while ((next = reader.readLine()) != null) {
      i++;
      next = removeUTF8BOM(next);
      tsvByRows.add(next);
      String[] nextLine = next.split("\t", -1);// this is needed for trailing tabs
      if (data.isEmpty() || nextLine.length == data.get(0).length) {
        data.add(nextLine);
      } else {
        reader.close();
        return -1;
      }
    }
    reader.close();

    String[] header = data.get(0);
    data.remove(0);
    // find out where the mandatory and other metadata data is
    Map<String, Integer> headerMapping = new HashMap<String, Integer>();
    List<Integer> meta = new ArrayList<Integer>();

    ArrayList<String> found = new ArrayList<String>(Arrays.asList(header));
    for (String col : mandatory) {
      if (!found.contains(col)) {
        return -1;
      }
    }
    if (found.contains("Analyte") && found.contains("Analyte ID")) {
      headerMapping.put("Analyte", found.indexOf("Analyte"));
      headerMapping.put("Analyte ID", found.indexOf("Analyte ID"));
      analytesIncluded = true;
    } else if (!found.contains("Analyte") && !found.contains("Analyte ID"))
      analytesIncluded = false;
    else {
      return -1;
    }
    for (i = 0; i < header.length; i++) {
      int position = mandatory.indexOf(header[i]);
      if (position > -1) {
        headerMapping.put(header[i], i);
        meta.add(i);
      }
    }
    for (String[] row : data) {
      String sourceID = row[headerMapping.get("Organism ID")];
      String extractID = row[headerMapping.get("Extract ID")];
      ids.add(sourceID);
      ids.add(extractID);
      String analyteID = "";
      if (analytesIncluded) {
        analyteID = row[headerMapping.get("Analyte ID")];
        ids.add(analyteID);
      }
    }

    return ids.size();
  }

  /**
   * Reads in a TSV file containing samples that should be registered. Returns a List of
   * TSVSampleBeans containing all the necessary information to register each sample with its meta
   * information to openBIS, given that the types and parents exist.
   * 
   * @param file
   * @param parseGraph
   * @return ArrayList of TSVSampleBeans
   * @throws IOException
   * @throws JAXBException
   */
  public List<ISampleBean> readSamples(File file, boolean parseGraph)
      throws IOException, JAXBException {

    nodesForFactorPerLabel = new HashMap<String, Set<SampleSummary>>();

    tsvByRows = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new FileReader(file));
    ArrayList<String[]> data = new ArrayList<String[]>();
    String next;
    int i = 0;
    // isPilot = false;
    while ((next = reader.readLine()) != null) {
      i++;
      next = removeUTF8BOM(next);
      tsvByRows.add(next);
      String[] nextLine = next.split("\t", -1);// this is needed for trailing tabs
      if (data.isEmpty() || nextLine.length == data.get(0).length) {
        data.add(nextLine);
      } else {
        error = "Wrong number of columns in row " + i;
        reader.close();
        return null;
      }
    }
    reader.close();

    String[] header = data.get(0);
    data.remove(0);
    // find out where the mandatory and other metadata data is
    Map<String, Integer> headerMapping = new HashMap<String, Integer>();
    List<Integer> meta = new ArrayList<Integer>();
    Set<Integer> factors = new HashSet<Integer>();
    List<Integer> properties = new ArrayList<Integer>();
    List<Integer> loci = new ArrayList<Integer>();
    int numOfLevels = 2;

    ArrayList<String> found = new ArrayList<String>(Arrays.asList(header));
    for (String col : mandatory) {
      if (!found.contains(col)) {
        error = "Mandatory column " + col + " not found.";
        return null;
      }
    }
    if (found.contains("Analyte") && found.contains("Analyte ID")) {
      headerMapping.put("Analyte", found.indexOf("Analyte"));
      headerMapping.put("Analyte ID", found.indexOf("Analyte ID"));
      analytesIncluded = true;
    } else if (!found.contains("Analyte") && !found.contains("Analyte ID"))
      analytesIncluded = false;
    else {
      error =
          "One of the columns Analyte and Analyte ID was not found. Both are needed to add Analyte samples.";
      return null;
    }
    for (i = 0; i < header.length; i++) {
      String lowerCase = header[i].toLowerCase();
      int position = mandatory.indexOf(header[i]);
      if (position > -1) {
        headerMapping.put(header[i], i);
        meta.add(i);
      } else if (lowerCase.contains("condition: ") || lowerCase.contains("property: ")) {
        // example: Property: mass [kg]
        String attribute = removeFactorKeywords(header[i]);
        for (char c : attribute.toCharArray()) {
          if (Character.isUpperCase(c)) {
            error = "Attributes are not allowed to contain upper case characters: " + attribute;
            return null;
          }
        }
        if (attribute.contains("[") && attribute.contains("]")) {
          String unit = parseUnit(attribute);
          attribute = attribute.replace(" [" + unit + "]", "");
          try {
            Unit.valueOf(unit);
          } catch (IllegalArgumentException e) {
            error = "Unknown unit " + unit + " in attribute " + attribute;
            return null;
          }
        }
        if (attribute.contains(" ")) {
          error = "Attributes are not allowed to contain spaces: " + attribute;
          return null;
        }
        char first = attribute.charAt(0);
        if (first >= '0' && first <= '9') {
          error = "Attributes are not allowed to start with a number: " + attribute;
          return null;
        }
        if (lowerCase.contains("property: "))
          properties.add(i);
        else
          factors.add(i);
      } else if (lowerCase.contains("locus:")) {
        loci.add(i);
      } else if (header[i].contains("DIGESTION_KEYWORD")// TODO?
          || header[i].contains("FRACTIONATION_KEYWORD")) {
        numOfLevels++;
      } else {
        meta.add(i);
      }
    }
    // sort attributes
    Set<Integer> entityFactors = new HashSet<Integer>();
    Set<Integer> extractFactors = new HashSet<Integer>();
    for (int col : factors) {
      Map<String, String> idToVal = new HashMap<String, String>();
      boolean ent = true;
      boolean extr = true;
      for (String[] row : data) {
        String val = row[col];
        String sourceID = row[headerMapping.get("Organism ID")];
        String extractID = row[headerMapping.get("Extract ID")];
        // if different for same entities: not an entity attribute
        if (idToVal.containsKey(sourceID)) {
          if (!idToVal.get(sourceID).equals(val))
            ent = false;
        }
        if (idToVal.containsKey(extractID)) {
          if (!idToVal.get(extractID).equals(val))
            extr = false;
        }
        idToVal.put(sourceID, val);
        idToVal.put(extractID, val);
      }
      if (ent)
        entityFactors.add(col);
      if (extr)
        extractFactors.add(col);
    }
    if (parseGraph) {
      for (int factorCol : factors) {
        String label = parseXMLPartLabel(header[factorCol]);
        nodesForFactorPerLabel.put(label, new LinkedHashSet<SampleSummary>());
      }
      nodesForFactorPerLabel.put("None", new LinkedHashSet<SampleSummary>());
    }

    // create samples
    List<ISampleBean> beans = new ArrayList<ISampleBean>();
    List<List<ISampleBean>> order = new ArrayList<List<ISampleBean>>();
    Map<String, TSVSampleBean> sourceIDToSample = new HashMap<String, TSVSampleBean>();
    Map<String, TSVSampleBean> extractIDToSample = new HashMap<String, TSVSampleBean>();
    Map<String, TSVSampleBean> analyteIDToSample = new HashMap<String, TSVSampleBean>();
    List<TSVSampleBean> roots = new ArrayList<TSVSampleBean>();
    Set<String> speciesSet = new HashSet<String>();
    Set<String> tissueSet = new HashSet<String>();
    Set<String> analyteSet = new HashSet<String>();
    int rowID = 0;
    // int sampleID = 0;
    for (String[] row : data) {
      rowID++;
      // boolean special = false;
      // if (!special) {
      for (String col : mandatory) {
        if (row[headerMapping.get(col)].isEmpty()) {
          error = col + " is a mandatory field, but it is not set for row " + rowID + "!";
          return null;
        }
      }
      String sourceID = row[headerMapping.get("Organism ID")];
      String extractID = row[headerMapping.get("Extract ID")];
      String species = row[headerMapping.get("Organism")];
      String tissue = row[headerMapping.get("Tissue")];

      String analyteID = "";
      String analyte = "";
      if (analytesIncluded) {
        numOfLevels++;
        analyteID = row[headerMapping.get("Analyte ID")];
        analyte = row[headerMapping.get("Analyte")];
        if (!analyte.isEmpty())
          analyteSet.add(analyte);
      }

      speciesSet.add(species);
      tissueSet.add(tissue);

      while (order.size() < numOfLevels) {
        order.add(new ArrayList<ISampleBean>());
      }
      // if analyte is known, nothing needs to be done
      if (!analyteIDToSample.containsKey(analyteID)) {
        if (!analyteID.isEmpty()) {// some analytes can be added, while other cells can be empty
          // sampleID++;
          TSVSampleBean firstASample = new TSVSampleBean(analyteID, "Q_TEST_SAMPLE", analyteID,
              fillMetadata(header, row, meta, factors, loci, "Q_TEST_SAMPLE"));
          firstASample.addProperty("Q_SAMPLE_TYPE", analyte);
          order.get(2).add(firstASample);
          firstASample.addParentID(extractID);
          analyteIDToSample.put(analyteID, firstASample);
        }

        // these will be filled below, if they don't exist
        TSVSampleBean eSample = extractIDToSample.get(extractID);
        TSVSampleBean sSample = sourceIDToSample.get(sourceID);
        if (!extractIDToSample.containsKey(extractID)) {
          // sampleID++;
          eSample = new TSVSampleBean(extractID, "Q_BIOLOGICAL_SAMPLE", extractID,
              fillMetadata(header, row, meta, extractFactors, loci, "Q_BIOLOGICAL_SAMPLE"));
          eSample.addProperty("Q_PRIMARY_TISSUE", tissue);
          order.get(1).add(eSample);
          extractIDToSample.put(extractID, eSample);
          eSample.addParentID(sourceID);

        }
        if (!sourceIDToSample.containsKey(sourceID)) {
          // sampleID++;
          sSample = new TSVSampleBean(sourceID, "Q_BIOLOGICAL_ENTITY", sourceID,
              fillMetadata(header, row, meta, entityFactors, loci, "Q_BIOLOGICAL_ENTITY"));
          sSample.addProperty("Q_NCBI_ORGANISM", species);
          roots.add(sSample);
          order.get(0).add(sSample);
          sourceIDToSample.put(sourceID, sSample);
        }
        if (parseGraph) {
          createGraphSummariesForRow(
              new ArrayList<TSVSampleBean>(Arrays.asList(sourceIDToSample.get(sourceID),
                  extractIDToSample.get(extractID), analyteIDToSample.get(analyteID))),
              new Integer(rowID));
        }
      }
      // }
    }
    for (List<ISampleBean> level : order)
      beans.addAll(level);
    boolean unique =
        checkUniqueNamesBetweenSets(speciesSet, tissueSet, analyteSet) && checkUniqueIDsBetweenSets(
            sourceIDToSample.keySet(), extractIDToSample.keySet(), analyteIDToSample.keySet());
    if (!unique)
      return null;
    this.speciesSet = speciesSet;
    this.tissueSet = tissueSet;
    this.analyteSet = analyteSet;
    return beans;
  }

  private String removeFactorKeywords(String string) {
    return string.replace("condition: ", "").replace("property: ", "").replace("Condition: ", "")
        .replace("Property: ", "");
  }

  private void createGraphSummariesForRow(List<TSVSampleBean> levels, int nodeID)
      throws JAXBException {
    nodeID *= 3;
    // create summary for this each node based on each experimental factor as well as "none"
    for (String label : nodesForFactorPerLabel.keySet()) {
      SampleSummary currentSummary = null;
      // source - extract - analyte (at least the first must exist) - create summaries for them all
      for (int level = 0; level < levels.size(); level++) {
        TSVSampleBean s = levels.get(level);
        // find out if this sample has children or not
        int next = level + 1;
        boolean leaf = levels.size() == next || levels.get(next) == null;
        // sample on this level does exist
        if (s != null) {
          nodeID++;
          Set<SampleSummary> parentSummaries = new LinkedHashSet<SampleSummary>();
          if (currentSummary != null)
            parentSummaries.add(currentSummary);
          currentSummary = createNodeSummary(s, parentSummaries, label, nodeID, leaf);
          // check for hashcode and add current sample s if node exists
          boolean exists = false;
          for (SampleSummary oldNode : nodesForFactorPerLabel.get(label)) {
            if (oldNode.equals(currentSummary)) {
              for (String code : currentSummary.getCodes()) {
                exists = oldNode.getCodes().contains(code);
              }
              if (!exists)
                oldNode.addSample(s);
              currentSummary = oldNode;
            }
          }
          // adds node if not already contained in set
          Set<SampleSummary> theseNodes = nodesForFactorPerLabel.get(label);
          theseNodes.add(currentSummary);
          nodesForFactorPerLabel.put(label, theseNodes);
          // add this id to parents' child ids
          for (SampleSummary parentSummary : parentSummaries) {
            parentSummary.addChildID(currentSummary.getId());
          }
        }
      }
    }
  }

  @Override
  public Map<String, List<SampleSummary>> getSampleGraphNodes() {
    Map<String, List<SampleSummary>> factorsToSamples = new HashMap<String, List<SampleSummary>>();
    for (String label : nodesForFactorPerLabel.keySet()) {
      Set<SampleSummary> nodes = nodesForFactorPerLabel.get(label);
      factorsToSamples.put(label, new ArrayList<SampleSummary>(nodes));
    }
    return factorsToSamples;
  }

  public Set<String> getSpeciesSet() {
    return speciesSet;
  }

  public Set<String> getTissueSet() {
    return tissueSet;
  }

  public Set<String> getAnalyteSet() {
    return analyteSet;
  }

  private Property getFactorOfSampleOrNull(String xml, String factorLabel) throws JAXBException {
    XMLParser xmlParser = new XMLParser();
    List<Property> factors = new ArrayList<Property>();
    if (xml != null)
      factors = xmlParser.getAllProperties(xmlParser.parseXMLString(xml));
    for (Property f : factors) {
      if (f.getLabel().equals(factorLabel))
        return f;
    }
    return null;
  }

  // new "sample to bucket" function, creates new summaries from sample metadata in reference to
  // parent summaries and experimental factor
  private SampleSummary createNodeSummary(TSVSampleBean s, Set<SampleSummary> parents, String label,
      int currentID, boolean leaf) throws JAXBException {
    // name: should be the visible discriminating factor between nodes
    // 1. contains the source, if the source is not the selected factor (e.g. tissues)
    // 2. contains the selected factor's value, except
    // a) if parent sample has the same factor value
    // b) if it has no factor
    // factor: the current selected factor object. If none exists, parents' sources are used.

    // the name alone is not enough to discriminate between different nodes! (e.g. different parent
    // nodes, same child node name)
    String type = s.getType();
    String source = "unknown";
    Map<String, Object> props = s.getMetadata();
    ParserHelpers.fixXMLProps(props);

    Property factor = getFactorOfSampleOrNull((String) props.get("Q_PROPERTIES"), label);
    boolean newFactor = true;
    Set<String> parentSources = new HashSet<String>();
    Set<Integer> parentIDs = new HashSet<Integer>();
    for (SampleSummary parentSum : parents) {
      parentIDs.add(parentSum.getId());
      String factorVal = parentSum.getFactorValue();
      if (factorVal != null && !factorVal.isEmpty()) {
        newFactor = false;
      }
      parentSources.add(parentSum.getSource());
    }
    if (factor == null) {
      factor = new Property("parents", String.join("+", parentSources), PropertyType.Factor);// TODO
                                                                                             // makes
                                                                                             // sense?
      newFactor = false;
    }
    String value = "";
    if (newFactor)
      value = factor.getValue();
    switch (type) {
      case "Q_BIOLOGICAL_ENTITY":
        source = (String) props.get("Q_NCBI_ORGANISM");
        value = source + " " + value;
        break;
      case "Q_BIOLOGICAL_SAMPLE":
        source = (String) props.get("Q_PRIMARY_TISSUE");
        if (!newFactor || source.equals(value)) {
          value = source;
        } else {
          value = source + " " + value;
        }
        break;
      case "Q_TEST_SAMPLE":
        source = (String) props.get("Q_SAMPLE_TYPE");
        value = source + " " + value;
        break;
      // case "Q_MHC_LIGAND_EXTRACT":
      // source = (String) props.get("Q_MHC_CLASS");
      // value = source;
      // break;
    }
    return new SampleSummary(currentID, parentIDs, new ArrayList<ISampleBean>(Arrays.asList(s)),
        factor.getValue(), tryShortenName(value, s).trim(), type, leaf);
  }

  private String tryShortenName(String key, TSVSampleBean s) {
    switch (s.getType()) {
      case "Q_BIOLOGICAL_ENTITY":
        return key;
      case "Q_BIOLOGICAL_SAMPLE":
        return key;
      case "Q_TEST_SAMPLE":
        String type = (String) s.getMetadata().get("Q_SAMPLE_TYPE");
        return key.replace(type, "") + " " + type;// shortenInfo(type);
      // case "Q_MHC_LIGAND_EXTRACT":
      // return s.getProperties().get("Q_MHC_CLASS").replace("_", " ").replace("CLASS", "Class");
    }
    return key;
  }

  // private String shortenInfo(String info) {
  // switch (info) {
  // case "CARBOHYDRATES":
  // return "Carbohydrates";
  // case "SMALLMOLECULES":
  // return "Smallmolecules";
  // case "DNA":
  // return "DNA";
  // case "RNA":
  // return "RNA";
  // default:
  // return WordUtils.capitalizeFully(info.replace("_", " "));
  // }
  // }

  private boolean checkUniqueIDsBetweenSets(Set<String> speciesSet, Set<String> tissueSet,
      Set<String> analyteSet) {
    Set<String> intersection1 = new HashSet<String>(speciesSet);
    intersection1.retainAll(tissueSet);
    Set<String> intersection2 = new HashSet<String>(speciesSet);
    intersection2.retainAll(analyteSet);
    Set<String> intersection3 = new HashSet<String>(tissueSet);
    intersection3.retainAll(analyteSet);
    if (!intersection1.isEmpty()) {
      error = "Identifier " + intersection1.iterator().next()
          + " was found as organism ID and tissue ID. It can't be both!";
      return false;
    }
    if (!intersection2.isEmpty()) {
      error = "Identifier " + intersection2.iterator().next()
          + " was found as organism ID and analyte ID. It can't be both!";
      return false;
    }
    if (!intersection3.isEmpty()) {
      error = "Identifier " + intersection3.iterator().next()
          + " was found as tissue ID and analyte ID. It can't be both!";
      return false;
    }
    return true;
  }

  private boolean checkUniqueNamesBetweenSets(Set<String> speciesSet, Set<String> tissueSet,
      Set<String> analyteSet) {
    Set<String> intersection1 = new HashSet<String>(speciesSet);
    intersection1.retainAll(tissueSet);
    Set<String> intersection2 = new HashSet<String>(speciesSet);
    intersection2.retainAll(analyteSet);
    Set<String> intersection3 = new HashSet<String>(tissueSet);
    intersection3.retainAll(analyteSet);
    if (!intersection1.isEmpty()) {
      error = "Entry " + intersection1.iterator().next()
          + " was found as organism and tissue. It can't be both!";
      return false;
    }
    if (!intersection2.isEmpty()) {
      error = "Entry " + intersection2.iterator().next()
          + " was found as organism and analyte. It can't be both!";
      return false;
    }
    if (!intersection3.isEmpty()) {
      error = "Entry " + intersection3.iterator().next()
          + " was found as tissue and analyte. It can't be both!";
      return false;
    }
    return true;
  }

  private String parseUnit(String label) {
    if (!label.contains("]") && !label.contains("["))
      return null;
    label = label.substring(label.indexOf("[") + 1);
    label = label.substring(0, label.indexOf("]"));
    return label;
  }

  // private String trySplitMetadata(String line, String keyword) {
  // try {
  // return line.split(keyword)[1].trim();
  // } catch (ArrayIndexOutOfBoundsException e) {
  // return "";
  // }
  // }
  //
  // private String replaceSpecials(String s) {
  // return s.replace("%%%", "#").replace(">>>", "=");
  // }

  private HashMap<String, Object> fillMetadata(String[] header, String[] data, List<Integer> meta,
      Set<Integer> factors, List<Integer> loci, String sampleType) {
    Map<String, String> headersToOpenbisCode = headersToTypeCodePerSampletype.get(sampleType);
    HashMap<String, Object> res = new HashMap<String, Object>();
    for (int i : meta) {
      String label = header[i];
      if (!data[i].isEmpty() && headersToOpenbisCode.containsKey(label))
        res.put(headersToOpenbisCode.get(label), data[i]);
    }
    if (factors.size() > 0) {
      String fRes = "";
      for (int i : factors) {
        if (!data[i].isEmpty()) {
          String values = unitCheck(data[i]);
          fRes += parseXMLPartLabel(header[i]) + ": " + values + ";";
        }
      }
      // remove trailing ";"
      fRes = fRes.substring(0, Math.max(1, fRes.length()) - 1);
      res.put("XML_FACTORS", fRes);
    }
    if (loci.size() > 0) {
      String lRes = "";
      for (int i : loci) {
        if (!data[i].isEmpty()) {
          lRes += parseXMLPartLabel(header[i]) + ": " + data[i] + ";";
        }
      }
      // remove trailing ";"
      lRes = lRes.substring(0, Math.max(1, lRes.length()) - 1);
      res.put("XML_LOCI", lRes);
    }
    return res;
  }

  private String unitCheck(String string) {
    String[] split = string.split(":");
    if (split.length > 2)
      return string.replace(":", " -");
    if (split.length == 2) {
      String unit = split[1].trim();
      for (Unit u : Unit.values()) {
        if (u.getValue().equals(unit))
          return string;
      }
      return string.replace(":", " -");
    }
    return string;
  }

  private String parseXMLPartLabel(String colName) {
    return colName.split(": ")[1];
  }

  public String getError() {
    if (error != null)
      logger.error(error);
    else
      logger.info("Parsing of experimental design successful.");
    return error;
  }

  @Override
  public List<String> getTSVByRows() {
    return tsvByRows;
  }

}
