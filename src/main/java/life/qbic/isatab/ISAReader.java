package life.qbic.isatab;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.isatools.errorreporter.model.ErrorMessage;
import org.isatools.errorreporter.model.ISAFileErrorReport;
import org.isatools.isacreator.io.importisa.ISAtabFilesImporter;
import org.isatools.isacreator.model.Assay;
import org.isatools.isacreator.model.Factor;
import org.isatools.isacreator.model.Investigation;
import org.isatools.isacreator.model.Study;

import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleSummary;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.io.IExperimentalDesignReader;
import life.qbic.expdesign.model.StructuredExperiment;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;
import life.qbic.xml.properties.Unit;
import life.qbic.xml.study.TechnologyType;

public class ISAReader implements IExperimentalDesignReader {

  public static final Path[] DEFAULT_CONFIG_PATHS =
      {Paths.get("Configurations", "isaconfig-default_v2015-07-02"),
          Paths.get("src", "main", "resources", "Configurations", "isaconfig-default_v2015-07-02")};
  private static Logger log = Logger.getLogger(ISAReader.class);

  private ISAtabFilesImporter importer = null;
  private String isatabParentDir = null;
  private HashMap<String, Set<SampleSummary>> nodesForFactorPerLabel;
  private List<StructuredExperiment> graphsByStudy;
  private StructuredExperiment currentGraphStructure;
  private List<TechnologyType> technologyTypes;
  private Investigation investigation;
  private String selectedStudy;
  private List<String> datasetTSV;
  private Set<String> speciesSet;
  private Set<String> tissueSet;
  private Set<String> analyteSet;
  private String error;
  private String CONFIG_PATH;
  private IKeywordToInterfaceTextMapper mapper;

  /**
   * Creates a new Reader using the Config at the given path
   * 
   * @param ISAConfigPath Path to the ISA config
   */
  public ISAReader(String ISAConfigPath, IKeywordToInterfaceTextMapper mapper) {
    CONFIG_PATH = ISAConfigPath;
    this.mapper = mapper;
  }

  /**
   * Creates a new Reader, expects ISA config in one of the default paths
   */
  public ISAReader(IKeywordToInterfaceTextMapper mapper) {
    this.mapper = mapper;
  }

  private String getAnalyteFromMeasureEndpoint(String technologyType) {
    return mapper.translate(technologyType);
  }

  public List<StructuredExperiment> getGraphsByStudy() {
    return graphsByStudy;
  }

  public StructuredExperiment getGraphStructure() {
    return currentGraphStructure;
  }

  public void selectStudyToParse(String study) {
    this.selectedStudy = study;
  }

  public static void main(String[] args) throws IOException {
    ISAReader i = new ISAReader(new ISAToQBIC());
    File test = new File("/Users/frieda/Downloads/MTBLS620_diabetes/");
    i.createAllGraphs(test);

    i = new ISAReader(new ISAToReadable());
    i.createAllGraphs(test);
    System.out.println(i.getGraphsByStudy());
  }

  public List<Study> listStudies(File file) {
    error = null;
    final String configDir = resolveConfigurationFilesPath();
    importer = new ISAtabFilesImporter(configDir);
    isatabParentDir = file.toString();
    try {
      importer.importFile(isatabParentDir);
      investigation = importer.getInvestigation();
    } catch (NullPointerException e) {
      error = "Investigation file not found or not the right format.";
    }

    for (ISAFileErrorReport report : importer.getMessages()) {
      for (ErrorMessage message : report.getMessages()) {
        error = mapError(message.getMessage());
        log.error(message.getErrorLevel().toString() + " > " + message.getMessage());
      }
    }
    List<Study> res = new ArrayList<Study>();
    if (error == null)
      res.addAll(investigation.getStudies().values());
    return res;
  }

  private String mapError(String message) {
    String res = message;
    if (message.startsWith("Investigation file does not exist"))
      res = "No Investigation file could be found.";
    if (message.contains(
        "Please ensure that the file exists within the folder and that the name referred to in the investigation file is correct!")) {
      String file = message.split(" was not found")[0];
      if (file.contains("/")) {
        String[] splt = file.split("/");
        file = splt[splt.length - 1];
      } else {
        file = file.split("The file ")[1];
      }
      res = file
          + " not found. Please ensure that the file exists within the folder and that the name referred to in the investigation file is correct.";
    }
    return res;
  }

