package life.qbic.expdesign.io;

import java.io.BufferedReader;
import java.io.File;
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
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.expdesign.model.OpenbisPropertyCodes;
import life.qbic.expdesign.model.StandardExperimentProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleSummary;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.ParserHelpers;
import life.qbic.expdesign.model.StructuredExperiment;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;
import life.qbic.xml.properties.Unit;
import life.qbic.xml.study.TechnologyType;
import org.junit.jupiter.api.DisplayNameGenerator.Standard;

public class EasyDesignReader implements IExperimentalDesignReader {

  private static final Logger logger = LogManager.getLogger(EasyDesignReader.class);
  private List<String> mandatory;
  private boolean analytesIncluded;
  private Map<SampleType, Map<String, String>> headersToTypeCodePerSampletype;

  private String error;
  private Map<ExperimentType, List<Map<String, Object>>> experimentInfos;
  private Set<String> speciesSet;
  private Set<String> tissueSet;
  private Set<String> analyteSet;
  private List<String> tsvByRows;

  private Map<String, Set<SampleSummary>> nodesForFactorPerLabel;
  private Map<String, Set<String>> parsedCategoriesToValues;

  public EasyDesignReader() {
    Set<String> mandatory =
        new HashSet<>();
    mandatory.add(StandardExperimentProperties.Organism.label);
    mandatory.add(StandardExperimentProperties.Organism_ID.label);
    mandatory.add(StandardExperimentProperties.Tissue.label);
    mandatory.add(StandardExperimentProperties.Extract_ID.label);
    this.mandatory = new ArrayList<String>(mandatory);

    Map<String, String> sourceMetadata = new HashMap<>();
    sourceMetadata.put(StandardExperimentProperties.Organism.label, OpenbisPropertyCodes.Q_NCBI_ORGANISM.name());
    sourceMetadata.put(StandardExperimentProperties.Organism_ID.label, OpenbisPropertyCodes.Q_EXTERNALDB_ID.name());
    sourceMetadata.put(StandardExperimentProperties.Source_Comment.label, OpenbisPropertyCodes.Q_ADDITIONAL_INFO.name());
    Map<String, String> extractMetadata = new HashMap<>();
    extractMetadata.put(StandardExperimentProperties.Tissue.label, OpenbisPropertyCodes.Q_PRIMARY_TISSUE.name());
    extractMetadata.put(StandardExperimentProperties.Extract_ID.label, OpenbisPropertyCodes.Q_EXTERNALDB_ID.name());
    extractMetadata.put(StandardExperimentProperties.Tissue_Comment.label, OpenbisPropertyCodes.Q_ADDITIONAL_INFO.name());
    extractMetadata.put(StandardExperimentProperties.Detailed_Tissue.label, OpenbisPropertyCodes.Q_TISSUE_DETAILED.name());
    Map<String, String> prepMetadata = new HashMap<>();
    prepMetadata.put(StandardExperimentProperties.Analyte.label, OpenbisPropertyCodes.Q_SAMPLE_TYPE.name());
    prepMetadata.put(StandardExperimentProperties.Analyte_ID.label, OpenbisPropertyCodes.Q_EXTERNALDB_ID.name());
    prepMetadata.put(StandardExperimentProperties.Preparation_Comment.label, OpenbisPropertyCodes.Q_ADDITIONAL_INFO.name());
    headersToTypeCodePerSampletype = new HashMap<>();
    headersToTypeCodePerSampletype.put(SampleType.Q_BIOLOGICAL_ENTITY, sourceMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_BIOLOGICAL_SAMPLE, extractMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_TEST_SAMPLE, prepMetadata);
  }

  private void fillParsedCategoriesToValuesForRow(Map<String, Integer> headerMapping,
      String[] row) {
    addValueForCategory(headerMapping, row, "Species");
    addValueForCategory(headerMapping, row, StandardExperimentProperties.Organism.label);
    addValueForCategory(headerMapping, row, StandardExperimentProperties.Analyte.label);
    addValueForCategory(headerMapping, row, StandardExperimentProperties.Expression_System.label);
  }

  private void addValueForCategory(Map<String, Integer> headerMapping, String[] row, String cat) {
    if (headerMapping.containsKey(cat)) {
      String val = row[headerMapping.get(cat)];
      if (val != null && !val.isEmpty()) {
        if (parsedCategoriesToValues.containsKey(cat)) {
          parsedCategoriesToValues.get(cat).add(val);
        } else {
          Set<String> set = new HashSet<String>();
          set.add(val);
          parsedCategoriesToValues.put(cat, set);
        }
      }
    }
  }

