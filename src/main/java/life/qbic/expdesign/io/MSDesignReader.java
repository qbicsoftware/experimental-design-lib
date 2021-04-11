package life.qbic.expdesign.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import life.qbic.datamodel.ms.MSProperties;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.model.MassSpecSampleHierarchy;
import life.qbic.expdesign.model.ProteinPeptidePreparationProperties;
import life.qbic.expdesign.model.StructuredExperiment;
import life.qbic.xml.properties.Unit;
import life.qbic.xml.study.TechnologyType;


public class MSDesignReader implements IExperimentalDesignReader {

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
  private static final Logger logger = LogManager.getLogger(MSDesignReader.class);

  public static final String UTF8_BOM = "\uFEFF";
  public static final String LIST_SEPARATOR = "\\+";
  public static final Set<String> LABELING_TYPES_WITHOUT_LABELS =
      new HashSet<>(Arrays.asList("LFQ", "None"));
  public static final String SAMPLE_KEYWORD = "Secondary Name";
  public static final String SAMPLE_ALTNAME_KEYWORD = "Sample Name";

  public MSDesignReader() {
    this.mandatoryColumns = new ArrayList<>(
        Arrays.asList("Labeling Type", "File Name", "Organism ID", "Sample Name", "Secondary Name",
            "Species", "Tissue", "LC Column", "MS Device", "LCMS Method", "Injection Volume (uL)"));
    this.mandatoryFilled =
        new ArrayList<>(Arrays.asList("Labeling Type", "File Name", "Organism ID", "Sample Name",
            "Secondary Name", "Species", "Tissue", "LC Column", "MS Device", "LCMS Method"));
    this.optionalCols = new ArrayList<>(Arrays.asList("Expression System", "Technical Replicates",
        "Pooled Sample", "Cycle/Fraction Name", "Fractionation Type", "Sample Preparation",
        "Sample Cleanup (Protein)", "Digestion Method", "Enrichment Method",
        "Sample Cleanup (Peptide)", "Label", "Customer Comment", "Facility Comment",
        "Digestion Enzyme"));

    Map<String, List<String>> sourceMetadata = new HashMap<>();
    sourceMetadata.put("Species", Collections.singletonList("Q_NCBI_ORGANISM"));
    sourceMetadata.put("Expression System", Collections.singletonList("Q_EXPRESSION_SYSTEM"));
    sourceMetadata.put("Organism ID", Arrays.asList("Q_EXTERNALDB_ID", "Q_SECONDARY_NAME"));

    Map<String, List<String>> extractMetadata = new HashMap<>();
    extractMetadata.put("Tissue", Collections.singletonList("Q_PRIMARY_TISSUE"));
    extractMetadata.put("Customer Comment", Collections.singletonList("Q_ADDITIONAL_INFO"));
    // extractMetadata.put("Extract ID", "Q_EXTERNALDB_ID");
    // extractMetadata.put("Detailed Tissue", "Q_TISSUE_DETAILED");

    Map<String, List<String>> proteinMetadata = new HashMap<>();
    proteinMetadata.put("Label", Collections.singletonList("Q_MOLECULAR_LABEL"));

    Map<String, List<String>> peptideMetadata = new HashMap<>();
    peptideMetadata.put("Label", Collections.singletonList("Q_MOLECULAR_LABEL"));
    // peptideMetadata.put("Secondary Name", Collections.singletonList("Q_SECONDARY_NAME"));
    // peptideMetadata.put("Sample Secondary Name", "Q_EXTERNALDB_ID");

    Map<String, List<String>> msRunMetadata = new HashMap<>();
    msRunMetadata.put("Facility Comment", Collections.singletonList("Q_ADDITIONAL_INFO"));
    msRunMetadata.put("Injection Volume (uL)", Collections.singletonList("Q_INJECTION_VOLUME"));

    headersToTypeCodePerSampletype = new HashMap<>();
    headersToTypeCodePerSampletype.put(SampleType.Q_BIOLOGICAL_ENTITY, sourceMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_BIOLOGICAL_SAMPLE, extractMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_TEST_SAMPLE, peptideMetadata);// TODO
    headersToTypeCodePerSampletype.put(SampleType.Q_MS_RUN, msRunMetadata);
  }