  public void createAllGraphs(File file) {
    final String configDir = resolveConfigurationFilesPath();

    log.debug("configDir=" + configDir);
    importer = new ISAtabFilesImporter(configDir);
    isatabParentDir = file.toString();
    log.debug("isatabParentDir=" + isatabParentDir);

    importer.importFile(isatabParentDir);
    investigation = importer.getInvestigation();

    for (ISAFileErrorReport report : importer.getMessages()) {
      // System.out.println(report.getFileName());
      for (ErrorMessage message : report.getMessages()) {
        log.error(message.getErrorLevel().toString() + " > " + message.getMessage());
      }
    }

    // TODO: list each study (by name), return graph for selected study
    graphsByStudy = new ArrayList<StructuredExperiment>();
    for (String std : investigation.getStudies().keySet()) {
      // for all samples in this study, first collect Source Name, Sample Name, Factors, Organism
      // and Tissue
      Study study = investigation.getStudies().get(std);

      nodesForFactorPerLabel = new HashMap<>();
      Map<String, TSVSampleBean> sourceIDToSample = new HashMap<>();
      Map<String, TSVSampleBean> sampleIDToSample = new HashMap<>();
      Map<String, TSVSampleBean> analyteIDToSample = new HashMap<>();
      int organismCol = findStudyColumnID(study, "Characteristics[Organism]");
      int organCol = findStudyColumnID(study, "Characteristics[Organism part]");
      int sourceCol = findStudyColumnID(study, "Source Name");
      int sampleCol = findStudyColumnID(study, "Sample Name");

      for (Factor factor : study.getFactors()) {
        String label = factor.getFactorName();
        nodesForFactorPerLabel.put(label, new LinkedHashSet<>());
      }
      nodesForFactorPerLabel.put("None", new LinkedHashSet<>());
      Set<String> sourceFactorLabels = getFactorsForSources(study);

      Object[][] matrix = study.getStudySampleDataMatrix();
      for (int rowID = 1; rowID < matrix.length; rowID++) {
        String organism = "unspecified species";
        if (organismCol != -1)
          organism = removeOntologyPrefix((String) matrix[rowID][organismCol]);
        String tissue = "unspecified organ";
        if (organCol != -1)
          tissue = removeOntologyPrefix((String) matrix[rowID][organCol]);
        organism = mapper.translate(organism);
        tissue = mapper.translate(tissue);
        String sourceID = (String) matrix[rowID][sourceCol];
        String sampleID = (String) matrix[rowID][sampleCol];
        List<Property> factors = new ArrayList<>();
        List<Property> sourceFactors = new ArrayList<>();
        Set<String> unknownUnits = new HashSet<>();
        for (String factorLabel : nodesForFactorPerLabel.keySet()) {
          String colLabel = "Factor Value[" + factorLabel + "]";
          int factorCol = findStudyColumnID(study, colLabel);
          if (factorCol != -1) {
            Property factor = null;
            String factorVal = (String) matrix[rowID][factorCol];
            if (studyFactorHasUnit(study, colLabel)) {
              String unitBlock = (String) matrix[rowID][factorCol + 1];
              Unit unit = null;
              try {
                unitBlock = removeOntologyPrefix(unitBlock);
                unit = Unit.fromString(unitBlock);
              } catch (IllegalArgumentException e) {
                unknownUnits.add(e.toString());
                factorVal += " " + unitBlock;
                unit = Unit.Arbitrary_Unit;
              } finally {
                factor = new Property(factorLabel, factorVal, unit, PropertyType.Factor);
              }
            } else {
              factor = new Property(factorLabel, factorVal, PropertyType.Factor);
            }
            factors.add(factor);
            if (sourceFactorLabels.contains(factorLabel)) {
              sourceFactors.add(factor);
            }
          }
        }
        if (!unknownUnits.isEmpty()) {
          log.warn(unknownUnits);
          log.warn("Units have been replaced by \"Arbitrary Unit\"");
        }
        // these will be filled below, if they don't exist
        TSVSampleBean eSample = sampleIDToSample.get(sampleID);
        TSVSampleBean sSample = sourceIDToSample.get(sourceID);
        if (!sampleIDToSample.containsKey(sampleID)) {
          Map<String, Object> metadata = new HashMap<String, Object>();
          metadata.put("Factors", factors);
          eSample = new TSVSampleBean(sampleID, "Q_BIOLOGICAL_SAMPLE", sampleID, metadata);
          eSample.addProperty("Q_PRIMARY_TISSUE", tissue);
          eSample.addProperty("Q_EXTERNALDB_ID", sampleID);
          sampleIDToSample.put(sampleID, eSample);
          eSample.addParentID(sourceID);

        }
        if (!sourceIDToSample.containsKey(sourceID)) {
          // sampleID++;
          Map<String, Object> metadata = new HashMap<String, Object>();
          metadata.put("Factors", sourceFactors);
          sSample = new TSVSampleBean(sourceID, "Q_BIOLOGICAL_ENTITY", sourceID, metadata);
          sSample.addProperty("Q_NCBI_ORGANISM", organism);
          sSample.addProperty("Q_EXTERNALDB_ID", sourceID);
          sourceIDToSample.put(sourceID, sSample);
        }
      }

      // for each assay find entities by Names and connect them via Sample Names to existing tissue
      // samples
      int unknownExtractID = 0;
      int uniqueEntityID = 0;
      for (String ass : study.getAssays().keySet()) {
        Assay assay = study.getAssays().get(ass);
        int assaySampleIDCol = findAssayColumnID(assay, "Sample Name");
        int assayExtractIDCol = findAssayColumnID(assay, "Extract Name");
        // Analyte
        String endpoint = assay.getMeasurementEndpoint();
        String analyte = getAnalyteFromMeasureEndpoint(endpoint);

        Object[][] assayMatrix = assay.getAssayDataMatrix();
        for (int rowID = 1; rowID < assayMatrix.length; rowID++) {
          String sampleID = (String) assayMatrix[rowID][assaySampleIDCol];
          unknownExtractID++;
          String extractID = Integer.toString(unknownExtractID);
          if (assayExtractIDCol != -1) {
            extractID = extractID + "-" + (String) assayMatrix[rowID][assayExtractIDCol];
          }
          TSVSampleBean eSample = sampleIDToSample.get(sampleID);
          Map<String, Object> metadata = new HashMap<String, Object>();
          metadata.put("Factors", eSample.getMetadata().get("Factors"));
          TSVSampleBean tSample =
              new TSVSampleBean(extractID, "Q_TEST_SAMPLE", extractID, metadata);

          tSample.addProperty("Q_SAMPLE_TYPE", analyte);
          tSample.addProperty("Q_EXTERNALDB_ID", extractID);
          analyteIDToSample.put(extractID, tSample);
          tSample.addParentID(sampleID);

          List<TSVSampleBean> sampleRow = new ArrayList<TSVSampleBean>(
              Arrays.asList(sourceIDToSample.get(eSample.getParentIDs().get(0)),
                  sampleIDToSample.get(sampleID), tSample));
          // if (analytesIncluded)
          // sampleRow.add(analyteIDToSample.get(analyteID));
          uniqueEntityID++;
          createGraphSummariesForRow(sampleRow, new Integer(uniqueEntityID));
        }
      }

      Map<String, List<SampleSummary>> nodeListsPerLabel =
          new HashMap<String, List<SampleSummary>>();
      for (String label : nodesForFactorPerLabel.keySet()) {
        nodeListsPerLabel.put(label,
            new ArrayList<SampleSummary>(nodesForFactorPerLabel.get(label)));
      }
      graphsByStudy.add(new StructuredExperiment(nodeListsPerLabel, study));
    }
  }

