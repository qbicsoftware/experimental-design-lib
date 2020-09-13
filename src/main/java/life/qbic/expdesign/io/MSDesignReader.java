package life.qbic.expdesign.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.datamodel.ms.LigandPrepRun;
import life.qbic.datamodel.ms.MSRunCollection;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.model.StructuredExperiment;
import life.qbic.xml.properties.Unit;
import life.qbic.xml.study.TechnologyType;

// private static final Logger logger = LogManager.getLogger(TopDownDesignReaderPCT.class);
//
// public TopDownDesignReaderPCT() {
// this.mandatoryColumns = new ArrayList<String>(Arrays.asList("Sample Cleanup",
// "Fractionation Type", "Cycle/Fraction Name", "Enrichment", "Labeling Type", "Label"));
// this.mandatoryFilled = new ArrayList<String>(Arrays.asList("Sample Name", "MS Device",
// "File Name", "LC Column", "LCMS Method", "Species", "Tissue"));
// this.optionalCols = new ArrayList<String>(Arrays.asList("Second Species", "Comment"));
//
// headersToTypeCodePerSampletype = new HashMap<>();
// headersToTypeCodePerSampletype.put(SampleType.Q_TEST_SAMPLE, new HashMap<>());
// // headersToTypeCodePerSampletype.put("SampleType.Q_MS_RUN", msRunMetadata);
// }
//
// /**
// * Reads in a TSV file containing openBIS samples that should be registered. Returns a List of
// * TSVSampleBeans containing all the necessary information to register each sample with its meta
// * information to openBIS, given that the types and parents exist.
// *
// * @param file
// * @return ArrayList of TSVSampleBeans
// * @throws IOException
// */
// public List<ISampleBean> readSamples(File file, boolean parseGraph) throws IOException {
// super.initReader();
//
// BufferedReader reader = new BufferedReader(new FileReader(file));
// ArrayList<String[]> data = new ArrayList<String[]>();
// String next;
// int i = 0;
// // isPilot = false;
// while ((next = reader.readLine()) != null) {
// i++;
// next = removeUTF8BOM(next);
// tsvByRows.add(next);
// String[] nextLine = next.split("\t", -1);// this is needed for trailing tabs
// if (data.isEmpty() || nextLine.length == data.get(0).length) {
// data.add(nextLine);
// } else {
// error = "Wrong number of columns in row " + i;
// reader.close();
// return null;
// }
// }
// reader.close();
//
// String[] header = data.get(0);
// data.remove(0);
// // find out where the mandatory and other metadata data is
// Map<String, Integer> headerMapping = new HashMap<String, Integer>();
// List<Integer> meta = new ArrayList<Integer>();
// List<Integer> factors = new ArrayList<Integer>();
// List<Integer> loci = new ArrayList<Integer>();
// int numOfLevels = 5;
//
// ArrayList<String> found = new ArrayList<String>(Arrays.asList(header));
// mandatoryColumns.addAll(mandatoryFilled);
// for (String col : mandatoryColumns) {
// if (!found.contains(col)) {
// error = "Mandatory column " + col + " not found.";
// return null;
// }
// }
// for (i = 0; i < header.length; i++) {
// int position = mandatoryColumns.indexOf(header[i]);
// if (position == -1)
// position = optionalCols.indexOf(header[i]);
// if (position > -1) {
// headerMapping.put(header[i], i);
// meta.add(i);
// } else {
// meta.add(i);
// }
// }
// // create samples
// List<ISampleBean> beans = new ArrayList<>();
// List<List<ISampleBean>> order = new ArrayList<>();
// Map<String, TSVSampleBean> analyteToSample = new HashMap<>();
// Map<SamplePreparationRun, Map<String, Object>> expIDToFracExp = new HashMap<>();
// Map<MSRunCollection, Map<String, Object>> msIDToMSExp = new HashMap<>();
//
// int rowID = 0;
// int sampleID = 0;
// for (String[] row : data) {
// rowID++;
// boolean special = false;
// if (!special) {
// for (String col : mandatoryFilled) {
// if (row[headerMapping.get(col)].isEmpty()) {
// error = col + " is a mandatory field, but it is not set for row " + rowID + "!";
// return null;
// }
// }
// // mandatory fields that need to be filled to identify sources and samples
// // String prepDate = row[headerMapping.get("Preparation Date")];
// // String ligandExtrID = sourceID + "-" + tissue + "-" + prepDate + "-" + antibody;
// // String msRunDate = row[headerMapping.get("MS Run Date")];
//
// String species = row[headerMapping.get("Species")];
// String tissue = row[headerMapping.get("Tissue")];
// String sampleName = row[headerMapping.get("Sample Name")];
// String lcmsMethod = row[headerMapping.get("LCMS Method")];
// String msDevice = row[headerMapping.get("MS Device")];
// String lcCol = row[headerMapping.get("LC Column")];
// String fName = row[headerMapping.get("File Name")];
//
// this.mandatoryColumns = new ArrayList<String>(Arrays.asList("Sample Cleanup",
// "Fractionation Type", "Cycle/Fraction Name", "Enrichment", "Labeling Type", "Label"));
//
// String cleanup = "";
// if (headerMapping.containsKey("Sample Cleanup")) {
// cleanup = row[headerMapping.get("Sample Cleanup")];
// }
// String comment = "";
// if (headerMapping.containsKey("Comment")) {
// comment = row[headerMapping.get("Comment")];
// }
//
// String labelingType = "";
// String label = "";
//
// if (headerMapping.containsKey("Labeling Type")) {
// labelingType = row[headerMapping.get("Labeling Type")];
// label = row[headerMapping.get("Label")];
// }
//
// String fracType = "";
// String fracName = "";
// String enrichType = "";
//
// fillParsedCategoriesToValuesForRow(headerMapping, row);
//
// if (headerMapping.containsKey("Enrichment")) {
// enrichType = row[headerMapping.get("Enrichment")];
// fracName = row[headerMapping.get("Cycle/Fraction Name")];
// }
//
// if (headerMapping.containsKey("Fractionation Type")) {
// fracType = row[headerMapping.get("Fractionation Type")];
// fracName = row[headerMapping.get("Cycle/Fraction Name")];
// }
//
// while (order.size() < numOfLevels) {
// order.add(new ArrayList<ISampleBean>());
// }
// // always one new measurement per row
// // chromatography options are stored on the MS level
// // if there is fractionation or enrichment, a new protein experiment and samples are needed
// // this is the case if fractionation or enrichment type is not empty
// // the number of fractions is taken from the fraction names as well as the source barcode
// // (protein barcode)
// // so all fractions from the same protein sample end up in the same fractionation experiment
// // IF the fractionation/enrichment type is the same
// SamplePreparationRun fracRun = null;
// if (!fracName.isEmpty()) {
// String fracID = proteinParent + "_" + fracType + "_" + fracName;
// fracRun = new SamplePreparationRun(proteinParent, prepDate, fracType, cleanup);
// TSVSampleBean fracSample = analyteToSample.get(fracID);
// if (fracSample == null) {
// sampleID++;
// fracSample = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE,
// fracID, fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
// fracSample.addParentID(proteinParent);
//
// proteinParent = Integer.toString(sampleID);
//
// fracSample.addProperty("Q_EXTERNALDB_ID", fracID);
// fracSample.addProperty("Q_SAMPLE_TYPE", "PROTEINS");
//
// order.get(0).add(fracSample);
// analyteToSample.put(fracID, fracSample);
//
// fracSample.setExperiment(Integer.toString(fracRun.hashCode()));
// Map<String, Object> fracExperimentMetadata = expIDToFracExp.get(fracRun);
// if (fracExperimentMetadata == null) {
// Map<String, Object> metadata = new HashMap<>();
// addFractionationOrEnrichmentToMetadata(metadata, fracType);
// // metadata.put("Q_FRACTIONATION_TYPE", fracType);
// expIDToFracExp.put(fracRun, parsePrepExperimentData(row, headerMapping, metadata));
// } else
// expIDToFracExp.put(fracRun,
// parsePrepExperimentData(row, headerMapping, fracExperimentMetadata));
// } else {
// proteinParent = fracSample.getCode();
// }
// }
// sampleID++;
// TSVSampleBean msRun = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_MS_RUN, "",
// fillMetadata(header, row, meta, factors, loci, SampleType.Q_MS_RUN));
// MSRunCollection msRuns = new MSRunCollection(fracRun, msRunDate, msDevice, lcCol);
// msRun.setExperiment(Integer.toString(msRuns.hashCode()));
// Map<String, Object> msExperiment = msIDToMSExp.get(msRuns);
// if (msExperiment == null)
// msIDToMSExp.put(msRuns, parseMSExperimentData(row, headerMapping, new HashMap<>()));
// msRun.addParentID(proteinParent);
// msRun.addProperty("File", fName);
// if (!comment.isEmpty()) {
// msRun.addProperty("Q_ADDITIONAL_INFO", comment);
// }
//
// order.get(1).add(msRun);
// }
// }
// experimentInfos = new HashMap<>();
//
// // fractionation experiments
// List<PreliminaryOpenbisExperiment> fracExperiments = new ArrayList<>();
// for (SamplePreparationRun prepRun : expIDToFracExp.keySet()) {
// Map<String, Object> map = expIDToFracExp.get(prepRun);
// // map.put("Code", Integer.toString(prepRun.hashCode()));// used to match samples to their
// // experiments later
// // msExperiments.add(map);
// PreliminaryOpenbisExperiment e =
// new PreliminaryOpenbisExperiment(ExperimentType.Q_SAMPLE_PREPARATION, map);
// e.setCode(Integer.toString(prepRun.hashCode()));
// fracExperiments.add(e);
// }
// experimentInfos.put(ExperimentType.Q_SAMPLE_PREPARATION, fracExperiments);
//
// // MS experiments
// List<PreliminaryOpenbisExperiment> msExperiments = new ArrayList<>();
// for (MSRunCollection runCollection : msIDToMSExp.keySet()) {
// Map<String, Object> map = msIDToMSExp.get(runCollection);
// // map.put("Code", Integer.toString(runCollection.hashCode()));// used to match samples to
// // their
// // experiments later
// // msExperiments.add(map);
// PreliminaryOpenbisExperiment e =
// new PreliminaryOpenbisExperiment(ExperimentType.Q_MS_MEASUREMENT, map);
// e.setCode(Integer.toString(runCollection.hashCode()));
// msExperiments.add(e);
// }
// experimentInfos.put(ExperimentType.Q_MS_MEASUREMENT, msExperiments);
// for (List<ISampleBean> level : order)
// beans.addAll(level);
// return beans;
// }
//
// @Override
// public Set<String> getAnalyteSet() {
// return new HashSet<String>(Arrays.asList("PROTEINS"));
// }
//
// @Override
// // TODO
// public int countEntities(File file) throws IOException {
// return 0;
// }
//
// @Override
// public List<TechnologyType> getTechnologyTypes() {
// // TODO Auto-generated method stub
// return null;
// }
//
// }