  private boolean collectionContainsStringCaseInsensitive(Collection<String> collection,
      String str) {
    boolean res = false;
    for (String s : collection) {
      res |= s.equalsIgnoreCase(str);
    }
    return res;
  }

  private void fillParsedCategoriesToValuesForRow(Map<String, Integer> headerMapping,
      String[] row) {
    // logger.info("Collecting possible CV entries for row.");
    addValueForCategory(headerMapping, row, "MS Device");
    addValueForCategory(headerMapping, row, "LC Column");
    addValueForCategory(headerMapping, row, "Sample Cleanup (Protein)");
    addValueForCategory(headerMapping, row, "Sample Cleanup (Peptide)");
    addValueForCategory(headerMapping, row, "Sample Preparation");
    addValueForCategory(headerMapping, row, "Digestion Enzyme");
    addValueForCategory(headerMapping, row, "Digestion Method");
    addValueForCategory(headerMapping, row, "Fractionation Type");
    addValueForCategory(headerMapping, row, "Enrichment Method");
    addValueForCategory(headerMapping, row, "Labeling Type");
    addValueForCategory(headerMapping, row, "Label");
    addValueForCategory(headerMapping, row, "Species");
    addValueForCategory(headerMapping, row, "Expression System");
    addValueForCategory(headerMapping, row, "Tissue");
    addValueForCategory(headerMapping, row, "LCMS Method");
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
        String replicateID = "";
        if (headerMapping.containsKey("Technical Replicates")) {
          replicateID = row[headerMapping.get("Technical Replicates")];
        }
        String extractID = sourceID + row[headerMapping.get("Tissue")] + replicateID;
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
    List<MassSpecSampleHierarchy> order =
        new ArrayList<>(Arrays.asList(MassSpecSampleHierarchy.values()));
    Map<MassSpecSampleHierarchy, List<ISampleBean>> samplesInOrder = new HashMap<>();
    for (MassSpecSampleHierarchy level : order) {
      samplesInOrder.put(level, new ArrayList<>());
    }

    Map<String, TSVSampleBean> sourceIDToSample = new HashMap<>();
    Map<String, TSVSampleBean> tissueToSample = new HashMap<>();
    Map<String, TSVSampleBean> proteinToSample = new HashMap<>();
    Map<String, TSVSampleBean> peptideToSample = new HashMap<>();
    Map<String, TSVSampleBean> fracProtToSample = new HashMap<>();
    Map<String, TSVSampleBean> fracPepToSample = new HashMap<>();
    Map<String, TSVSampleBean> protPoolToSample = new HashMap<>();
    Map<String, TSVSampleBean> peptPoolToSample = new HashMap<>();
    Map<String, TSVSampleBean> fracProtPoolToSample = new HashMap<>();
    Map<String, TSVSampleBean> fracPeptPoolToSample = new HashMap<>();

    Map<MSProperties, String> msPropertiesToID = new HashMap<>();
    Map<ProteinPeptidePreparationProperties, String> SamplePrepPropertiesToID = new HashMap<>();

    speciesSet = new HashSet<String>();
    tissueSet = new HashSet<String>();
    analyteSet = new HashSet<String>();
    analyteSet.add("PROTEINS");
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
        String sourceID = row[headerMapping.get("Organism ID")];
        String species = row[headerMapping.get("Species")];
        String expressionSystem = null;
        if (headerMapping.containsKey("Expression System")) {
          expressionSystem = row[headerMapping.get("Expression System")];
        }
        String tissue = row[headerMapping.get("Tissue")];
        String replicateID = "";
        if (headerMapping.containsKey("Technical Replicates")) {
          replicateID = row[headerMapping.get("Technical Replicates")];
        }

        String fileName = row[headerMapping.get("File Name")];

        String sampleKey = row[headerMapping.get(SAMPLE_KEYWORD)];
        String sampleAltName = row[headerMapping.get(SAMPLE_ALTNAME_KEYWORD)];

        String poolName = row[headerMapping.get("Pooled Sample")];
        String digestType = row[headerMapping.get("Digestion Method")];
        String enzymeString = row[headerMapping.get("Digestion Enzyme")];
        List<String> enzymes = new ArrayList<>();
        if (!enzymeString.isEmpty()) {
          enzymes = new ArrayList<String>(Arrays.asList(enzymeString.split(LIST_SEPARATOR)));
        }
        if (!digestType.isEmpty()) {
          analyteSet.add("PEPTIDES");
        }
        String cleanProt = null;
        if (headerMapping.containsKey("Sample Cleanup (Protein)")) {
          cleanProt = row[headerMapping.get("Sample Cleanup (Protein)")];
        }
        String cleanPept = null;
        if (headerMapping.containsKey("Sample Cleanup (Peptide)")) {
          cleanPept = row[headerMapping.get("Sample Cleanup (Peptide)")];
        }
        String sampPrepType = null;
        if (headerMapping.containsKey("Sample Preparation")) {
          sampPrepType = row[headerMapping.get("Sample Preparation")];
        }

        String fracType = row[headerMapping.get("Fractionation Type")];
        String enrichType = row[headerMapping.get("Enrichment Method")];
        String fracName = row[headerMapping.get("Cycle/Fraction Name")];
        String isoLabelType = row[headerMapping.get("Labeling Type")];
        String isoLabel = row[headerMapping.get("Label")];

        // perform some sanity testing using XOR operator
        if ((isoLabelType.isEmpty() ^ isoLabel.isEmpty())
            && !collectionContainsStringCaseInsensitive(LABELING_TYPES_WITHOUT_LABELS,
                isoLabelType)) {
          error = String.format(
              "Error in line %s: If sample label is specified, the isotope labeling type must be set and vice versa.",
              rowID);
          return null;
        }
        if (enzymes.isEmpty() ^ digestType.isEmpty()) {
          error = String.format(
              "Error in line %s: If sample digestion enzyme is specified, the digestion method must be set and vice versa.",
              rowID);
          return null;
        }
        if (fracName.isEmpty() ^ (enrichType.isEmpty() && fracType.isEmpty())) {
          error = String.format(
              "Error in line %s: If fractionation or enrichment is used, both type and fraction/cycle names must be specified.",
              rowID);
          return null;
        }
        if (!poolName.isEmpty() && !poolName.contains(LIST_SEPARATOR.replace("\\", ""))) {
          error = String.format(
              "Error in line %s: Pool name %s must reference more than one previously defined sample, separated by '+'.",
              rowID, poolName);
        }
        if (!fracName.isEmpty() && !poolName.isEmpty()) {
          error = String.format(
              "Error in line %s: Cannot define fractionation/enrichment samples in the same line as pooled samples. Please refer "
                  + "to fraction/cycle names that are to be pooled after defining them in their own lines. Alternatively, you can use "
                  + "fractionation/enrichment on a pooled sample, which has been defined in a previous line by using its sample name.",
              rowID);
          return null;
        }

        speciesSet.add(species);
        tissueSet.add(tissue);

        // always one new measurement per row
        sampleID++;
        TSVSampleBean msRun = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_MS_RUN, "",
            fillMetadata(header, row, meta, factors, new ArrayList<>(), SampleType.Q_MS_RUN));
        msRun.addProperty("File", fileName);