  private Set<String> getFactorsForSources(Study study) {
    Set<String> res = new HashSet<>();
    for (Factor f : study.getFactors()) {
      Map<String, String> sourcesToValue = new HashMap<>();
      String label = f.getFactorName();
      res.add(label);
      int sourceCol = findStudyColumnID(study, "Source Name");
      Object[][] matrix = study.getStudySampleDataMatrix();
      for (int rowID = 1; rowID < matrix.length; rowID++) {
        String sourceID = (String) matrix[rowID][sourceCol];
        String colLabel = "Factor Value[" + label + "]";
        int factorCol = findStudyColumnID(study, colLabel);
        if (factorCol != -1) {
          String factorVal = (String) matrix[rowID][factorCol];
          String entry = sourcesToValue.get(sourceID);
          // not source factor, as there are different entries
          if (entry != null && !entry.equals(factorVal)) {
            res.remove(label);
            break;
          } else {
            sourcesToValue.put(sourceID, factorVal);
          }
        }
      }
    }
    return res;
  }

  private String resolveConfigurationFilesPath() {
    if (CONFIG_PATH != null)
      return CONFIG_PATH;
    for (int i = 0; i < DEFAULT_CONFIG_PATHS.length; i++) {
      final File possibleConfigFolder = DEFAULT_CONFIG_PATHS[i].toFile();
      if (possibleConfigFolder.exists() && possibleConfigFolder.isDirectory()) {
        return DEFAULT_CONFIG_PATHS[i].toString();
      } else {
        log.info(String.format("Configuration files not found in folder %s",
            DEFAULT_CONFIG_PATHS[i].toString()));
      }
    }
    // TODO: change for ApplicationException, one of QBiC's "generic" runtime exceptions
    throw new RuntimeException(
        "Required configuration files were not found at any of the default folders.");
  }