public class MSDesignReader implements IExperimentalDesignReader {

  private List<String> mandatoryColumns;
  private List<String> mandatoryFilled;
  private List<String> optionalCols;
  private final List<String> sampleTypesInOrder = new ArrayList<>(
      Arrays.asList("Q_BIOLOGICAL_ENTITY", "Q_BIOLOGICAL_SAMPLE", "Q_TEST_SAMPLE", "Q_MS_RUN"));
  private Map<SampleType, Map<String, List<String>>> headersToTypeCodePerSampletype;

  private String error;
  private Map<String, List<Map<String, Object>>> experimentInfos;
  private Set<String> speciesSet;
  private Set<String> tissueSet;
  private List<String> tsvByRows;
  private static final Logger logger = LogManager.getLogger(MSDesignReader.class);

  private HashMap<String, Command> parsers;

  public MSDesignReader() {
    this.mandatoryColumns = new ArrayList<>(Arrays.asList("File Name", "Organism ID",
        "Sample Secondary Name", "Species", "Tissue", "LC Column", "MS Device", "LCMS Method"));
    this.mandatoryFilled = new ArrayList<>(Arrays.asList("File Name", "Organism ID",
        "Sample Secondary Name", "Species", "Tissue", "LC Column", "MS Device", "LCMS Method"));
    this.optionalCols = new ArrayList<>(Arrays.asList("Expression System", "Pooled Sample",
        "Cycle/Fraction Name", "Fractionation Type", "Sample Preparation",
        "Sample Cleanup (protein)", "Digestion Method", "Digestion Enzyme", "Enrichment",
        "Sample Cleanup (peptide)", "Labeling Type", "Label", "Comments"));

    Map<String, List<String>> sourceMetadata = new HashMap<>();
    sourceMetadata.put("Species", Collections.singletonList("Q_NCBI_ORGANISM"));
    sourceMetadata.put("Expression System", Collections.singletonList(""));// TODO
    sourceMetadata.put("Organism ID", Arrays.asList("Q_EXTERNALDB_ID", "Q_SECONDARY_NAME"));

    Map<String, List<String>> extractMetadata = new HashMap<>();
    extractMetadata.put("Tissue", Collections.singletonList("Q_PRIMARY_TISSUE"));
    // extractMetadata.put("Extract ID", "Q_EXTERNALDB_ID");
    // extractMetadata.put("Detailed Tissue", "Q_TISSUE_DETAILED");

    Map<String, List<String>> proteinMetadata = new HashMap<>();
    proteinMetadata.put("Label", Collections.singletonList("Q_MOLECULAR_LABEL"));

    Map<String, List<String>> peptideMetadata = new HashMap<>();
    peptideMetadata.put("Label", Collections.singletonList("Q_MOLECULAR_LABEL"));
    peptideMetadata.put("Sample Secondary Name", Collections.singletonList("Q_SECONDARY_NAME"));
    // peptideMetadata.put("Sample Secondary Name", "Q_EXTERNALDB_ID");

    Map<String, List<String>> msRunMetadata = new HashMap<>();
    msRunMetadata.put("", Collections.singletonList(""));

    headersToTypeCodePerSampletype = new HashMap<>();
    headersToTypeCodePerSampletype.put(SampleType.Q_BIOLOGICAL_ENTITY, sourceMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_BIOLOGICAL_SAMPLE, extractMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_TEST_SAMPLE, new HashMap<>());
    // headersToTypeCodePerSampletype.put("Q_MS_RUN", msRunMetadata);
  }

