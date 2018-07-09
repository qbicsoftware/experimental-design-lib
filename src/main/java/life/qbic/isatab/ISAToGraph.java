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
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.isatools.errorreporter.model.ErrorMessage;
import org.isatools.errorreporter.model.ISAFileErrorReport;
import org.isatools.isacreator.io.importisa.ISAtabFilesImporter;
import org.isatools.isacreator.model.Assay;
import org.isatools.isacreator.model.Factor;
import org.isatools.isacreator.model.Investigation;
import org.isatools.isacreator.model.Study;
import org.isatools.isacreator.settings.ISAcreatorProperties;
import org.isatools.isacreator.utils.PropertyFileIO;

import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleSummary;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.ParserHelpers;
import life.qbic.expdesign.model.StructuredExperiment;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;
import life.qbic.xml.properties.Unit;

public class ISAToGraph {

  public static final Path[] DEFAULT_CONFIG_PATHS = {
    Paths.get("Configurations", "isaconfig-default_v2015-07-02"),
    Paths.get("src", "main", "resources", "Configurations", "isaconfig-default_v2015-07-02")};
  private static Logger log = Logger.getLogger(ISAToGraph.class);

  private ISAtabFilesImporter importer = null;
  private String isatabParentDir = null;
  private HashMap<String, Set<SampleSummary>> nodesForFactorPerLabel;
  private Map<Study, StructuredExperiment> graphsByStudy;

  private String getAnalyteFromMeasureEndpoint(String technologyType) {
    return KeywordTranslator.getQBiCKeyword(technologyType);
  }

  public Map<Study, StructuredExperiment> getGraphsByStudy() {
    return graphsByStudy;
  }

  public void read(File file) {
//    ISAcreatorProperties.setProperties(PropertyFileIO.DEFAULT_CONFIGS_SETTINGS_PROPERTIES);
    final String configDir = resolveConfigurationFilesPath();

    log.debug("configDir=" + configDir);
    importer = new ISAtabFilesImporter(configDir);
    isatabParentDir = file.toString();
    log.debug("isatabParentDir=" + isatabParentDir);

    importer.importFile(isatabParentDir);
    Investigation inv = importer.getInvestigation();

    for (ISAFileErrorReport report : importer.getMessages()) {
      // System.out.println(report.getFileName());
      System.out.println("---ERRORS---");
      for (ErrorMessage message : report.getMessages()) {
        System.out.println(message.getErrorLevel().toString() + " > " + message.getMessage());
      }
    }

    // TODO: list each study (by name), return graph for selected study
    graphsByStudy = new HashMap<Study, StructuredExperiment>();
    for (String std : inv.getStudies().keySet()) {
      // for all samples in this study, first collect Source Name, Sample Name, Factors, Organism
      // and Tissue
      Study study = inv.getStudies().get(std);

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
        // String colLabel = "Factor Value[" + label + "]";
        // label = factorNameForXML(label, validateForDataModel); needed to register experiment at
        // qbic
        nodesForFactorPerLabel.put(label, new LinkedHashSet<SampleSummary>());
      }
      nodesForFactorPerLabel.put("None", new LinkedHashSet<SampleSummary>());

      Object[][] matrix = study.getStudySampleDataMatrix();
      for (int rowID = 1; rowID < matrix.length; rowID++) {
        String organism = "unspecified species";
        if (organismCol != -1)
          organism = removeOntologyPrefix((String) matrix[rowID][organismCol]);
        String tissue = "unspecified organ";
        if (organCol != -1)
          tissue = removeOntologyPrefix((String) matrix[rowID][organCol]);
        String sourceID = (String) matrix[rowID][sourceCol];
        String sampleID = (String) matrix[rowID][sampleCol];
        List<Property> factors = new ArrayList<Property>();
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
                System.err.println(e);
                factorVal += " " + unitBlock;
                unit = Unit.Arbitrary_Unit;
              } finally {
                factor = new Property(factorLabel, factorVal, unit, PropertyType.Factor);
              }
            } else {
              factor = new Property(factorLabel, factorVal, PropertyType.Factor);
            }
            factors.add(factor);
          }
        }
        // these will be filled below, if they don't exist
        TSVSampleBean eSample = sampleIDToSample.get(sampleID);
        TSVSampleBean sSample = sourceIDToSample.get(sourceID);
        if (!sampleIDToSample.containsKey(sampleID)) {
          Map<String, Object> metadata = new HashMap<String, Object>();
          metadata.put("Factors", factors);
          eSample = new TSVSampleBean(sampleID, "Q_BIOLOGICAL_SAMPLE", sampleID, metadata);
          eSample.addProperty("Q_PRIMARY_TISSUE", tissue);
          sampleIDToSample.put(sampleID, eSample);
          eSample.addParentID(sourceID);

        }
        if (!sourceIDToSample.containsKey(sourceID)) {
          // sampleID++;
          Map<String, Object> metadata = new HashMap<String, Object>();
          metadata.put("Factors", new ArrayList<Property>());
          sSample = new TSVSampleBean(sourceID, "Q_BIOLOGICAL_ENTITY", sourceID, metadata);
          sSample.addProperty("Q_NCBI_ORGANISM", organism);
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
          if(assayExtractIDCol !=-1) {
            extractID = extractID+"-"+(String) assayMatrix[rowID][assayExtractIDCol];
          }
          TSVSampleBean eSample = sampleIDToSample.get(sampleID);
          Map<String, Object> metadata = eSample.getMetadata();
//          metadata.put("Factors", new ArrayList<Property>());
          TSVSampleBean tSample = new TSVSampleBean(extractID, "Q_TEST_SAMPLE", extractID, metadata);
          
          tSample.addProperty("Q_SAMPLE_TYPE", analyte);
          analyteIDToSample.put(extractID, tSample);
          tSample.addParentID(sampleID);
                    
          List<TSVSampleBean> sampleRow = new ArrayList<TSVSampleBean>(
              Arrays.asList(sourceIDToSample.get(eSample.getParentIDs().get(0)), sampleIDToSample.get(sampleID), tSample));
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
      graphsByStudy.put(study, new StructuredExperiment(nodeListsPerLabel));
    }
  }

  private String resolveConfigurationFilesPath() {
    for (int i = 0; i < DEFAULT_CONFIG_PATHS.length; i++) {
      final File possibleConfigFolder = DEFAULT_CONFIG_PATHS[i].toFile();
      if (possibleConfigFolder.exists() && possibleConfigFolder.isDirectory()) {
        return DEFAULT_CONFIG_PATHS[i].toString();
      } else {
        log.info(String.format("Configuration files not found in folder %s", DEFAULT_CONFIG_PATHS[i].toString()));
      }
    }
    // TODO: change for ApplicationException, one of QBiC's "generic" runtime exceptions
    throw new RuntimeException("Required configuration files were not found at any of the default folders.");
  }

  private String factorNameForXML(String label, boolean validate) {
    Pattern p = Pattern.compile("([a-z]+_?[a-z]*)+([a-z]|[0-9]*)");
    if (!validate || p.matcher(label).matches())
      return label;

    label = label.trim();
    label = label.replace(" ", "_");
    char first = label.charAt(0);
    if (Character.isDigit(first))
      label = label.replaceFirst(Character.toString(first), "factor_" + first);
    return label;
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
    nodeID *= levels.size();
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
}