  public Map<ExperimentType, List<Map<String, Object>>> getExperimentInfos() {
    return experimentInfos;
  }

  public int countEntities(File file) throws IOException {
    Set<String> ids = new HashSet<>();
    nodesForFactorPerLabel = new HashMap<>();

    tsvByRows = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new FileReader(file));
    ArrayList<String[]> data = new ArrayList<>();
    String next;
    int i = 0;
    // isPilot = false;
    while ((next = reader.readLine()) != null) {
      i++;
      next = ParserHelpers.removeUTF8BOM(next);
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
    Map<String, Integer> headerMapping = new HashMap<>();
    List<Integer> meta = new ArrayList<>();

    ArrayList<String> found = new ArrayList<>(Arrays.asList(header));
    for (String col : mandatory) {
      if (!found.contains(col)) {
        return -1;
      }
    }
    if (found.contains(StandardExperimentProperties.Analyte.label) && found.contains(StandardExperimentProperties.Analyte_ID.label)) {
      headerMapping.put(StandardExperimentProperties.Analyte.label, found.indexOf(StandardExperimentProperties.Analyte.label));
      headerMapping.put(StandardExperimentProperties.Analyte_ID.label, found.indexOf(StandardExperimentProperties.Analyte_ID.label));
      analytesIncluded = true;
    } else if (!found.contains(StandardExperimentProperties.Analyte.label) && !found.contains(StandardExperimentProperties.Analyte_ID.label))
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
      String sourceID = row[headerMapping.get(StandardExperimentProperties.Organism_ID.label)];
      String extractID = row[headerMapping.get(StandardExperimentProperties.Extract_ID.label)];
      ids.add(sourceID);
      ids.add(extractID);
      String analyteID = "";
      if (analytesIncluded) {
        analyteID = row[headerMapping.get(StandardExperimentProperties.Analyte_ID.label)];
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
    parsedCategoriesToValues = new HashMap<>();
    nodesForFactorPerLabel = new HashMap<>();

    tsvByRows = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new FileReader(file));
    ArrayList<String[]> data = new ArrayList<>();
    String next;
    int i = 0;
    // isPilot = false;
    while ((next = reader.readLine()) != null) {
      i++;
      next = ParserHelpers.removeUTF8BOM(next);
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
    int rowCount = data.size();
    // find out where the mandatory and other metadata data is
    Map<String, Integer> headerMapping = new HashMap<>();
    List<Integer> meta = new ArrayList<>();
    Set<Integer> factors = new HashSet<>();
    List<Integer> properties = new ArrayList<>();
    List<Integer> loci = new ArrayList<>();
    int numOfLevels = 2;

    ArrayList<String> found = new ArrayList<>(Arrays.asList(header));
    for (String col : mandatory) {
      if (!found.contains(col)) {
        error = "Mandatory column " + col + " not found.";
        return null;
      }
    }
    String analyteName = StandardExperimentProperties.Analyte.label;
    String analyteIDName = StandardExperimentProperties.Analyte_ID.label;
    if (found.contains(analyteName) && found.contains(analyteIDName)) {
      headerMapping.put(analyteName, found.indexOf(analyteName));
      headerMapping.put(analyteIDName, found.indexOf(analyteIDName));
      analytesIncluded = true;
    } else if (!found.contains(analyteName) && !found.contains(analyteIDName))
      analytesIncluded = false;
    else {
      error =
          "One of the columns "+analyteName+" and "+analyteIDName+" was not found. Both are needed to add "+analyteName+" samples.";
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
    Set<Integer> entityFactors = new HashSet<>();
    Set<Integer> extractFactors = new HashSet<>();
    for (int col : factors) {
      Map<String, String> idToVal = new HashMap<>();
      boolean ent = true;
      boolean extr = true;
      for (String[] row : data) {
        String val = row[col];
        String sourceID = row[headerMapping.get(StandardExperimentProperties.Organism_ID.label)];
        String extractID = row[headerMapping.get(StandardExperimentProperties.Extract_ID.label)];
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
    List<ISampleBean> beans = new ArrayList<>();
    List<List<ISampleBean>> order = new ArrayList<>();
    Map<String, TSVSampleBean> sourceIDToSample = new HashMap<>();
    Map<String, TSVSampleBean> extractIDToSample = new HashMap<>();
    Map<String, TSVSampleBean> analyteIDToSample = new HashMap<>();
    Set<String> speciesSet = new HashSet<>();
    Set<String> tissueSet = new HashSet<>();
    Set<String> analyteSet = new HashSet<>();
    int rowID = 0;
    // int sampleID = 0;
    for (String[] row : data) {
      fillParsedCategoriesToValuesForRow(headerMapping, row);
      rowID++;
      // boolean special = false;
      // if (!special) {
      for (String col : mandatory) {
        if (row[headerMapping.get(col)].isEmpty()) {
          error = col + " is a mandatory field, but it is not set for row " + rowID + "!";
          return null;
        }
      }
      String sourceID = row[headerMapping.get(StandardExperimentProperties.Organism_ID.label)];
      String extractID = row[headerMapping.get(StandardExperimentProperties.Extract_ID.label)];
      String species = row[headerMapping.get(StandardExperimentProperties.Organism.label)];
      String tissue = row[headerMapping.get(StandardExperimentProperties.Tissue.label)];

      String analyteID = "";
      String analyte = "";
      if (analytesIncluded) {
        numOfLevels++;
        analyteID = row[headerMapping.get(StandardExperimentProperties.Analyte_ID.label)];
        analyte = row[headerMapping.get(StandardExperimentProperties.Analyte.label)];
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
          TSVSampleBean firstASample = new TSVSampleBean(analyteID, SampleType.Q_TEST_SAMPLE,
              analyteID, fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
          firstASample.addProperty(OpenbisPropertyCodes.Q_SAMPLE_TYPE.name(), analyte);
          order.get(2).add(firstASample);
          firstASample.addParentID(extractID);
          analyteIDToSample.put(analyteID, firstASample);
        }

        // these will be filled below, if they don't exist
        TSVSampleBean eSample = extractIDToSample.get(extractID);
        TSVSampleBean sSample = sourceIDToSample.get(sourceID);
        if (!extractIDToSample.containsKey(extractID)) {
          // sampleID++;
          eSample =
              new TSVSampleBean(extractID, SampleType.Q_BIOLOGICAL_SAMPLE, extractID, fillMetadata(
                  header, row, meta, extractFactors, loci, SampleType.Q_BIOLOGICAL_SAMPLE));
          eSample.addProperty(OpenbisPropertyCodes.Q_PRIMARY_TISSUE.name(), tissue);
          order.get(1).add(eSample);
          extractIDToSample.put(extractID, eSample);
          eSample.addParentID(sourceID);

        }
        if (!sourceIDToSample.containsKey(sourceID)) {
          // sampleID++;
          sSample = new TSVSampleBean(sourceID, SampleType.Q_BIOLOGICAL_ENTITY, sourceID,
              fillMetadata(header, row, meta, entityFactors, loci, SampleType.Q_BIOLOGICAL_ENTITY));
          sSample.addProperty(OpenbisPropertyCodes.Q_NCBI_ORGANISM.name(), species);
          order.get(0).add(sSample);
          sourceIDToSample.put(sourceID, sSample);
        }
        if (parseGraph) {
          List<TSVSampleBean> sampleRow = new ArrayList<>(
              Arrays.asList(sourceIDToSample.get(sourceID), extractIDToSample.get(extractID)));
          if (analytesIncluded)
            sampleRow.add(analyteIDToSample.get(analyteID));
          createGraphSummariesForRow(sampleRow, new Integer(rowID), rowCount);
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

  private void createGraphSummariesForRow(List<TSVSampleBean> levels, int rowID, int rowCount)
      throws JAXBException {
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
          // there can be rowCount number of entities/samples in each tier, so we offset the ID for
          // each level by that much
          int locNodeID = rowID + (rowCount * level);
          Set<SampleSummary> parentSummaries = new LinkedHashSet<SampleSummary>();
          if (currentSummary != null)
            parentSummaries.add(currentSummary);
          currentSummary = createNodeSummary(s, parentSummaries, label, locNodeID, leaf);
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
  public StructuredExperiment getGraphStructure() {
    Map<String, List<SampleSummary>> factorsToSamples = new HashMap<>();
    for (String label : nodesForFactorPerLabel.keySet()) {
      Set<SampleSummary> nodes = nodesForFactorPerLabel.get(label);
      factorsToSamples.put(label, new ArrayList<>(nodes));
    }
    return new StructuredExperiment(factorsToSamples);
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

  private Property getFactorOfSampleOrNull(List<Property> props, String factorLabel)
      throws JAXBException {
    if (props != null) {
      for (Property f : props) {
        if (f.getLabel().equals(factorLabel))
          return f;
      }
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
    SampleType type = s.getType();
    String source = "unknown";
    Map<String, Object> props = s.getMetadata();
    // List<Property> factors = ParserHelpers.getPropsFromString(props);
    ParserHelpers.fixProps(props);
    Property factor = getFactorOfSampleOrNull((List<Property>) props.get(OpenbisPropertyCodes.Q_PROPERTIES.name()), label);

    // Property factor = getFactorOfSampleOrNull(factors, label);
    boolean newFactor = true;
    Set<String> parentSources = new HashSet<>();
    Set<Integer> parentIDs = new HashSet<>();
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
      case Q_BIOLOGICAL_ENTITY:
        source = (String) props.get(OpenbisPropertyCodes.Q_NCBI_ORGANISM.name());
        value = source + " " + value;
        break;
      case Q_BIOLOGICAL_SAMPLE:
        source = (String) props.get(OpenbisPropertyCodes.Q_PRIMARY_TISSUE.name());
        if (!newFactor || source.equals(value)) {
          value = source;
        } else {
          value = source + " " + value;
        }
        break;
      case Q_TEST_SAMPLE:
        source = (String) props.get(OpenbisPropertyCodes.Q_SAMPLE_TYPE.name());
        value = source + " " + value;
        break;
      // case "Q_MHC_LIGAND_EXTRACT":
      // source = (String) props.get("Q_MHC_CLASS");
      // value = source;
      // break;
    }
    return new SampleSummary(currentID, parentIDs, new ArrayList<ISampleBean>(Arrays.asList(s)),
        factor.getValue(), tryShortenName(value, s).trim(), type.toString(), leaf);
  }

  private String tryShortenName(String key, TSVSampleBean s) {
    switch (s.getType()) {
      case Q_BIOLOGICAL_ENTITY:
        return key;
      case Q_BIOLOGICAL_SAMPLE:
        return key;
      case Q_TEST_SAMPLE:
        String type = (String) s.getMetadata().get(OpenbisPropertyCodes.Q_SAMPLE_TYPE.name());
        return key.replace(type, "") + " " + type;// shortenInfo(type);
      // case "Q_MHC_LIGAND_EXTRACT":
      // return s.getProperties().get("Q_MHC_CLASS").replace("_", " ").replace("CLASS", "Class");
    }
    return key;
  }

  private boolean checkUniqueIDsBetweenSets(Set<String> speciesSet, Set<String> tissueSet,
      Set<String> analyteSet) {
    Set<String> intersection1 = new HashSet<>(speciesSet);
    intersection1.retainAll(tissueSet);
    Set<String> intersection2 = new HashSet<>(speciesSet);
    intersection2.retainAll(analyteSet);
    Set<String> intersection3 = new HashSet<>(tissueSet);
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
    Set<String> intersection1 = new HashSet<>(speciesSet);
    intersection1.retainAll(tissueSet);
    Set<String> intersection2 = new HashSet<>(speciesSet);
    intersection2.retainAll(analyteSet);
    Set<String> intersection3 = new HashSet<>(tissueSet);
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

  private HashMap<String, Object> fillMetadata(String[] header, String[] data, List<Integer> meta,
      Set<Integer> factors, List<Integer> loci, SampleType type) {
    Map<String, String> headersToOpenbisCode = headersToTypeCodePerSampletype.get(type);
    HashMap<String, Object> res = new HashMap<>();
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

  @Override
  // TODO can't be sure at this point, should be handled in import view/controller
  public List<TechnologyType> getTechnologyTypes() {
    return new ArrayList<>();
  }

  @Override
  public Map<String, List<String>> getParsedValuesForColumns(List<String> colNames) {
    Map<String, List<String>> res = new HashMap<>();
    for (String columnName : colNames) {
      if (parsedCategoriesToValues.containsKey(columnName)) {
        res.put(columnName, new ArrayList<>(parsedCategoriesToValues.get(columnName)));
      } else {
        logger.warn(columnName + " not found");
      }
    }
    return res;
  }

}