  private int findStudyColumnID(Study study, String label) {
    Object[][] matrix = study.getStudySampleDataMatrix();
    for (int j = 0; j < matrix[0].length; j++) {
      String colName = (String) matrix[0][j];
      if (colName.toLowerCase().equals(label.toLowerCase()))
        return j;
    }
    return -1;
  }

  private int findAssayColumnID(Assay assay, String label) {
    Object[][] matrix = assay.getAssayDataMatrix();
    for (int j = 0; j < matrix[0].length; j++) {
      String colName = (String) matrix[0][j];
      if (colName.toLowerCase().equals(label.toLowerCase()))
        return j;
    }
    return -1;
  }

  private boolean studyFactorHasUnit(Study study, String label) {
    Object[][] matrix = study.getStudySampleDataMatrix();
    int colLabel = findStudyColumnID(study, label) + 1;
    if (matrix[0].length <= colLabel)
      return false;
    return matrix[0][colLabel].equals("Unit");
  }

  private String removeOntologyPrefix(String value) {
    try {
      String res = value.split(":")[1];
      return res;
    } catch (IndexOutOfBoundsException e) {
      return value;
    }
  }

  private void createGraphSummariesForRow(List<TSVSampleBean> levels, int nodeID) {
    // nodeID *= levels.size();
    // create summary for this each node based on each experimental factor as well as "none"
    for (String label : nodesForFactorPerLabel.keySet()) {
      SampleSummary currentSummary = null;
      // source - extract - analyte (at least the first must exist) - create summaries each
      for (int level = 0; level < levels.size(); level++) {
        TSVSampleBean s = levels.get(level);
        // find out if this sample has children or not
        int next = level + 1;
        boolean leaf = levels.size() == next || levels.get(next) == null;
        // sample on this level does exist
        if (s != null) {
          nodeID = nodeID * next + 1;
          Set<SampleSummary> parentSummaries = new LinkedHashSet<SampleSummary>();
          if (currentSummary != null)
            parentSummaries.add(currentSummary);
          currentSummary = createNodeSummary(s, parentSummaries, label, nodeID, leaf);
          // check for hashcode and add current sample s if node exists and doesn't contain code yet
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

  // new "sample to bucket" function, creates new summaries from sample metadata in reference to
  // parent summaries and experimental factor
  private SampleSummary createNodeSummary(TSVSampleBean s, Set<SampleSummary> parents, String label,
      int currentID, boolean leaf) {
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

    Property factor = getFactorOfSampleOrNull((List<Property>) props.get("Factors"), label);
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
        if (value.isEmpty())
          value = "unspecified source";
        break;
      case "Q_BIOLOGICAL_SAMPLE":
        source = (String) props.get("Q_PRIMARY_TISSUE");
        if (!newFactor || source.equals(value)) {
          value = source;
        } else {
          value = source + " " + value;
        }
        if (value.isEmpty())
          value = "unspecified extract";
        break;
      case "Q_TEST_SAMPLE":
        source = (String) props.get("Q_SAMPLE_TYPE");
        value = source + " " + value;
        break;
    }
    return new SampleSummary(currentID, parentIDs, new ArrayList<ISampleBean>(Arrays.asList(s)),
        factor.getValue(), tryShortenName(value, s).trim(), type, leaf);
  }

  private Property getFactorOfSampleOrNull(List<Property> factors, String label) {
    for (Property p : factors) {
      if (p.getLabel().equals(label))
        return p;
    }
    return null;
  }

  private String tryShortenName(String value, TSVSampleBean s) {
    // TODO Auto-generated method stub
    return value;
  }

  public Investigation getInvestigation() {
    return investigation;
  }

  @Override
  public List<ISampleBean> readSamples(File file, boolean parseGraph) throws IOException {
    log.debug("reading samples of selected study " + selectedStudy);
    List<ISampleBean> res = new ArrayList<ISampleBean>();
    speciesSet = new HashSet<String>();
    tissueSet = new HashSet<String>();
    analyteSet = new HashSet<String>();
    technologyTypes = new ArrayList<TechnologyType>();

    final String configDir = resolveConfigurationFilesPath();

    importer = new ISAtabFilesImporter(configDir);
    isatabParentDir = file.toString();

    importer.importFile(isatabParentDir);
    investigation = importer.getInvestigation();

    for (ISAFileErrorReport report : importer.getMessages()) {
      for (ErrorMessage message : report.getMessages()) {
        error = message.getMessage();
        log.error(message.getErrorLevel().toString() + " > " + message.getMessage());
      }
    }
    // for all samples in this study, first collect Source Name, Sample Name, Factors, Organism
    // and Tissue
    Study study = investigation.getStudies().get(selectedStudy);

    nodesForFactorPerLabel = new HashMap<String, Set<SampleSummary>>();
    Map<String, TSVSampleBean> sourceIDToSample = new HashMap<String, TSVSampleBean>();
    Map<String, TSVSampleBean> sampleIDToSample = new HashMap<String, TSVSampleBean>();
    Map<String, TSVSampleBean> analyteIDToSample = new HashMap<String, TSVSampleBean>();
    int organismCol = findStudyColumnID(study, "Characteristics[Organism]");
    int organCol = findStudyColumnID(study, "Characteristics[Organism part]");
    int sourceCol = findStudyColumnID(study, "Source Name");
    int sampleCol = findStudyColumnID(study, "Sample Name");

    for (Factor factor : study.getFactors()) {
      String label = factor.getFactorName();
      nodesForFactorPerLabel.put(label, new LinkedHashSet<SampleSummary>());
    }
    nodesForFactorPerLabel.put("None", new LinkedHashSet<SampleSummary>());
    Set<String> sourceFactorLabels = getFactorsForSources(study);

    Object[][] matrix = study.getStudySampleDataMatrix();
    for (int rowID = 1; rowID < matrix.length; rowID++) {
      String organism = "unspecified species";
      if (organismCol != -1)
        organism = removeOntologyPrefix((String) matrix[rowID][organismCol]);
      String tissue = "unspecified organ";
      if (organCol != -1)
        tissue = removeOntologyPrefix((String) matrix[rowID][organCol]);
      organism = mapper.translate(organism);
      tissue = mapper.translate(tissue);
      speciesSet.add(organism);
      tissueSet.add(tissue);
      String sourceID = (String) matrix[rowID][sourceCol];
      String sampleID = (String) matrix[rowID][sampleCol];
      List<Property> factors = new ArrayList<Property>();
      List<Property> sourceFactors = new ArrayList<Property>();
      Set<String> unknownUnits = new HashSet<String>();
      for (String factorLabel : nodesForFactorPerLabel.keySet()) {
        String colLabel = "Factor Value[" + factorLabel + "]";
        int factorCol = findStudyColumnID(study, colLabel);
        if (factorCol != -1) {
          Property factor = null;
          String factorVal = (String) matrix[rowID][factorCol];
          if (studyFactorHasUnit(study, colLabel)) {
            String unitBlock = (String) matrix[rowID][factorCol + 1];
            Unit unit = null;
            try {
              unitBlock = removeOntologyPrefix(unitBlock);
              unit = Unit.fromString(unitBlock);
            } catch (IllegalArgumentException e) {
              unknownUnits.add(e.toString());
              // TODO unknown units
              // factorVal += " " + unitBlock;
              unit = Unit.Arbitrary_Unit;
            } finally {
              factor = new Property(factorLabel, factorVal, unit, PropertyType.Factor);
            }
          } else {
            factor = new Property(factorLabel, factorVal, PropertyType.Factor);
          }
          factors.add(factor);
          if (sourceFactorLabels.contains(factorLabel)) {
            sourceFactors.add(factor);
          }
        }
      }
      if (!unknownUnits.isEmpty()) {
        log.warn(unknownUnits);
        log.warn("Units have been replaced by \"Arbitrary Unit\"");
      }
      // these will be filled below, if they don't exist
      TSVSampleBean eSample = sampleIDToSample.get(sampleID);
      TSVSampleBean sSample = sourceIDToSample.get(sourceID);
      if (!sampleIDToSample.containsKey(sampleID)) {
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("Factors", factors);
        eSample = new TSVSampleBean(sampleID, "Q_BIOLOGICAL_SAMPLE", sampleID, metadata);
        eSample.addProperty("Q_PRIMARY_TISSUE", tissue);
        eSample.addProperty("Q_EXTERNALDB_ID", sampleID);
        sampleIDToSample.put(sampleID, eSample);
        eSample.addParentID(sourceID);

      }
      if (!sourceIDToSample.containsKey(sourceID)) {
        // sampleID++;
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("Factors", sourceFactorLabels);
        sSample = new TSVSampleBean(sourceID, "Q_BIOLOGICAL_ENTITY", sourceID, metadata);
        sSample.addProperty("Q_NCBI_ORGANISM", organism);
        sSample.addProperty("Q_EXTERNALDB_ID", sourceID);
        sourceIDToSample.put(sourceID, sSample);
      }
    }

    // for each assay find entities by Names and connect them via Sample Names to existing tissue
    // samples
    int unknownExtractID = 0;
    int uniqueEntityID = 0;
    for (String ass : study.getAssays().keySet()) {
      Assay assay = study.getAssays().get(ass);
      int assaySampleIDCol = findAssayColumnID(assay, "Sample Name");
      int assayExtractIDCol = findAssayColumnID(assay, "Extract Name");
      // Analyte
      String endpoint = assay.getMeasurementEndpoint();
      technologyTypes.add(new TechnologyType(endpoint));
      String analyte = getAnalyteFromMeasureEndpoint(endpoint);
      analyteSet.add(analyte);

      Object[][] assayMatrix = assay.getAssayDataMatrix();
      for (int rowID = 1; rowID < assayMatrix.length; rowID++) {
        String sampleID = (String) assayMatrix[rowID][assaySampleIDCol];
        unknownExtractID++;
        String extractID = Integer.toString(unknownExtractID);
        if (assayExtractIDCol != -1) {
          extractID = extractID + "-" + (String) assayMatrix[rowID][assayExtractIDCol];
        }
        TSVSampleBean eSample = sampleIDToSample.get(sampleID);
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("Factors", eSample.getMetadata().get("Factors"));

        TSVSampleBean tSample = new TSVSampleBean(extractID, "Q_TEST_SAMPLE", extractID, metadata);

        tSample.addProperty("Q_SAMPLE_TYPE", analyte);
        tSample.addProperty("Q_EXTERNALDB_ID", extractID);
        analyteIDToSample.put(extractID, tSample);
        tSample.addParentID(sampleID);

        List<TSVSampleBean> sampleRow = new ArrayList<TSVSampleBean>(
            Arrays.asList(sourceIDToSample.get(eSample.getParentIDs().get(0)),
                sampleIDToSample.get(sampleID), tSample));
        // if (analytesIncluded)
        // sampleRow.add(analyteIDToSample.get(analyteID));
        uniqueEntityID++;
        createGraphSummariesForRow(sampleRow, new Integer(uniqueEntityID));
      }
    }

    Map<String, List<SampleSummary>> nodeListsPerLabel = new HashMap<String, List<SampleSummary>>();
    for (String label : nodesForFactorPerLabel.keySet()) {
      nodeListsPerLabel.put(label, new ArrayList<SampleSummary>(nodesForFactorPerLabel.get(label)));
    }
    currentGraphStructure = new StructuredExperiment(nodeListsPerLabel, study);

    res.addAll(sourceIDToSample.values());
    res.addAll(sampleIDToSample.values());
    res.addAll(analyteIDToSample.values());
    return res;
  }

  @Override
  public String getError() {
    return error;
  }

  @Override
  public Map<String, List<Map<String, Object>>> getExperimentInfos() {
    // TODO Auto-generated method stub
    return null;
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

  @Override
  public int countEntities(File file) throws IOException {
    // TODO needed?
    return -1;
  }

  @Override
  public List<String> getTSVByRows() {
    return datasetTSV;
  }

  @Override
  public List<TechnologyType> getTechnologyTypes() {
    return technologyTypes;
  }
}
