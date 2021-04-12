package life.qbic.expdesign.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.model.CultureProperties;
import life.qbic.expdesign.model.ExtendedMSProperties;
import life.qbic.expdesign.model.MetaboSampleHierarchy;
import life.qbic.expdesign.model.MetabolitePrepProperties;
import life.qbic.expdesign.model.StructuredExperiment;
import life.qbic.xml.properties.Unit;
import life.qbic.xml.study.TechnologyType;


public class MetaboDesignReader implements IExperimentalDesignReader {

  private List<String> mandatoryColumns;
  private List<String> mandatoryFilled;
  private List<String> optionalCols;
  private Map<SampleType, Map<String, List<String>>> headersToTypeCodePerSampletype;
  private Map<String, Set<String>> parsedCategoriesToValues;

  private String error;
  private Map<String, List<Map<String, Object>>> experimentInfos;
  private Set<String> speciesSet;
  private Set<String> tissueSet;
  private Set<String> analyteSet;
  private List<String> tsvByRows;
  private static final Logger logger = LogManager.getLogger(MetaboDesignReader.class);

  public static final String UTF8_BOM = "\uFEFF";
  public static final String LIST_SEPARATOR = "\\+";
  public static final Set<String> LABELING_TYPES_WITHOUT_LABELS =
      new HashSet<>(Arrays.asList("LFQ", "None"));
  public static final String SAMPLE_KEYWORD = "Secondary Name";
  // public static final String SAMPLE_ALTNAME_KEYWORD = "Sample Name";



  public MetaboDesignReader() {
    this.mandatoryColumns = new ArrayList<>(Arrays.asList("Secondary name", "Sample type",
        "Species", "Injection volume (uL)", "LCMS method name", "LC device", "LC detection method",
        "LC column name", "MS device", "MS ion mode"));
    this.mandatoryFilled = new ArrayList<>(
        Arrays.asList("Secondary name", "Sample type", "Species", "LCMS method name", "LC device",
            "LC detection method", "LC column name", "MS device", "MS ion mode"));
    this.optionalCols = new ArrayList<>(Arrays.asList("Strain lab collection number",
        "Culture type", "Growth conditions: Temperature (°C)", "Growth conditions: Time ",
        "Growth conditions: rpm", "Medium", "Condition: Stimulus", "Stimulation OD",
        "Stimulation  Time", "Harvesting conditions", "Washing solvent", "Cell lysis",
        "Lysis parameters", "Sample solvent", "SOP reference code for technical information"));

    Map<String, List<String>> sourceMetadata = new HashMap<>();
    sourceMetadata.put("Species", Collections.singletonList("Q_NCBI_ORGANISM"));
    sourceMetadata.put("Expression system", Collections.singletonList("Q_EXPRESSION_SYSTEM"));
    sourceMetadata.put("Strain lab collection number",
        Arrays.asList("Q_STRAIN_LAB_COLLECTION_NUMBER"));

    sourceMetadata.put("Medium", Arrays.asList());// TODO
    sourceMetadata.put("Culture type", Arrays.asList());// TODO

    Map<String, List<String>> extractMetadata = new HashMap<>();
    extractMetadata.put("Tissue", Collections.singletonList("Q_PRIMARY_TISSUE"));
    // extractMetadata.put("Customer Comment", Collections.singletonList("Q_ADDITIONAL_INFO"));
    extractMetadata.put("Sample type", Collections.singletonList("Q_TISSUE_DETAILED"));


    Map<String, List<String>> molMetadata = new HashMap<>();
    // peptideMetadata.put("Label", Collections.singletonList("Q_MOLECULAR_LABEL"));
    // peptideMetadata.put("Secondary Name", Collections.singletonList("Q_SECONDARY_NAME"));
    // peptideMetadata.put("Sample Secondary Name", "Q_EXTERNALDB_ID");

    Map<String, List<String>> msRunMetadata = new HashMap<>();
    // msRunMetadata.put("Facility Comment", Collections.singletonList("Q_ADDITIONAL_INFO"));
    msRunMetadata.put("Injection volume (uL)", Collections.singletonList("Q_INJECTION_VOLUME"));
    msRunMetadata.put("Sample solvent", Collections.singletonList("Q_SAMPLE_SOLVENT"));

    headersToTypeCodePerSampletype = new HashMap<>();
    headersToTypeCodePerSampletype.put(SampleType.Q_BIOLOGICAL_ENTITY, sourceMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_BIOLOGICAL_SAMPLE, extractMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_TEST_SAMPLE, molMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_MS_RUN, msRunMetadata);
  }

