package life.qbic.isatab;

import java.io.File;
import java.io.IOException;
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
import org.isatools.isacreator.ontologymanager.OntologyManager;
import org.isatools.isacreator.ontologymanager.OntologySourceRefObject;
import org.isatools.isacreator.ontologymanager.common.OntologyTerm;
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

  private String configDir = null;
  public static final String DEFAULT_CONFIG_DIR =
      "/src/main/resources/Configurations/isaconfig-default_v2015-07-02/";
  private static Logger log = Logger.getLogger(ISAToGraph.class);

  private ISAtabFilesImporter importer = null;
  private String isatabParentDir = null;
  private HashMap<String, Set<SampleSummary>> nodesForFactorPerLabel;
  private Map<Study, StructuredExperiment> graphsByStudy;

  public static void main(String[] args) {
    ISAToGraph g = new ISAToGraph();
    g.read(true);
  }

  private String getAnalyteFromMeasureEndpoint(String technologyType) {
    return KeywordTranslator.getQBiCKeyword(technologyType);
  }

  public Map<Study, StructuredExperiment> getGraphsByStudy() {
    return graphsByStudy;
  }

  private void read(boolean validateForDataModel) {

    String baseDir = System.getProperty("basedir");

    if (baseDir == null) {
      try {
        baseDir = new File(".").getCanonicalPath();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    ISAcreatorProperties.setProperties(PropertyFileIO.DEFAULT_CONFIGS_SETTINGS_PROPERTIES);
    configDir = baseDir + DEFAULT_CONFIG_DIR;

    log.debug("configDir=" + configDir);
    importer = new ISAtabFilesImporter(configDir);
    isatabParentDir = "/Users/frieda/Downloads/isatab";
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

    // if import worked ok, there should not be error messages
    // System.out.println(importer.getMessages().size() + " errors");
    //
    // System.out.println("ontologies used=" + OntologyManager.getOntologiesUsed());
    // System.out
    // .println("ontology description=" + OntologyManager.getOntologyDescription("NCBITAXON"));
    // // System.out.println("ontology selection history=" +
    // // OntologyManager.getOntologySelectionHistory());
    // System.out.println("ontology selection history size=" +
    // OntologyManager.getOntologyTermsSize());

    for (String term : OntologyManager.getOntologyTermsKeySet()) {
      OntologyTerm ontologyTerm = OntologyManager.getOntologyTerm(term);
      OntologySourceRefObject ontologySourceRefObject = ontologyTerm.getOntologySourceInformation();
      // System.out.println("ontology term=" + ontologyTerm);
      // System.out.println("term URI = " + ontologyTerm.getOntologyTermURI());
      // System.out.println(ontologySourceRefObject);
      // System.out.println();
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
        String organism = removeOntologyPrefix((String) matrix[rowID][organismCol]);
        String tissue = removeOntologyPrefix((String) matrix[rowID][organCol]);
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
          sSample = new TSVSampleBean(sampleID, "Q_BIOLOGICAL_ENTITY", sampleID, metadata);
          sSample.addProperty("Q_NCBI_ORGANISM", organism);
          sourceIDToSample.put(sourceID, sSample);
        }
        List<TSVSampleBean> sampleRow = new ArrayList<TSVSampleBean>(
            Arrays.asList(sourceIDToSample.get(sourceID), sampleIDToSample.get(sampleID)));
        // if (analytesIncluded)
        // sampleRow.add(analyteIDToSample.get(analyteID));
        createGraphSummariesForRow(sampleRow, new Integer(rowID));
      }

      for (String ass : study.getAssays().keySet()) {
        // System.out.println(ass);
        Assay assay = study.getAssays().get(ass);
        // System.out.println(assay.getFieldKeysAsList());
        // System.out.println(assay.getFieldValues());
        // System.out.println();
        // String platform = assay.getAssayPlatform();
        // String tech = assay.getTechnologyType();
        // System.out.println(assay.getTechnologyTypeTermAccession());
        // System.out.println(tech);
        // System.out.println(platform);

        // Analyte
        String endpoint = assay.getMeasurementEndpoint();
        System.out.println(getAnalyteFromMeasureEndpoint(endpoint));
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
      if (colName.equals(label))
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
    ParserHelpers.fixXMLProps(props);

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