        String lcmsMethod = row[headerMapping.get("LCMS Method")];
        String msDevice = row[headerMapping.get("MS Device")];
        String column = row[headerMapping.get("LC Column")];
        MSProperties msProperties = new MSProperties(lcmsMethod, msDevice, "", column);
        String expID = Integer.toString(msProperties.hashCode());
        msPropertiesToID.put(msProperties, expID);
        msRun.setExperiment(expID);

        samplesInOrder.get(MassSpecSampleHierarchy.MassSpecRun).add(msRun);

        // if organism id not known => create organism entity. put in map.
        // else get organism entity. same for tissue. this is done for pool rows, as well
        TSVSampleBean sampleSource = sourceIDToSample.get(sourceID);
        if (sampleSource == null) {
          sampleID++;

          sampleSource = new TSVSampleBean(Integer.toString(sampleID),
              SampleType.Q_BIOLOGICAL_ENTITY, sourceID,
              fillMetadata(header, row, meta, entityFactors, loci, SampleType.Q_BIOLOGICAL_ENTITY));
          // sampleSource.addProperty("Q_EXTERNALDB_ID", sourceID);
          samplesInOrder.get(MassSpecSampleHierarchy.Organism).add(sampleSource);
          sourceIDToSample.put(sourceID, sampleSource);

          if (expressionSystem != null) {
            speciesSet.add(expressionSystem);
            sampleSource.addProperty("Q_EXPRESSION_SYSTEM", expressionSystem);
          }
        }
        // we don't have tissue ids, so we build unique identifiers by adding sourceID and tissue
        // name
        String tissueID = sourceID + "-" + tissue + "-" + replicateID;
        TSVSampleBean tissueSample = tissueToSample.get(tissueID);
        if (tissueSample == null) {
          sampleID++;

          tissueSample = new TSVSampleBean(Integer.toString(sampleID),
              SampleType.Q_BIOLOGICAL_SAMPLE, tissueID, fillMetadata(header, row, meta,
                  extractFactors, loci, SampleType.Q_BIOLOGICAL_SAMPLE));
          samplesInOrder.get(MassSpecSampleHierarchy.Tissue).add(tissueSample);
          tissueSample.addParentID(sampleSource.getCode());
          tissueSample.addProperty("Q_EXTERNALDB_ID", tissueID);
          tissueToSample.put(tissueID, tissueSample);
        }