  private void fillParsedCategoriesToValuesForRow(Map<String, Integer> headerMapping,
      String[] row) {
    // logger.info("Collecting possible CV entries for row.");
    addValueForCategory(headerMapping, row, "Sample type");
    addValueForCategory(headerMapping, row, "Species");
    addValueForCategory(headerMapping, row, "Culture type");
    addValueForCategory(headerMapping, row, "Medium");
    addValueForCategory(headerMapping, row, "Harvesting conditions");
    addValueForCategory(headerMapping, row, "Cell lysis");
    addValueForCategory(headerMapping, row, "Lysis parameters");
    addValueForCategory(headerMapping, row, "LCMS method name");
    addValueForCategory(headerMapping, row, "LC device");
    addValueForCategory(headerMapping, row, "LC detection method");
    addValueForCategory(headerMapping, row, "LC column name");
    addValueForCategory(headerMapping, row, "MS device");
    addValueForCategory(headerMapping, row, "MS ion mode");
  }

  private void addValueForCategory(Map<String, Integer> headerMapping, String[] row, String cat) {
    if (headerMapping.containsKey(cat)) {
      String val = row[headerMapping.get(cat)];
      if (val != null && !val.isEmpty()) {
        for (String v : val.split(LIST_SEPARATOR)) {
          if (parsedCategoriesToValues.containsKey(cat)) {
            parsedCategoriesToValues.get(cat).add(v);
          } else {
            Set<String> set = new HashSet<String>();
            set.add(v);
            parsedCategoriesToValues.put(cat, set);
          }
        }
      }
    }
  }

  public Map<String, List<Map<String, Object>>> getExperimentInfos() {
    return experimentInfos;
  }

  private static String removeUTF8BOM(String s) {
    if (s.startsWith(UTF8_BOM)) {
      s = s.substring(1);
    }
    return s;
  }