  public Map<String, List<Map<String, Object>>> getExperimentInfos() {
    return experimentInfos;
  }

  public static final String UTF8_BOM = "\uFEFF";
  public static final String LIST_SEPARATOR = "+";

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
    List<Integer> factors = new ArrayList<Integer>();
    List<Integer> loci = new ArrayList<Integer>();
    int numOfLevels = 7;

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
      } else if (header[i].contains("Condition:")) {
        String condition = header[i].replace("Condition: ", "");
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
    // create samples
    List<ISampleBean> beans = new ArrayList<>();
    List<List<ISampleBean>> order = new ArrayList<>();
    Map<String, TSVSampleBean> sourceIDToSample = new HashMap<>();
    Map<String, TSVSampleBean> tissueToSample = new HashMap<>();
    Map<String, TSVSampleBean> proteinToSample = new HashMap<>();
    Map<String, TSVSampleBean> peptideToSample = new HashMap<>();
    Map<String, TSVSampleBean> fracProtToSample = new HashMap<>();
    Map<String, TSVSampleBean> fracPepToSample = new HashMap<>();

    Map<LigandPrepRun, Map<String, Object>> expIDToLigandExp =
        new HashMap<LigandPrepRun, Map<String, Object>>();
    Map<MSRunCollection, Map<String, Object>> msIDToMSExp =
        new HashMap<MSRunCollection, Map<String, Object>>();
    // Map<String, TSVSampleBean>
    List<TSVSampleBean> roots = new ArrayList<TSVSampleBean>();
    Set<String> speciesSet = new HashSet<String>();
    Set<String> tissueSet = new HashSet<String>();
    int rowID = 0;
    int sampleID = 0;
    while (order.size() < numOfLevels) {
      order.add(new ArrayList<ISampleBean>());
    }
    for (String[] row : data) {
      rowID++;
      boolean special = false;
      if (!special) {
        for (String col : mandatoryFilled) {
          if (row[headerMapping.get(col)].isEmpty()) {
            error = col + " is a mandatory field, but it is not set for row " + rowID + "!";
            return null;
          }
        }
        parsers = new HashMap<String, Command>();
        parsers.put("Q_MS_LCMS_METHOD", new Command() {
          @Override
          public Object parse(String value) {
            return parseLCMSMethod(value);
          }
        });
        // mandatory fields that need to be filled to identify sources and samples
        // possibilities:
        // 1. measurement without fractions/enrichment and pooling. use organism ID and sample
        // secondary name to search for existing entities, create and store in map otherwise.
        // a) without digestion: create protein sample
        // b) with digestion: create protein and peptide sample - collect peptide samples using same
        // digestion enzymes and method into same experiment
        // 2. measurement with fractions or enrichment cycles. use organism ID and secondary name to
        // search for existing entities.
        // a) without digestion: create protein sample and use fraction names to fractionate them
        // into additional protein samples
        // b) with digestion: do the same, then create peptide samples
        // 3. pooling. use organism ID and sample secondary name to search for existing entities:
        // a) existing secondary name: only allowed if fractionation/enrichment is used. use
        // alpha:



        // if digested => search
        // if not known create digested

        String sourceID = row[headerMapping.get("Organism ID")];
        String species = row[headerMapping.get("Species")];
        String expressionSystem = row[headerMapping.get("Expression System")];
        String tissue = row[headerMapping.get("Tissue")];
        String fileName = row[headerMapping.get("File Name")];
        String sampleName = row[headerMapping.get("Sample Secondary Name")];
        String poolName = row[headerMapping.get("Pooled Sample")];
        String digestType = row[headerMapping.get("Digestion Method")];
        String enzymes = row[headerMapping.get("Digestion enzyme")];
        String fracType = row[headerMapping.get("Fractionation Type")];
        String enrichType = row[headerMapping.get("Enrichment")];
        String fracName = row[headerMapping.get("Cycle/Fraction Name")];
        String isoLabelType = row[headerMapping.get("Labeling Type")];
        String isoLabel = row[headerMapping.get("Label")];

        // perform some sanity testing using XOR operator
        if (isoLabelType == null ^ isoLabel == null) {
          error = String.format(
              "Error in line %s: If sample label is specified, the isotope labeling type must be set and vice versa.",
              rowID);
          return null;
        }
        if (enzymes == null ^ digestType == null) {
          error = String.format(
              "Error in line %s: If sample digestion enzyme is specified, the digestion method must be set and vice versa.",
              rowID);
          return null;
        }
        if (fracName == null ^ (enrichType == null && fracType == null)) {
          error = String.format(
              "Error in line %s: If fractionation or enrichment is used, both type and fraction/cycle names must be specified.",
              rowID);
          return null;
        }

        speciesSet.add(species);
        tissueSet.add(tissue);

        // always one new measurement per row

        // if sample is pooled, all levels before must be known already from previous lines
        if (poolName != null) {
          TSVSampleBean source = sourceIDToSample.get(sourceID);
          String tissueID = sourceID + "-" + tissue;
          TSVSampleBean tissueSample = tissueToSample.get(tissueID);

          if (tissueSample == null || source == null) {
            error = String.format(
                "Error in line %s: Source with respective tissue must be defined in previous lines for pooled sample %s.",
                rowID, poolName);
            return null;
          }

          // try to find all defined parent samples
          List<TSVSampleBean> parents = new ArrayList<>();
          boolean parentFound = false;
          boolean parentsPeptides = false;
          boolean parentsProteins = false;
          for (String parentID : parsePoolParents(poolName)) {
            if (proteinToSample.containsKey(parentID)) {
              parentFound = true;
              parentsProteins = true;
              parents.add(proteinToSample.get(parentID));
            }
            if (peptideToSample.containsKey(parentID)) {
              parentFound = true;
              parentsPeptides = true;
              parents.add(peptideToSample.get(parentID));
            }
            if (fracProtToSample.containsKey(parentID)) {
              parentFound = true;
              parentsProteins = true;
              parents.add(fracProtToSample.get(parentID));
            }
            if (fracPepToSample.containsKey(parentID)) {
              parentFound = true;
              parentsPeptides = true;
              parents.add(fracPepToSample.get(parentID));
            }
            if (!parentFound) {
              error = String.format(
                  "Error in line %s: Sample identifier %s used in pooled sample %s must be defined in previous line %s.",
                  rowID, parentID, poolName);
              return null;
            }
            parentFound = false;
          }
          if (parentsPeptides && parentsProteins) {
            error = String.format(
                "Error in line %s: Pooled sample %s cannot consist of protein and peptide samples at the same time.",
                rowID, poolName);
            return null;
          }
          sampleID++;
          String analyte = "PROTEINS";
          if (parentsPeptides) {
            analyte = "PEPTIDES";
          }
          TSVSampleBean pool =
              new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE, poolName,
                  fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
          pool.addProperty("Q_EXTERNALDB_ID", poolName);
          peptideToSample.put(peptideID, pool);
          pool.addProperty("Q_SAMPLE_TYPE", analyte);
          for (TSVSampleBean parent : parents) {
            pool.addParentID(parent.getCode());
          }
          // a new digestion sample below the pool is needed when digest parameters are specified
          // and parent sample(s) are not already peptides
          if (digestType != null && !parentsPeptides) {
            
          }
        }


        // if organism id not known => create organism entity. put in map.
        // else get organism entity.
        TSVSampleBean sampleSource = sourceIDToSample.get(sourceID);
        if (sampleSource == null) {
          sampleID++;

          sampleSource = new TSVSampleBean(Integer.toString(sampleID),
              SampleType.Q_BIOLOGICAL_ENTITY, sourceID,
              fillMetadata(header, row, meta, factors, loci, SampleType.Q_BIOLOGICAL_ENTITY));
          // sampleSource.addProperty("Q_EXTERNALDB_ID", sourceID);
          roots.add(sampleSource);
          order.get(0).add(sampleSource);
          sourceIDToSample.put(sourceID, sampleSource);
        }
        // we don't have tissue ids, so we build unique identifiers by adding sourceID and tissue
        // name
        String tissueID = sourceID + "-" + tissue;
        TSVSampleBean tissueSample = tissueToSample.get(tissueID);
        if (tissueSample == null) {
          sampleID++;

          tissueSample = new TSVSampleBean(Integer.toString(sampleID),
              SampleType.Q_BIOLOGICAL_SAMPLE, tissueID,
              fillMetadata(header, row, meta, factors, loci, SampleType.Q_BIOLOGICAL_SAMPLE));
          order.get(1).add(tissueSample);
          tissueSample.addParentID(sourceID);
          tissueSample.addProperty("Q_EXTERNALDB_ID", tissueID);
          tissueToSample.put(tissueID, tissueSample);
        }
        // if sample secondary name not known => create protein sample
        TSVSampleBean proteinSample = proteinToSample.get(sampleName);
        if (proteinSample == null) {
          sampleID++;

          proteinSample = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE,
              sampleName, fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
          order.get(2).add(proteinSample);
          proteinSample.addParentID(tissueID);
          proteinSample.addProperty("Q_EXTERNALDB_ID", sampleName);
          proteinToSample.put(sampleName, proteinSample);
          proteinSample.addProperty("Q_SAMPLE_TYPE", "PROTEINS");

          // TODO compare: fractionation/enrichment, check pooling
          // if sample is digested, the peptide sample will relate to the sample name
          if (digestType != null) {
            String peptideID = sampleName + " digested";
            TSVSampleBean peptideSample = peptideToSample.get(peptideID);
            if (peptideSample == null) {
              sampleID++;

              peptideSample =
                  new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE, peptideID,
                      fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
              order.get(3).add(peptideSample);
              peptideSample.addParentID(sampleName);
              peptideSample.addProperty("Q_EXTERNALDB_ID", peptideID);
              peptideToSample.put(peptideID, peptideSample);
              peptideSample.addProperty("Q_SAMPLE_TYPE", "PEPTIDES");
            }
          }
        }
        // TODO place in right context
        if (fracName != null) {
          String fracID = sampleName + "-" + fracName;// TODO?
          TSVSampleBean fracProtSample = fracProtToSample.get(fracID);
          if (fracProtSample == null) {
            sampleID++;

            fracProtSample = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE,
                fracName, fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
            order.get(4).add(proteinSample);
            fracProtSample.addParentID(sampleName);
            fracProtSample.addProperty("Q_EXTERNALDB_ID", fracName);
            fracProtToSample.put(fracID, fracProtSample);
            fracProtSample.addProperty("Q_SAMPLE_TYPE", "PROTEINS");
          }
          if (digestType != null) {
            sampleID++;

            String peptideID = fracID + " digested";
            TSVSampleBean fracPeptideSample = fracPepToSample.get(peptideID);
            if (fracPeptideSample == null) {
              sampleID++;

              fracPeptideSample =
                  new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE, peptideID,
                      fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
              order.get(5).add(fracPeptideSample);
              fracPeptideSample.addParentID(fracID);
              fracPeptideSample.addProperty("Q_EXTERNALDB_ID", peptideID);
              fracPepToSample.put(peptideID, fracPeptideSample);
              fracPeptideSample.addProperty("Q_SAMPLE_TYPE", "PEPTIDES");
            }
          }
        }


        // Ligand Extract Level (Analyte)
        // TSVSampleBean ligandExtract = analyteToSample.get(ligandExtrID);

        // Two ligand samples were prepared together (e.g. multiple antibody columns) only if
        // patient, prep date, handled tissue and sample amount (mass, vol or cell count) are the
        // same
        // LigandPrepRun ligandPrepRun =
        // new LigandPrepRun(sourceID, tissue, prepDate, sampleAmount + " " + amountColName);
        // if (ligandExtract == null) {
        // sampleID++;
        // ligandExtract = new TSVSampleBean(Integer.toString(sampleID),
        // SampleType.Q_MHC_LIGAND_EXTRACT, extractID,
        // fillMetadata(header, row, meta, factors, loci, SampleType.Q_MHC_LIGAND_EXTRACT));
        // ligandExtract.addProperty("Q_ANTIBODY", antibody);
        // ligandExtract.addParentID(prepID);
        // ligandExtract.addProperty("Q_EXTERNALDB_ID", ligandExtrID);
        // order.get(3).add(ligandExtract);
        // analyteToSample.put(ligandExtrID, ligandExtract);
        //
        // ligandExtract.setExperiment(Integer.toString(ligandPrepRun.hashCode()));
        // Map<String, Object> ligandExperimentMetadata = expIDToLigandExp.get(ligandPrepRun);
        // if (ligandExperimentMetadata == null) {
        // Map<String, Object> metadata = new HashMap<String, Object>();
        // expIDToLigandExp.put(ligandPrepRun,
        // parseLigandExperimentData(row, headerMapping, metadata));
        // } else
        // expIDToLigandExp.put(ligandPrepRun,
        // parseLigandExperimentData(row, headerMapping, ligandExperimentMetadata));
        // }
        TSVSampleBean msRun = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_MS_RUN, "",
            fillMetadata(header, row, meta, factors, loci, SampleType.Q_MS_RUN));
        // MSRunCollection msRuns = new MSRunCollection(ligandPrepRun, msRunDate);
        // msRun.setExperiment(Integer.toString(msRuns.hashCode()));
        // Map<String, Object> msExperiment = msIDToMSExp.get(msRuns);
        // if (msExperiment == null)
        // TODO can we be sure that all metadata is the same if ligand prep run
        // and ms run date are the same?
        // msIDToMSExp.put(msRuns,
        // parseMSExperimentData(row, headerMapping, new HashMap<String, Object>()));
        // msRun.addParentID(ligandExtrID);
        msRun.addProperty("File", fName);

        order.get(4).add(msRun);
      }
    }
    experimentInfos = new HashMap<String, List<Map<String, Object>>>();

    // mhc ligand extraction experiments
    List<Map<String, Object>> ligandExperiments = new ArrayList<Map<String, Object>>();
    for (LigandPrepRun prepRun : expIDToLigandExp.keySet()) {
      Map<String, Object> map = expIDToLigandExp.get(prepRun);
      map.put("Code", Integer.toString(prepRun.hashCode()));// used to match samples to their
      // experiments later
      ligandExperiments.add(map);
    }
    experimentInfos.put("Q_MHC_LIGAND_EXTRACTION", ligandExperiments);

    // MS experiments
    List<Map<String, Object>> msExperiments = new ArrayList<Map<String, Object>>();
    for (MSRunCollection runCollection : msIDToMSExp.keySet()) {
      Map<String, Object> map = msIDToMSExp.get(runCollection);
      map.put("Code", Integer.toString(runCollection.hashCode()));// used to match samples to their
      // experiments later
      msExperiments.add(map);
    }
    experimentInfos.put("Q_MS_MEASUREMENT", msExperiments);
    System.out.println(expIDToLigandExp.keySet());
    for (List<ISampleBean> level : order)
      beans.addAll(level);
    boolean unique = checkUniqueIDsBetweenSets(speciesSet, tissueSet);
    if (!unique)
      return null;
    this.speciesSet = speciesSet;
    this.tissueSet = tissueSet;
    return beans;
  }

  private List<String> parsePoolParents(String poolName) {
    return new ArrayList<>(Arrays.asList(poolName.split(LIST_SEPARATOR)));
  }

  private Map<String, Object> parseMSExperimentData(String[] row,
      Map<String, Integer> headerMapping, HashMap<String, Object> metadata) {
    Map<String, String> designMap = new HashMap<String, String>();
    designMap.put("MS Device", "Q_MS_DEVICE");
    designMap.put("LCMS Method", "Q_MS_LCMS_METHOD");
    designMap.put("Comments", "Q_ADDITIONAL_INFO");
    for (String col : designMap.keySet()) {
      Object val = "";
      String openbisType = designMap.get(col);
      if (headerMapping.containsKey(col)) {
        val = row[headerMapping.get(col)];
        if (parsers.containsKey(openbisType)) {
          val = parsers.get(openbisType).parse((String) val);
        }
      }
      metadata.put(openbisType, val);
    }
    return metadata;
  }

  protected Object parseLCMSMethod(String value) {
    return value;
  }

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
      List<Integer> factors, List<Integer> loci, SampleType type) {
    Map<String, List<String>> headersToOpenbisCode = headersToTypeCodePerSampletype.get(type);
    HashMap<String, Object> res = new HashMap<String, Object>();
    if (headersToOpenbisCode != null) {
      for (int i : meta) {
        String label = header[i];
        if (!data[i].isEmpty() && headersToOpenbisCode.containsKey(label)) {
          for (String propertyCode : headersToOpenbisCode.get(label)) {
            Object val = data[i];
            if (parsers.containsKey(propertyCode))
              val = parsers.get(propertyCode).parse(data[i]);
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
    return 0;
  }

  @Override
  public List<TechnologyType> getTechnologyTypes() {
    // TODO Auto-generated method stub
    return null;
  }

}