        if (!poolName.isEmpty()) {

          // old code, pool parents can now be defined in the pooling row
          // if (tissueSample == null || source == null) {
          // error = String.format(
          // "Error in line %s: Source with respective tissue must be defined in previous lines for
          // pooled sample %s.",
          // rowID, poolName);
          // return null;
          // }
          // try to find all defined parent samples
          List<TSVSampleBean> parents = new ArrayList<>();
          boolean fractParents = false;
          boolean parentFound = false;
          boolean parentsPeptides = false;
          boolean parentsProteins = false;
          for (String parentID : parsePoolParents(poolName)) {
            if (peptideToSample.containsKey(parentID)) {
              parentFound = true;
              parentsPeptides = true;
              parents.add(peptideToSample.get(parentID));
            } else if (proteinToSample.containsKey(parentID)) {
              parentFound = true;
              parentsProteins = true;
              parents.add(proteinToSample.get(parentID));
            } else if (fracPepToSample.containsKey(parentID)) {
              parentFound = true;
              parentsPeptides = true;
              fractParents = true;
              parents.add(fracPepToSample.get(parentID));
            } else if (fracProtToSample.containsKey(parentID)) {
              parentFound = true;
              parentsProteins = true;
              fractParents = true;
              parents.add(fracProtToSample.get(parentID));
            }
            if (!parentFound) {
              // old code, pool parents can now be defined in the pooling row
              // error = String.format(
              // "Error in line %s: Sample identifier %s used in pooled sample %s must be defined in
              // previous line.",
              // rowID, parentID, poolName);
              // return null;
              // define necessary parent hierarchy
              // Assumptions:
              // 1. digestion happens AFTER pooling of (protein) samples, so we only have to create
              // a protein sample
              sampleID++;

              TSVSampleBean parentProteinSample =
                  new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE, parentID,
                      fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
              System.err.println("510");
              System.err.println(sampleID);
              ProteinPeptidePreparationProperties props =
                  new ProteinPeptidePreparationProperties(enzymes, digestType, "PROTEINS",
                      cleanProt, fracType, enrichType, isoLabelType, sampPrepType);
              String prepExpID = Integer.toString(props.hashCode());
              SamplePrepPropertiesToID.put(props, prepExpID);
              parentProteinSample.setExperiment(prepExpID);

              samplesInOrder.get(MassSpecSampleHierarchy.Proteins).add(parentProteinSample);
              parentProteinSample.addParentID(tissueSample.getCode());
              parentProteinSample.addProperty("Q_EXTERNALDB_ID", parentID);
              proteinToSample.put(parentID, parentProteinSample);
              parentProteinSample.addProperty("Q_SAMPLE_TYPE", "PROTEINS");

              parents.add(parentProteinSample);
              parentsProteins = true;

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
          String cleanType = cleanProt;
          if (parentsPeptides) {
            analyte = "PEPTIDES";
            cleanType = cleanPept;
          }
          TSVSampleBean pool =
              new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE, sampleAltName,
                  fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
          pool.addProperty("Q_EXTERNALDB_ID", sampleKey);
          System.err.println("548");
          System.err.println(sampleID);

          ProteinPeptidePreparationProperties props =
              new ProteinPeptidePreparationProperties(enzymes, digestType, analyte, cleanType,
                  fracType, enrichType, isoLabelType, sampPrepType);
          String prepExpID = Integer.toString(props.hashCode());
          SamplePrepPropertiesToID.put(props, prepExpID);
          pool.setExperiment(prepExpID);

          pool.addProperty("Q_SAMPLE_TYPE", analyte);
          for (TSVSampleBean parent : parents) {
            pool.addParentID(parent.getCode());
          }

          // a new digestion sample below the pool is needed when digest parameters are specified
          // and parent sample(s) are not already peptides
          if (!digestType.isEmpty() && !parentsPeptides) {
            sampleID++;
            System.err.println("567");
            System.err.println(sampleID);
            TSVSampleBean digestedPool = new TSVSampleBean(Integer.toString(sampleID),
                SampleType.Q_TEST_SAMPLE, sampleAltName,
                fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
            digestedPool.addProperty("Q_EXTERNALDB_ID", sampleKey);
            digestedPool.addProperty("Q_SAMPLE_TYPE", "PEPTIDES");

            props = new ProteinPeptidePreparationProperties(enzymes, digestType, "PEPTIDES",
                cleanPept, fracType, enrichType, isoLabelType, sampPrepType);
            prepExpID = Integer.toString(props.hashCode());
            SamplePrepPropertiesToID.put(props, prepExpID);
            digestedPool.setExperiment(prepExpID);

            if (fractParents) {
              fracPeptPoolToSample.put(sampleKey, digestedPool);
              samplesInOrder.get(MassSpecSampleHierarchy.PooledFractionedPeptides)
                  .add(digestedPool);
            } else {
              samplesInOrder.get(MassSpecSampleHierarchy.PooledPeptides).add(digestedPool);
              peptPoolToSample.put(sampleKey, digestedPool);
            }
            pool.addProperty("Q_SAMPLE_TYPE", "PEPTIDES");
            msRun.addParentID(digestedPool.getCode());
          } else {
            msRun.addParentID(pool.getCode());
          }

          // ordering - testing for parent peptide samples and fractions is important, as these are
          // the lowest possible previous levels
          if (parentsPeptides && fractParents) {
            samplesInOrder.get(MassSpecSampleHierarchy.PooledFractionedPeptides).add(pool);
            fracPeptPoolToSample.put(sampleKey, pool);
          } else if (parentsPeptides && !fractParents) {
            samplesInOrder.get(MassSpecSampleHierarchy.PooledPeptides).add(pool);
            peptPoolToSample.put(sampleKey, pool);
          } else if (!parentsPeptides && fractParents) {
            samplesInOrder.get(MassSpecSampleHierarchy.PooledFractionedProteins).add(pool);
            fracProtPoolToSample.put(sampleKey, pool);
          } else {
            samplesInOrder.get(MassSpecSampleHierarchy.PooledProteins).add(pool);
            protPoolToSample.put(sampleKey, pool);
          }

        } else {
          // end pooling block
          ////////////////////

          // if sample secondary name not known => create protein sample
          // but: ignore, if this is a fractionation of a known pooled sample
          if (fracName.isEmpty() || (!peptPoolToSample.containsKey(sampleKey)
              && !protPoolToSample.containsKey(sampleKey))) {

            TSVSampleBean proteinSample = proteinToSample.get(sampleKey);
            if (proteinSample == null) {
              sampleID++;
              System.err.println("622");
              System.err.println(sampleID);
              proteinSample = new TSVSampleBean(Integer.toString(sampleID),
                  SampleType.Q_TEST_SAMPLE, sampleAltName,
                  fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
              samplesInOrder.get(MassSpecSampleHierarchy.Proteins).add(proteinSample);
              proteinSample.addParentID(tissueSample.getCode());
              proteinSample.addProperty("Q_EXTERNALDB_ID", sampleKey);
              proteinToSample.put(sampleKey, proteinSample);
              proteinSample.addProperty("Q_SAMPLE_TYPE", "PROTEINS");

              ProteinPeptidePreparationProperties props =
                  new ProteinPeptidePreparationProperties(enzymes, digestType, "PROTEINS",
                      cleanProt, fracType, enrichType, isoLabelType, sampPrepType);
              String prepExpID = Integer.toString(props.hashCode());
              SamplePrepPropertiesToID.put(props, prepExpID);
              proteinSample.setExperiment(prepExpID);

              // TODO compare: fractionation/enrichment, check pooling
              // if sample is digested, the peptide sample will relate to the sample name
              if (!digestType.isEmpty()) {
                String peptideID = sampleKey;
                TSVSampleBean peptideSample = peptideToSample.get(peptideID);
                if (peptideSample == null) {
                  sampleID++;
                  System.err.println("647");
                  System.err.println(sampleID);
                  peptideSample = new TSVSampleBean(Integer.toString(sampleID),
                      SampleType.Q_TEST_SAMPLE, sampleAltName,
                      fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
                  samplesInOrder.get(MassSpecSampleHierarchy.Peptides).add(peptideSample);
                  peptideSample.addParentID(proteinSample.getCode());
                  peptideSample.addProperty("Q_EXTERNALDB_ID", peptideID);
                  peptideToSample.put(peptideID, peptideSample);
                  peptideSample.addProperty("Q_SAMPLE_TYPE", "PEPTIDES");

                  props = new ProteinPeptidePreparationProperties(enzymes, digestType, "PEPTIDES",
                      cleanPept, fracType, enrichType, isoLabelType, sampPrepType);
                  prepExpID = Integer.toString(props.hashCode());
                  SamplePrepPropertiesToID.put(props, prepExpID);
                  peptideSample.setExperiment(prepExpID);
                }
                // peptide sample is parent of ms run irrespective of being newly created or already
                // existing (multiple measurements of existing samples)
                if (fracName.isEmpty()) {
                  msRun.addParentID(peptideSample.getCode());
                }
              }

            }
            // if no peptide sample needed, protein sample is parent of ms run irrespective of being
            // newly created or already existing (multiple measurements of existing samples)
            if (digestType.isEmpty() && fracName.isEmpty()) {
              msRun.addParentID(proteinSample.getCode());
            }
          }
          if (!fracName.isEmpty()) {
            // find parent of fraction by sampleName, starting at the lowest possible hierachy
            // (peptides of pools)
            String parentID = null;
            boolean parentPeptides = false;
            if (peptPoolToSample.containsKey(sampleKey)) {
              parentID = peptPoolToSample.get(sampleKey).getCode();
              parentPeptides = true;
            } else if (protPoolToSample.containsKey(sampleKey)) {
              parentID = protPoolToSample.get(sampleKey).getCode();
            } else if (peptideToSample.containsKey(sampleKey)) {
              parentID = peptideToSample.get(sampleKey).getCode();
              parentPeptides = true;
            } else if (proteinToSample.containsKey(sampleKey)) {
              parentID = proteinToSample.get(sampleKey).getCode();
            }
            if (parentID == null) {
              error = String.format(
                  "Error in line %s: Sample identifier %s used in fractionation/enrichment %s must be defined in previous line.",
                  rowID, sampleKey, fracName);
              return null;
            }

            sampleID++;
            System.err.println("695");
            System.err.println(sampleID);
            TSVSampleBean fracSample =
                new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE, fracName,
                    fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
            fracSample.addProperty("Q_EXTERNALDB_ID", fracName);

            ProteinPeptidePreparationProperties props =
                new ProteinPeptidePreparationProperties(enzymes, digestType, "PROTEINS", cleanProt,
                    fracType, enrichType, isoLabelType, sampPrepType);
            String prepExpID = Integer.toString(props.hashCode());
            SamplePrepPropertiesToID.put(props, prepExpID);
            fracSample.setExperiment(prepExpID);

            String analyte = "PROTEINS";
            if (parentPeptides) {
              analyte = "PEPTIDES";
              fracPepToSample.put(fracName, fracSample);
              samplesInOrder.get(MassSpecSampleHierarchy.FractionedPeptides).add(fracSample);
            } else {
              fracProtToSample.put(fracName, fracSample);
              samplesInOrder.get(MassSpecSampleHierarchy.FractionedProteins).add(fracSample);
            }
            fracSample.addProperty("Q_SAMPLE_TYPE", analyte);
            fracSample.addParentID(parentID);

            // a new digestion sample is needed when digest parameters are specified
            // and parent sample is not already peptide sample
            if (!digestType.isEmpty() && !parentPeptides) {
              sampleID++;
              TSVSampleBean digestedFrac =
                  new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE, fracName,
                      fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
              System.err.println("728");
              System.err.println(sampleID);
              digestedFrac.addProperty("Q_EXTERNALDB_ID", fracName);
              digestedFrac.addProperty("Q_SAMPLE_TYPE", "PEPTIDES");
              digestedFrac.addParentID(fracSample.getCode());
              fracPepToSample.put(fracName, digestedFrac);
              samplesInOrder.get(MassSpecSampleHierarchy.FractionedPeptides).add(digestedFrac);

              props = new ProteinPeptidePreparationProperties(enzymes, digestType, "PEPTIDES",
                  cleanPept, fracType, enrichType, isoLabelType, sampPrepType);
              prepExpID = Integer.toString(props.hashCode());
              SamplePrepPropertiesToID.put(props, prepExpID);
              digestedFrac.setExperiment(prepExpID);

              msRun.addParentID(digestedFrac.getCode());
            } else {
              msRun.addParentID(fracSample.getCode());
            }
          }
        } // end non-pooled block
      }

    }
    experimentInfos = new HashMap<String, List<Map<String, Object>>>();

    // Sample preparation experiments
    List<Map<String, Object>> samplePreparations = new ArrayList<Map<String, Object>>();
    for (ProteinPeptidePreparationProperties props : SamplePrepPropertiesToID.keySet()) {
      Map<String, Object> propMap = props.getPropertyMap();
      propMap.put("Code", Integer.toString(props.hashCode()));// used to match samples to their
      // experiments later
      samplePreparations.add(propMap);
    }
    experimentInfos.put("Q_SAMPLE_PREPARATION", samplePreparations);

    // MS experiments
    List<Map<String, Object>> msExperiments = new ArrayList<Map<String, Object>>();
    for (MSProperties expProperties : msPropertiesToID.keySet()) {
      Map<String, Object> propMap = expProperties.getPropertyMap();
      propMap.put("Code", msPropertiesToID.get(expProperties));
      msExperiments.add(propMap);
    }
    experimentInfos.put("Q_MS_MEASUREMENT", msExperiments);
    for (MassSpecSampleHierarchy level : order) {
      beans.addAll(samplesInOrder.get(level));
      // printSampleLevel(samplesInOrder.get(level));
    }
    return beans;
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