  /**
   * Reads in a TSV file containing openBIS samples that should be registered. Returns a List of
   * TSVSampleBeans containing all the necessary information to register each sample with its meta
   * information to openBIS, given that the types and parents exist.
   * 
   * @param file
   * @return ArrayList of TSVSampleBeans
   * @throws IOException
   */
  public List<ISampleBean> readSamples(File file, boolean parseGraph) throws IOException {
    tsvByRows = new ArrayList<String>();
    parsedCategoriesToValues = new HashMap<>();

    BufferedReader reader = new BufferedReader(new FileReader(file));
    ArrayList<String[]> data = new ArrayList<String[]>();
    String next;
    int i = 0;
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
    String[] trimmedHeader = new String[header.length];
    for (int j = 0; j < header.length; j++) {
      trimmedHeader[j] = header[j].trim();
    }
    header = trimmedHeader;
    data.remove(0);
    // find out where the mandatory and other metadata data is
    Map<String, Integer> headerMapping = new HashMap<>();
    List<Integer> meta = new ArrayList<>();
    Set<Integer> factors = new HashSet<>();
    List<Integer> properties = new ArrayList<>();
    List<Integer> loci = new ArrayList<>();

    ArrayList<String> found = new ArrayList<String>(Arrays.asList(header));
    for (String col : mandatoryColumns) {
      if (!found.contains(col)) {
        error = "Mandatory column " + col + " not found.";
        return null;
      }
    }
    for (i = 0; i < header.length; i++) {
      int position = mandatoryColumns.indexOf(header[i]);
      if (position == -1)
        position = optionalCols.indexOf(header[i]);
      if (position > -1) {
        headerMapping.put(header[i], i);
        meta.add(i);
      } else if (header[i].contains("condition") && header[i].contains(":")) {
        String condition = tryReplacePredefinedConditionNames(header[i]);
        if (condition.contains(":")) {
          condition = header[i].split(":")[1].trim();
        }

        for (char c : condition.toCharArray()) {
          if (Character.isUpperCase(c)) {
            error = "Conditions are not allowed to contain upper case characters: " + condition;
            return null;
          }
        }
        if (condition.contains(" ")) {
          error = "Conditions are not allowed to contain spaces: " + condition;
          return null;
        }
        char first = condition.charAt(0);
        if (first >= '0' && first <= '9') {
          error = "Conditions are not allowed to start with a number: " + condition;
          return null;
        }
        factors.add(i);
      } else if (header[i].contains("Locus:")) {
        loci.add(i);
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
        String sourceID = row[headerMapping.get("Secondary name")]; // for now: one row, one new
                                                                    // source
        // String replicateID = "";
        // if (headerMapping.containsKey("Technical Replicates")) {
        // replicateID = row[headerMapping.get("Technical Replicates")];
        // }
        String extractID = sourceID + row[headerMapping.get("Sample type")];// + replicateID; TODO
                                                                            // might use more cols
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
    // if (parseGraph) {
    // for (int factorCol : factors) {
    // String label = parseXMLPartLabel(header[factorCol]);
    // nodesForFactorPerLabel.put(label, new LinkedHashSet<SampleSummary>());
    // }
    // nodesForFactorPerLabel.put("None", new LinkedHashSet<SampleSummary>());
    // }
    // create samples
    List<ISampleBean> beans = new ArrayList<>();

    // worst case order of samples (hierarchy):
    // organism - tissue - protein - peptide - protein pool - peptides of pool - protein
    // fractions/cycles - peptides of fractions/cycles - pool of fractions - peptides of pool - ms
    // measurements
    List<MetaboSampleHierarchy> order =
        new ArrayList<>(Arrays.asList(MetaboSampleHierarchy.values()));
    Map<MetaboSampleHierarchy, List<ISampleBean>> samplesInOrder = new HashMap<>();
    for (MetaboSampleHierarchy level : order) {
      samplesInOrder.put(level, new ArrayList<>());
    }

    Map<String, TSVSampleBean> sourceIDToSample = new HashMap<>();
    Map<String, TSVSampleBean> tissueToSample = new HashMap<>();
    Map<String, TSVSampleBean> metaboliteToSample = new HashMap<>();
    // TODO another level of measurements?
    Map<ExtendedMSProperties, String> msPropertiesToID = new HashMap<>();
    Map<CultureProperties, String> culturePropertiesToID = new HashMap<>();
    Map<MetabolitePrepProperties, String> metaboPrepPropertiesToID = new HashMap<>();

    speciesSet = new HashSet<String>();
    tissueSet = new HashSet<String>();
    analyteSet = new HashSet<String>();
    analyteSet.add("SMALLMOLECULES");
    int rowID = 0;
    int sampleID = 0;

    for (String[] row : data) {
      fillParsedCategoriesToValuesForRow(headerMapping, row);
      rowID++;
      boolean special = false;
      if (!special) {
        for (String col : mandatoryFilled) {
          if (row[headerMapping.get(col)].isEmpty()) {
            error = col + " is a mandatory field, but it is not set for row " + rowID + "!";
            return null;
          }
        }
        String sourceID = row[headerMapping.get("Secondary name")];
        String species = row[headerMapping.get("Species")];
        String expressionSystem = null;
        // if (headerMapping.containsKey("Expression System")) {
        // expressionSystem = row[headerMapping.get("Expression System")];
        // }
        // String tissue = row[headerMapping.get("Tissue")];//TODO whole organism?
        String replicateID = "";
        // if (headerMapping.containsKey("Technical Replicates")) {
        // replicateID = row[headerMapping.get("Technical Replicates")];
        // }

        // String fileName = row[headerMapping.get("File Name")];// TODO Sample Number?

        String sampleKey = row[headerMapping.get(SAMPLE_KEYWORD)];
        // String sampleAltName = row[headerMapping.get(SAMPLE_ALTNAME_KEYWORD)];

        String cultureMedium = row[headerMapping.get("Medium")];
        String harvestingConditions = row[headerMapping.get("Harvesting conditions")];
        String washingSolvent = row[headerMapping.get("Washing solvent")];
        String sampleSolvent = row[headerMapping.get("Sample solvent")];
        String cultureType = row[headerMapping.get("Culture type")];
        String sampleType = row[headerMapping.get("Sample type")];
        String cellLysis = row[headerMapping.get("Cell lysis")];
        String lysisParams = row[headerMapping.get("Lysis parameters")];
        String strainCollNumber = row[headerMapping.get("Strain lab collection number")];

        speciesSet.add(species);
        // tissueSet.add(tissue);//TODO set standard name and show user?

        // always one new measurement per row
        sampleID++;
        TSVSampleBean msRun = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_MS_RUN, "",
            fillMetadata(header, row, meta, factors, new ArrayList<>(), SampleType.Q_MS_RUN));
         msRun.addProperty("File", sampleKey);//TODO? file name
        msRun.addProperty("Q_SAMPLE_SOLVENT", sampleSolvent);

        String lcmsMethod = row[headerMapping.get("LCMS method name")];
        String msDevice = row[headerMapping.get("MS device")];
        String lcDevice = row[headerMapping.get("LC device")];
        String column = row[headerMapping.get("LC column name")];
        String ionMode = row[headerMapping.get("MS ion mode")];
        String lcDetectionMethod = row[headerMapping.get("LC detection method")];// TODO do not put
                                                                                 // this into ms
                                                                                 // props
        ExtendedMSProperties msProperties = new ExtendedMSProperties(lcmsMethod, msDevice);
        msProperties.setLCDetectionMethod(lcDetectionMethod);
        msProperties.setLCDevice(lcDevice);
        msProperties.setIonizationMode(ionMode);
        msProperties.setColumnName(column);
        msProperties.setWashingSolvent(washingSolvent);
        String expID = Integer.toString(msProperties.hashCode());
        msPropertiesToID.put(msProperties, expID);
        msRun.setExperiment(expID);

        samplesInOrder.get(MetaboSampleHierarchy.MassSpecRun).add(msRun);

        // if organism id not known => create organism entity. put in map.
        // else get organism entity. same for tissue. this is done for pool rows, as well
        TSVSampleBean sampleSource = sourceIDToSample.get(sourceID);
        if (sampleSource == null) {
          sampleID++;

          sampleSource = new TSVSampleBean(Integer.toString(sampleID),
              SampleType.Q_BIOLOGICAL_ENTITY, sourceID,
              fillMetadata(header, row, meta, entityFactors, loci, SampleType.Q_BIOLOGICAL_ENTITY));
          sampleSource.addProperty("Q_STRAIN_LAB_COLLECTION_NUMBER", strainCollNumber);
          // sampleSource.addProperty("Q_EXTERNALDB_ID", sourceID);
          samplesInOrder.get(MetaboSampleHierarchy.Organism).add(sampleSource);
          sourceIDToSample.put(sourceID, sampleSource);

          CultureProperties props = new CultureProperties(cultureMedium, cultureType);
          String cultExpID = Integer.toString(props.hashCode());
          culturePropertiesToID.put(props, cultExpID);
          sampleSource.setExperiment(cultExpID);

          if (expressionSystem != null) {
            speciesSet.add(expressionSystem);
            sampleSource.addProperty("Q_EXPRESSION_SYSTEM", expressionSystem);
          }
        }
        // we don't have tissue ids, so we build unique identifiers by adding sourceID and tissue
        // name
        // TODO use sample type instead of tissue
        String tissueID = sourceID + "-" + sampleType + "-" + replicateID;
        TSVSampleBean tissueSample = tissueToSample.get(tissueID);
        if (tissueSample == null) {
          sampleID++;

          tissueSample = new TSVSampleBean(Integer.toString(sampleID),
              SampleType.Q_BIOLOGICAL_SAMPLE, tissueID, fillMetadata(header, row, meta,
                  extractFactors, loci, SampleType.Q_BIOLOGICAL_SAMPLE));
          tissueSample.addProperty("Q_PRIMARY_TISSUE", "Whole organism");
          samplesInOrder.get(MetaboSampleHierarchy.Tissue).add(tissueSample);
          tissueSample.addParentID(sampleSource.getCode());
          tissueSample.addProperty("Q_EXTERNALDB_ID", tissueID);
          tissueToSample.put(tissueID, tissueSample);

          MetabolitePrepProperties props =
              new MetabolitePrepProperties(harvestingConditions, cellLysis, lysisParams);
          String prepExpID = Integer.toString(props.hashCode());
          metaboPrepPropertiesToID.put(props, prepExpID);
          tissueSample.setExperiment(prepExpID);
        }

        // if sample secondary name not known => create metabolite sample
        TSVSampleBean metabolite = metaboliteToSample.get(sampleKey);
        if (metabolite == null) {
          sampleID++;

          metabolite = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE,
              sampleKey, fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
          samplesInOrder.get(MetaboSampleHierarchy.Molecules).add(metabolite);
          metabolite.addParentID(tissueSample.getCode());
          metabolite.addProperty("Q_EXTERNALDB_ID", sampleKey);
          metaboliteToSample.put(sampleKey, metabolite);
          metabolite.addProperty("Q_SAMPLE_TYPE", "SMALLMOLECULES");
        }
        msRun.addParentID(metabolite.getCode());
      }
    }
    experimentInfos = new HashMap<String, List<Map<String, Object>>>();

    // Cell cultures
    List<Map<String, Object>> cultures = new ArrayList<Map<String, Object>>();
    for (CultureProperties props : culturePropertiesToID.keySet()) {
      Map<String, Object> propMap = props.getPropertyMap();
      propMap.put("Code", Integer.toString(props.hashCode()));// used to match samples to their
      // experiments later
      cultures.add(propMap);
    }
    experimentInfos.put("Q_EXPERIMENTAL_DESIGN", cultures);

    // Extract preparation experiments
    List<Map<String, Object>> extractPreparations = new ArrayList<Map<String, Object>>();
    for (MetabolitePrepProperties props : metaboPrepPropertiesToID.keySet()) {
      Map<String, Object> propMap = props.getPropertyMap();
      propMap.put("Code", Integer.toString(props.hashCode()));// used to match samples to their
      // experiments later
      extractPreparations.add(propMap);
    }
    experimentInfos.put("Q_SAMPLE_EXTRACTION", extractPreparations);

    // MS experiments
    List<Map<String, Object>> msExperiments = new ArrayList<Map<String, Object>>();
    for (ExtendedMSProperties expProperties : msPropertiesToID.keySet()) {
      Map<String, Object> propMap = expProperties.getPropertyMap();
      propMap.put("Code", msPropertiesToID.get(expProperties));
      msExperiments.add(propMap);
    }
    experimentInfos.put("Q_MS_MEASUREMENT", msExperiments);
    for (MetaboSampleHierarchy level : order) {
      beans.addAll(samplesInOrder.get(level));
      // printSampleLevel(samplesInOrder.get(level));TODO
    }
    return beans;
  }

  private String tryReplacePredefinedConditionNames(String condition) {
    Map<String, String> headNamesToConditions = new HashMap<>();
    headNamesToConditions.put("Growth conditions: Temperature (°C)", "growth_temperature");
    headNamesToConditions.put("Growth conditions: Temperature", "growth_temperature");
    headNamesToConditions.put("Growth conditions: Time", "growth_time");
    headNamesToConditions.put("Growth conditions: rpm", "rpm");
    if (headNamesToConditions.containsKey(condition)) {
      return headNamesToConditions.get(condition);
    }
    return condition;
  }

  private void printSampleLevel(List<ISampleBean> level) {
    logger.info("###");
    logger.info("New Level of Samples:");
    for (ISampleBean s : level) {
      logger.info("Sample " + s.getSecondaryName() + " (id: " + s.getCode() + ")");
      logger.info("Parents: " + s.getParentIDs());
      logger.info(s.getType());
      logger.info(s.getMetadata());
      logger.info("#");
    }
    logger.info("End of Sample Level");
  }

  private List<String> parsePoolParents(String poolName) {
    return new ArrayList<>(Arrays.asList(poolName.split(LIST_SEPARATOR)));
  }

  // private Map<String, Object> parseMSExperimentData(String[] row,
  // Map<String, Integer> headerMapping, HashMap<String, Object> metadata) {
  // Map<String, String> designMap = new HashMap<String, String>();
  // designMap.put("MS Device", "Q_MS_DEVICE");
  // designMap.put("LC Column", "Q_CHROMATOGRAPHY_TYPE");
  // designMap.put("LCMS Method", "Q_MS_LCMS_METHOD");
  // for (String col : designMap.keySet()) {
  // Object val = "";
  // String openbisType = designMap.get(col);
  // if (headerMapping.containsKey(col)) {
  // val = row[headerMapping.get(col)];
  // if (parsers.containsKey(openbisType)) {
  // val = parsers.get(openbisType).parse((String) val);
  // }
  // }
  // metadata.put(openbisType, val);
  // }
  // return metadata;
  // }

  public Set<String> getSpeciesSet() {
    return speciesSet;
  }

  public Set<String> getTissueSet() {
    return tissueSet;
  }

  private boolean checkUniqueIDsBetweenSets(Set<String> speciesSet, Set<String> tissueSet) {
    Set<String> intersection1 = new HashSet<String>(speciesSet);
    intersection1.retainAll(tissueSet);
    if (!intersection1.isEmpty()) {
      error = "Entry " + intersection1.iterator().next()
          + " was found as organism and tissue. It can't be both!";
      return false;
    }
    return true;
  }

  private String trySplitMetadata(String line, String keyword) {
    try {
      return line.split(keyword)[1].trim();
    } catch (ArrayIndexOutOfBoundsException e) {
      return "";
    }
  }

  private String replaceSpecials(String s) {
    return s.replace("%%%", "#").replace(">>>", "=");
  }

  private HashMap<String, Object> fillMetadata(String[] header, String[] data, List<Integer> meta,
      Set<Integer> factors, List<Integer> loci, SampleType type) {
    Map<String, List<String>> headersToOpenbisCode = headersToTypeCodePerSampletype.get(type);
    HashMap<String, Object> res = new HashMap<String, Object>();
    if (headersToOpenbisCode != null) {
      for (int i : meta) {
        String label = header[i];
        if (!data[i].isEmpty() && headersToOpenbisCode.containsKey(label)) {
          for (String propertyCode : headersToOpenbisCode.get(label)) {
            Object val = data[i];
            // if (parsers.containsKey(propertyCode))
            // val = parsers.get(propertyCode).parse(data[i]);
            res.put(propertyCode, val);
          }
        }
      }
    }
    if (factors.size() > 0)

    {
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
  public Set<String> getAnalyteSet() {
    return new HashSet<String>(Arrays.asList("PROTEINS", "PEPTIDES"));
  }

  @Override
  public List<String> getTSVByRows() {
    return tsvByRows;
  }

  @Override
  public StructuredExperiment getGraphStructure() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  // TODO
  public int countEntities(File file) throws IOException {
    return -1;
  }

  @Override
  public List<TechnologyType> getTechnologyTypes() {
    ArrayList<TechnologyType> res = new ArrayList<>();
    res.add(new TechnologyType("Proteomics"));
    if (analyteSet.contains("PEPTIDES")) {
      res.add(new TechnologyType("Peptidomics"));
    }
    return res;
  }

  @Override
  public Map<String, List<String>> getParsedCategoriesToValues(List<String> header) {
    Map<String, List<String>> res = new HashMap<>();
    for (String cat : header) {
      if (parsedCategoriesToValues.containsKey(cat)) {
        res.put(cat, new ArrayList<>(parsedCategoriesToValues.get(cat)));
      } else {
        logger.warn(cat + " not found");
      }
    }
    return res;
  }

}
