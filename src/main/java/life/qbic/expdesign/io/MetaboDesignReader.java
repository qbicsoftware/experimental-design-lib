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
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.expdesign.model.MetaboExperimentProperties;
import life.qbic.expdesign.model.OpenbisPropertyCodes;
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
  private HashMap<String, String> headerNamesToConditions;
  private Map<SampleType, Map<String, List<String>>> headersToTypeCodePerSampletype;
  private Map<String, Set<String>> parsedCategoriesToValues;

  private String error;
  private Map<ExperimentType, List<Map<String, Object>>> experimentInfos;
  private Set<String> speciesSet;
  private Set<String> tissueSet;
  private Set<String> analyteSet;
  private List<String> tsvByRows;
  private static final Logger logger = LogManager.getLogger(MetaboDesignReader.class);

  public static final String UTF8_BOM = "\uFEFF";
  public static final String LIST_SEPARATOR = "\\+";
  public static final Set<String> LABELING_TYPES_WITHOUT_LABELS =
      new HashSet<>(Arrays.asList("LFQ", "None"));
  public static final String SAMPLE_KEYWORD = MetaboExperimentProperties.Secondary_Name.label;

  public MetaboDesignReader() {
    this.mandatoryColumns = new ArrayList<>();
    this.mandatoryColumns.add(MetaboExperimentProperties.Secondary_Name.label);
    this.mandatoryColumns.add(MetaboExperimentProperties.Organism_ID.label);
    this.mandatoryColumns.add(MetaboExperimentProperties.Biospecimen.label);
    this.mandatoryColumns.add(MetaboExperimentProperties.Species.label);
    this.mandatoryColumns.add(MetaboExperimentProperties.Injection_Volume.label);
    this.mandatoryColumns.add(MetaboExperimentProperties.LCMS_Method_Name.label);
    this.mandatoryColumns.add(MetaboExperimentProperties.LC_Device.label);
    this.mandatoryColumns.add(MetaboExperimentProperties.LC_Detection_Method.label);
    this.mandatoryColumns.add(MetaboExperimentProperties.LC_Column_Name.label);
    this.mandatoryColumns.add(MetaboExperimentProperties.MS_Device.label);
    this.mandatoryColumns.add(MetaboExperimentProperties.MS_Ion_Mode.label);
    this.mandatoryColumns.add(MetaboExperimentProperties.Harvesting_Method.label);
    this.mandatoryColumns.add(MetaboExperimentProperties.Harvesting_Volume.label);
    this.mandatoryColumns.add(MetaboExperimentProperties.Technical_Comments.label);

    this.mandatoryFilled = new ArrayList<>();
    this.mandatoryFilled.add(MetaboExperimentProperties.Secondary_Name.label);
    this.mandatoryFilled.add(MetaboExperimentProperties.Organism_ID.label);
    this.mandatoryFilled.add(MetaboExperimentProperties.Biospecimen.label);
    this.mandatoryFilled.add(MetaboExperimentProperties.Species.label);

    this.mandatoryFilled.add(MetaboExperimentProperties.LCMS_Method_Name.label);
    this.mandatoryFilled.add(MetaboExperimentProperties.LC_Device.label);
    this.mandatoryFilled.add(MetaboExperimentProperties.LC_Detection_Method.label);
    this.mandatoryFilled.add(MetaboExperimentProperties.LC_Column_Name.label);

    this.optionalCols = new ArrayList<>();
    this.optionalCols.add(MetaboExperimentProperties.Strain_Lab_Collection_Number.label);
    this.optionalCols.add(MetaboExperimentProperties.Medium.label);
    this.optionalCols.add(MetaboExperimentProperties.Harvesting_Method.label);
    this.optionalCols.add(MetaboExperimentProperties.Harvesting_Volume.label);
    this.optionalCols.add(MetaboExperimentProperties.Technical_Comments.label);
    this.optionalCols.add(MetaboExperimentProperties.Washing_Solvent.label);
    this.optionalCols.add(MetaboExperimentProperties.Cell_Lysis.label);
    this.optionalCols.add(MetaboExperimentProperties.Lysis_Parameters.label);
    this.optionalCols.add(MetaboExperimentProperties.Sample_Solvent.label);
    this.optionalCols.add(MetaboExperimentProperties.Mass_Resolving_Power.label);
    this.optionalCols.add(MetaboExperimentProperties.Dissociation_Energy.label);
    this.optionalCols.add(MetaboExperimentProperties.Dissociation_Method.label);

    this.headerNamesToConditions = new HashMap<>();
    headerNamesToConditions.put("Growth conditions: Temperature (°C)", "growth_temperature");
    headerNamesToConditions.put("Growth conditions: Temperature", "growth_temperature");
    headerNamesToConditions.put("Growth conditions: Time", "growth_time");
    headerNamesToConditions.put("Growth conditions: rpm", "growth_rpm");
    headerNamesToConditions.put("Stimulation OD", "stimulation_od");
    headerNamesToConditions.put("Stimulation Time", "stimulation_time");
    headerNamesToConditions.put("Condition: Stimulus", "stimulus");

    Map<String, List<String>> sourceMetadata = new HashMap<>();
    sourceMetadata.put(MetaboExperimentProperties.Species.label, Collections.singletonList(
        OpenbisPropertyCodes.Q_NCBI_ORGANISM.name()));
    sourceMetadata.put(MetaboExperimentProperties.Expression_System.label, Collections.singletonList(OpenbisPropertyCodes.Q_EXPRESSION_SYSTEM.name()));
    sourceMetadata.put(MetaboExperimentProperties.Strain_Lab_Collection_Number.label,
        Arrays.asList(OpenbisPropertyCodes.Q_STRAIN_LAB_COLLECTION_NUMBER.name()));

    Map<String, List<String>> extractMetadata = new HashMap<>();
    extractMetadata.put(MetaboExperimentProperties.Biospecimen.label, Collections.singletonList(OpenbisPropertyCodes.Q_PRIMARY_TISSUE.name()));

    Map<String, List<String>> molMetadata = new HashMap<>();
    // peptideMetadata.put("Label", Collections.singletonList("Q_MOLECULAR_LABEL"));
    // peptideMetadata.put("Secondary Name", Collections.singletonList("Q_SECONDARY_NAME"));
    // peptideMetadata.put("Sample Secondary Name", "Q_EXTERNALDB_ID");

    Map<String, List<String>> msRunMetadata = new HashMap<>();
    msRunMetadata.put(MetaboExperimentProperties.Injection_Volume.label, Collections.singletonList(OpenbisPropertyCodes.Q_INJECTION_VOLUME.name()));
    msRunMetadata.put(MetaboExperimentProperties.Sample_Solvent.label, Collections.singletonList(OpenbisPropertyCodes.Q_SAMPLE_SOLVENT.name()));
    msRunMetadata.put(MetaboExperimentProperties.Technical_Comments.label, Collections.singletonList(OpenbisPropertyCodes.Q_ADDITIONAL_INFO.name()));

    headersToTypeCodePerSampletype = new HashMap<>();
    headersToTypeCodePerSampletype.put(SampleType.Q_BIOLOGICAL_ENTITY, sourceMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_BIOLOGICAL_SAMPLE, extractMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_TEST_SAMPLE, molMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_MS_RUN, msRunMetadata);
  }

  private void fillParsedCategoriesToValuesForRow(Map<String, Integer> headerMapping,
      String[] row) {
    // logger.info("Collecting possible CV entries for row.");
    addValueForCategory(headerMapping, row, MetaboExperimentProperties.Biospecimen.label);
    addValueForCategory(headerMapping, row, MetaboExperimentProperties.Species.label);
    addValueForCategory(headerMapping, row, MetaboExperimentProperties.Medium.label);
    addValueForCategory(headerMapping, row, MetaboExperimentProperties.Harvesting_Method.label);
    addValueForCategory(headerMapping, row, MetaboExperimentProperties.Cell_Lysis.label);
    addValueForCategory(headerMapping, row, MetaboExperimentProperties.Lysis_Parameters.label);
    addValueForCategory(headerMapping, row, MetaboExperimentProperties.LCMS_Method_Name.label);
    addValueForCategory(headerMapping, row, MetaboExperimentProperties.LC_Device.label);
    addValueForCategory(headerMapping, row, MetaboExperimentProperties.LC_Detection_Method.label);
    addValueForCategory(headerMapping, row, MetaboExperimentProperties.LC_Column_Name.label);
    addValueForCategory(headerMapping, row, MetaboExperimentProperties.MS_Device.label);
    addValueForCategory(headerMapping, row, MetaboExperimentProperties.MS_Ion_Mode.label);
    addValueForCategory(headerMapping, row, MetaboExperimentProperties.Dissociation_Method.label);
  }

  private void addValueForCategory(Map<String, Integer> headerMapping, String[] row, String cat) {
    if (headerMapping.containsKey(cat)) {
      String val = row[headerMapping.get(cat)];
      if (val != null && !val.isEmpty()) {
        for (String v : val.split(LIST_SEPARATOR)) {
          if (parsedCategoriesToValues.containsKey(cat)) {
            parsedCategoriesToValues.get(cat).add(v);
          } else {
            Set<String> set = new HashSet<>();
            set.add(v);
            parsedCategoriesToValues.put(cat, set);
          }
        }
      }
    }
  }

  public Map<ExperimentType, List<Map<String, Object>>> getExperimentInfos() {
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
    tsvByRows = new ArrayList<>();
    parsedCategoriesToValues = new HashMap<>();

    BufferedReader reader = new BufferedReader(new FileReader(file));
    ArrayList<String[]> data = new ArrayList<>();
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

    ArrayList<String> found = new ArrayList<>(Arrays.asList(header));
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
      } else if (headerNamesToConditions.containsKey(header[i])
          || header[i].toLowerCase().contains("condition:")) {
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
    Set<Integer> entityFactors = new HashSet<>();
    Set<Integer> extractFactors = new HashSet<>();
    for (int col : factors) {
      Map<String, String> idToVal = new HashMap<>();
      boolean ent = true;
      boolean extr = true;
      for (String[] row : data) {
        String val = row[col];
        String sourceID = row[headerMapping.get(MetaboExperimentProperties.Species.label)];
        String organismID = row[headerMapping.get(MetaboExperimentProperties.Organism_ID.label)];
        sourceID += " " + organismID;
        String extractID = sourceID + row[headerMapping.get(MetaboExperimentProperties.Biospecimen.label)];
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
    Map<ExtendedMSProperties, String> msPropertiesToID = new HashMap<>();
    Map<CultureProperties, String> culturePropertiesToID = new HashMap<>();
    Map<MetabolitePrepProperties, String> metaboPrepPropertiesToID = new HashMap<>();

    speciesSet = new HashSet<>();
    tissueSet = new HashSet<>();
    analyteSet = new HashSet<>();
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
        String species = row[headerMapping.get(MetaboExperimentProperties.Species.label)];
        String sourceID = species;
        String organismID = row[headerMapping.get(MetaboExperimentProperties.Organism_ID.label)];
        sourceID += " " + organismID;

        String expressionSystem = null;
        // if (headerMapping.containsKey("Expression System")) {
        // expressionSystem = row[headerMapping.get("Expression System")];
        // }

        String sampleKey = row[headerMapping.get(SAMPLE_KEYWORD)];
        // String sampleAltName = row[headerMapping.get(SAMPLE_ALTNAME_KEYWORD)];

        String cultureMedium = row[headerMapping.get(MetaboExperimentProperties.Medium.label)];
        String harvestingMethod = row[headerMapping.get(MetaboExperimentProperties.Harvesting_Method.label)];
        String harvestingVolume = row[headerMapping.get(MetaboExperimentProperties.Harvesting_Volume.label)];
        String washingSolvent = row[headerMapping.get(MetaboExperimentProperties.Washing_Solvent.label)];
        String sampleSolvent = row[headerMapping.get(MetaboExperimentProperties.Sample_Solvent.label)];
        String biospecimen = row[headerMapping.get(MetaboExperimentProperties.Biospecimen.label)];
        String cellLysis = row[headerMapping.get(MetaboExperimentProperties.Cell_Lysis.label)];
        String lysisParams = row[headerMapping.get(MetaboExperimentProperties.Lysis_Parameters.label)];
        String strainCollNumber = row[headerMapping.get(MetaboExperimentProperties.Strain_Lab_Collection_Number.label)];

        List<String> lysisList = new ArrayList<>();
        if (!cellLysis.isEmpty()) {
          lysisList = new ArrayList<String>(Arrays.asList(cellLysis.split(LIST_SEPARATOR)));
        }

        speciesSet.add(species);
        tissueSet.add(biospecimen);

        // always one new measurement per row
        sampleID++;
        TSVSampleBean msRun = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_MS_RUN, "",
            fillMetadata(header, row, meta, factors, new ArrayList<>(), SampleType.Q_MS_RUN));
        msRun.addProperty("File", sampleKey);// TODO? file name
        msRun.addProperty(OpenbisPropertyCodes.Q_SAMPLE_SOLVENT.name(), sampleSolvent);

        String lcmsMethod = row[headerMapping.get(MetaboExperimentProperties.LCMS_Method_Name.label)];
        String msDevice = row[headerMapping.get(MetaboExperimentProperties.MS_Device.label)];
        String column = row[headerMapping.get(MetaboExperimentProperties.LC_Column_Name.label)];
        String ionMode = row[headerMapping.get(MetaboExperimentProperties.MS_Ion_Mode.label)];
        String sopRefCode = row[headerMapping.get(MetaboExperimentProperties.Technical_Comments.label)];
        String lcDevice = row[headerMapping.get(MetaboExperimentProperties.LC_Device.label)];
        String lcDetectionMethod = row[headerMapping.get(MetaboExperimentProperties.LC_Detection_Method.label)];// TODO do not put
                                                                                 // this into ms
                                                                                 // props
        String dissociationMethod = row[headerMapping.get(MetaboExperimentProperties.Dissociation_Method.label)];
        double dissociationEnergy = -1;
        String energyString = row[headerMapping.get(MetaboExperimentProperties.Dissociation_Energy.label)];
        String massResPower = row[headerMapping.get(MetaboExperimentProperties.Mass_Resolving_Power.label)];

        if (energyString != null && !energyString.isEmpty()) {
          try {
            dissociationEnergy = Double.parseDouble(energyString);
          } catch (NumberFormatException e) {
            error = MetaboExperimentProperties.Dissociation_Energy.label+": " + energyString + " is not a number.";
            return null;
          }
        }

        ExtendedMSProperties msProperties = new ExtendedMSProperties(lcmsMethod, msDevice);
        msProperties.setLCDetectionMethod(lcDetectionMethod);
        msProperties.setLCDevice(lcDevice);
        msProperties.setIonizationMode(ionMode);
        msProperties.setColumnName(column);
        if (dissociationEnergy > 0) {
          msProperties.setDissociationEnergy(dissociationEnergy);
        }
        msProperties.setDissociationMethod(dissociationMethod);
        msProperties.setMassResolvingPower(massResPower);
        if (sopRefCode != null && !sopRefCode.isEmpty()) {
          msProperties.setAdditionalInformation("SOP reference code: " + sopRefCode);
        }
        msProperties.setWashingSolvent(washingSolvent);
        String expID = Integer.toString(msProperties.hashCode());
        msPropertiesToID.put(msProperties, expID);
        msRun.setExperiment(expID);

        samplesInOrder.get(MetaboSampleHierarchy.MassSpecRun).add(msRun);

        // if organism id not known => create organism entity. put in map.
        TSVSampleBean sampleSource = sourceIDToSample.get(sourceID);
        if (sampleSource == null) {
          sampleID++;

          sampleSource = new TSVSampleBean(Integer.toString(sampleID),
              SampleType.Q_BIOLOGICAL_ENTITY, sourceID,
              fillMetadata(header, row, meta, entityFactors, loci, SampleType.Q_BIOLOGICAL_ENTITY));
          sampleSource.addProperty(OpenbisPropertyCodes.Q_STRAIN_LAB_COLLECTION_NUMBER.name(), strainCollNumber);
          // sampleSource.addProperty("Q_EXTERNALDB_ID", sourceID);
          samplesInOrder.get(MetaboSampleHierarchy.Organism).add(sampleSource);
          sourceIDToSample.put(sourceID, sampleSource);

          CultureProperties props = new CultureProperties(cultureMedium);
          String cultExpID = Integer.toString(props.hashCode());
          culturePropertiesToID.put(props, cultExpID);
          sampleSource.setExperiment(cultExpID);

          if (expressionSystem != null) {
            speciesSet.add(expressionSystem);
            sampleSource.addProperty(OpenbisPropertyCodes.Q_EXPRESSION_SYSTEM.name(), expressionSystem);
          }
        }
        // we don't have tissue ids, so we build unique identifiers by adding sourceID and tissue
        // name - biospecimen is used here
        String tissueID = sourceID + "-" + biospecimen;
        TSVSampleBean tissueSample = tissueToSample.get(tissueID);
        if (tissueSample == null) {
          sampleID++;

          tissueSample = new TSVSampleBean(Integer.toString(sampleID),
              SampleType.Q_BIOLOGICAL_SAMPLE, tissueID, fillMetadata(header, row, meta,
                  extractFactors, loci, SampleType.Q_BIOLOGICAL_SAMPLE));
          // tissueSample.addProperty("Q_PRIMARY_TISSUE", "Whole organism");
          samplesInOrder.get(MetaboSampleHierarchy.Tissue).add(tissueSample);
          tissueSample.addParentID(sampleSource.getCode());
          tissueSample.addProperty(OpenbisPropertyCodes.Q_EXTERNALDB_ID.name(), tissueID);
          tissueToSample.put(tissueID, tissueSample);

          MetabolitePrepProperties props = new MetabolitePrepProperties(harvestingMethod,
              harvestingVolume, lysisList, lysisParams);

          String prepExpID = Integer.toString(props.hashCode());
          metaboPrepPropertiesToID.put(props, prepExpID);
          tissueSample.setExperiment(prepExpID);
        }

        // if sample secondary name not known => create metabolite sample
        String measureID = row[headerMapping.get(SAMPLE_KEYWORD)];
        TSVSampleBean metabolite = metaboliteToSample.get(measureID);
        if (metabolite == null) {
          sampleID++;

          metabolite = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE,
              measureID, fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
          samplesInOrder.get(MetaboSampleHierarchy.Molecules).add(metabolite);
          metabolite.addParentID(tissueSample.getCode());
          metabolite.addProperty(OpenbisPropertyCodes.Q_EXTERNALDB_ID.name(), measureID);
          metaboliteToSample.put(measureID, metabolite);
          metabolite.addProperty(OpenbisPropertyCodes.Q_SAMPLE_TYPE.name(), "SMALLMOLECULES");
        }
        msRun.addParentID(metabolite.getCode());
      }
    }
    experimentInfos = new HashMap<>();

    // Cell cultures
    List<Map<String, Object>> cultures = new ArrayList<>();
    for (CultureProperties props : culturePropertiesToID.keySet()) {
      Map<String, Object> propMap = props.getPropertyMap();
      propMap.put("Code", Integer.toString(props.hashCode()));// used to match samples to their
      // experiments later
      cultures.add(propMap);
    }
    experimentInfos.put(ExperimentType.Q_EXPERIMENTAL_DESIGN, cultures);

    // Extract preparation experiments
    List<Map<String, Object>> extractPreparations = new ArrayList<>();
    for (MetabolitePrepProperties props : metaboPrepPropertiesToID.keySet()) {
      Map<String, Object> propMap = props.getPropertyMap();
      propMap.put("Code", Integer.toString(props.hashCode()));// used to match samples to their
      // experiments later
      extractPreparations.add(propMap);
    }
    experimentInfos.put(ExperimentType.Q_SAMPLE_EXTRACTION, extractPreparations);

    // MS experiments
    List<Map<String, Object>> msExperiments = new ArrayList<>();
    for (ExtendedMSProperties expProperties : msPropertiesToID.keySet()) {
      Map<String, Object> propMap = expProperties.getPropertyMap();
      propMap.put("Code", msPropertiesToID.get(expProperties));
      msExperiments.add(propMap);
    }
    experimentInfos.put(ExperimentType.Q_MS_MEASUREMENT, msExperiments);
    for (MetaboSampleHierarchy level : order) {
      beans.addAll(samplesInOrder.get(level));
      // printSampleLevel(samplesInOrder.get(level));
    }
    return beans;
  }

  private String tryReplacePredefinedConditionNames(String condition) {
    if (headerNamesToConditions.containsKey(condition)) {
      return headerNamesToConditions.get(condition);
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

  public Set<String> getSpeciesSet() {
    return speciesSet;
  }

  public Set<String> getTissueSet() {
    return tissueSet;
  }

  private boolean checkUniqueIDsBetweenSets(Set<String> speciesSet, Set<String> tissueSet) {
    Set<String> intersection1 = new HashSet<>(speciesSet);
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
    HashMap<String, Object> res = new HashMap<>();
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
    String label = tryReplacePredefinedConditionNames(colName);
    if (label.contains(": ")) {
      label = label.split(": ")[1];
    }
    return label;
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
    return new HashSet<String>(Arrays.asList("SMALLMOLECULES"));
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
    res.add(new TechnologyType("Metabolomics"));
    return res;
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
