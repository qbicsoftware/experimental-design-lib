package life.qbic.expdesign.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.expdesign.model.LigandomicsExperimentProperties;
import life.qbic.expdesign.model.OpenbisPropertyCodes;
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

public class MHCLigandDesignReader implements IExperimentalDesignReader {

  private List<String> mandatoryColumns;
  private List<String> mandatoryFilled;
  private List<String> optionalCols;
  private Map<SampleType, Map<String, String>> headersToTypeCodePerSampletype;
  private Map<String, Set<String>> parsedCategoriesToValues;
  private String msSampleXML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> <qproperties> <qfactors> <qcategorical label=\"technical_replicate\" value=\"%repl\"/> <qcategorical label=\"workflow_type\" value=\"%wftype\"/> </qfactors> </qproperties>";


  // private ExperimentalDesignType designType;

  private String error;
  private Map<ExperimentType, List<Map<String, Object>>> experimentInfos;
  private Set<String> speciesSet;
  private Set<String> tissueSet;
  private List<String> tsvByRows;

  private static final Logger logger = LogManager.getLogger(MHCLigandDesignReader.class);

  private Map<String, String> amountCodeMap = new HashMap<String, String>() {
    /**
     * 
     */
    private static final long serialVersionUID = -1689329489943865332L;

    {
      put(LigandomicsExperimentProperties.Cell_Count.label, OpenbisPropertyCodes.Q_CELL_COUNT.name());
      put(LigandomicsExperimentProperties.Sample_Mass.label, OpenbisPropertyCodes.Q_SAMPLE_MASS.name());
      put(LigandomicsExperimentProperties.Sample_Volume.label, OpenbisPropertyCodes.Q_SAMPLE_VOLUME.name());
    };
  };

  private Map<String, String[]> antibodyToMHCClass = new HashMap<String, String[]>() {
    /**
     * 
     */
    private static final long serialVersionUID = 1442236401110900500L;

    {
      put("MAE", new String[] {"MHC_CLASS_I", "MHC_CLASS_II"});
      put("L243", new String[] {"MHC_CLASS_II"});
      put("L243_TUE39", new String[] {"MHC_CLASS_II"});
      put("BB7.2", new String[] {"MHC_CLASS_I"});
      put("B1.23.2", new String[] {"MHC_CLASS_I"});
      put("TUE39", new String[] {"MHC_CLASS_II"});
      put("W6-32", new String[] {"MHC_CLASS_I"});
      put("GAPA3", new String[] {"MHC_CLASS_I"});
      // Mouse MHC (H-2)
      put("B22.249", new String[] {"MHC_CLASS_I"});// H2-Db
      put("Y3", new String[] {"MHC_CLASS_I"});// H2-Kb
      put("Y3_B22.249", new String[] {"MHC_CLASS_I"});//H2-Db+H2-Kb
      put("M5.144.15.2", new String[] {"MHC_CLASS_II"});// H2-Ab
    };
  };
  private HashMap<String, Command> parsers;

  public String[] getMHCClass(String antibody) {
    if (antibodyToMHCClass.containsKey(antibody))
      return antibodyToMHCClass.get(antibody);
    else {
      error = antibody + " is an unknown antibody. Please make sure to only use pre-defined antibodies.";
      return null;
    }
  }
  
  public MHCLigandDesignReader() {
    this.mandatoryColumns = new ArrayList<>();
    this.mandatoryColumns.add(LigandomicsExperimentProperties.Organism.label);
    this.mandatoryColumns.add(LigandomicsExperimentProperties.Patient_ID.label);
    this.mandatoryColumns.add(LigandomicsExperimentProperties.Tissue.label);
    this.mandatoryColumns.add(LigandomicsExperimentProperties.Antibody.label);
    this.mandatoryColumns.add(LigandomicsExperimentProperties.Antibody_Mass.label);
    this.mandatoryColumns.add(LigandomicsExperimentProperties.Prep_Date.label);
    this.mandatoryColumns.add(LigandomicsExperimentProperties.MS_Run_Date.label);
    this.mandatoryColumns.add(LigandomicsExperimentProperties.File_Name.label);
    this.mandatoryColumns.add(LigandomicsExperimentProperties.HLA_Typing.label);
    this.mandatoryColumns.add(LigandomicsExperimentProperties.Share.label);
    this.mandatoryColumns.add(LigandomicsExperimentProperties.MS_Device.label);
    this.mandatoryColumns.add(LigandomicsExperimentProperties.LCMS_Method.label);
    this.mandatoryColumns.add(LigandomicsExperimentProperties.Replicate.label);
    this.mandatoryColumns.add(LigandomicsExperimentProperties.Workflow_Type.label);

    this.mandatoryFilled = new ArrayList<>();
    this.mandatoryFilled.add(LigandomicsExperimentProperties.Organism.label);
    this.mandatoryFilled.add(LigandomicsExperimentProperties.Patient_ID.label);
    this.mandatoryFilled.add(LigandomicsExperimentProperties.Tissue.label);
    this.mandatoryFilled.add(LigandomicsExperimentProperties.Antibody.label);
    this.mandatoryFilled.add(LigandomicsExperimentProperties.MS_Run_Date.label);
    this.mandatoryFilled.add(LigandomicsExperimentProperties.File_Name.label);

    this.optionalCols = new ArrayList<>();
    this.optionalCols.add(LigandomicsExperimentProperties.Sample_Mass.label);
    this.optionalCols.add(LigandomicsExperimentProperties.Cell_Count.label);
    this.optionalCols.add(LigandomicsExperimentProperties.Sample_Volume.label);
    this.optionalCols.add(LigandomicsExperimentProperties.HLA_Typing.label);
    this.optionalCols.add(LigandomicsExperimentProperties.MS_Comment.label);
    this.optionalCols.add(LigandomicsExperimentProperties.Cell_Type.label);
    this.optionalCols.add(LigandomicsExperimentProperties.Tumor_Type.label);
    this.optionalCols.add(LigandomicsExperimentProperties.Sequencing.label);

    Map<String, String> sourceMetadata = new HashMap<>();
    sourceMetadata.put(LigandomicsExperimentProperties.Organism.label, OpenbisPropertyCodes.Q_NCBI_ORGANISM.name());
    sourceMetadata.put(LigandomicsExperimentProperties.Patient_ID.label, OpenbisPropertyCodes.Q_EXTERNALDB_ID.name());
    sourceMetadata.put(LigandomicsExperimentProperties.Source_Comment.label, OpenbisPropertyCodes.Q_ADDITIONAL_INFO.name());
    sourceMetadata.put(LigandomicsExperimentProperties.Other_Data_Reference.label, OpenbisPropertyCodes.Q_ADDITIONAL_DATA_INFO.name());

    Map<String, String> extractMetadata = new HashMap<>();
    extractMetadata.put(LigandomicsExperimentProperties.Tissue.label, OpenbisPropertyCodes.Q_PRIMARY_TISSUE.name());
    extractMetadata.put(LigandomicsExperimentProperties.Tissue_Comment.label, OpenbisPropertyCodes.Q_ADDITIONAL_INFO.name());
    extractMetadata.put(LigandomicsExperimentProperties.Detailed_Tissue.label, OpenbisPropertyCodes.Q_TISSUE_DETAILED.name());
    extractMetadata.put(LigandomicsExperimentProperties.Dignity.label, OpenbisPropertyCodes.Q_DIGNITY.name());
    extractMetadata.put(LigandomicsExperimentProperties.Cell_Type.label, OpenbisPropertyCodes.Q_TISSUE_DETAILED.name());
    extractMetadata.put(LigandomicsExperimentProperties.Tumor_Type.label, OpenbisPropertyCodes.Q_TUMOR_TYPE.name());
    extractMetadata.put(LigandomicsExperimentProperties.Location.label, OpenbisPropertyCodes.Q_TISSUE_LOCATION.name());
    extractMetadata.put(LigandomicsExperimentProperties.TNM.label, OpenbisPropertyCodes.Q_TUMOR_STAGE.name());
    extractMetadata.put(LigandomicsExperimentProperties.Metastasis.label, OpenbisPropertyCodes.Q_IS_METASTASIS.name());

    Map<String, String> ligandsMetadata = new HashMap<>();
    ligandsMetadata.put(LigandomicsExperimentProperties.Antibody.label, OpenbisPropertyCodes.Q_ANTIBODY.name());
    ligandsMetadata.put(LigandomicsExperimentProperties.MHC_Class.label, OpenbisPropertyCodes.Q_MHC_CLASS.name());

    headersToTypeCodePerSampletype = new HashMap<>();
    headersToTypeCodePerSampletype.put(SampleType.Q_BIOLOGICAL_ENTITY, sourceMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_BIOLOGICAL_SAMPLE, extractMetadata);
    headersToTypeCodePerSampletype.put(SampleType.Q_TEST_SAMPLE, new HashMap<>());
    headersToTypeCodePerSampletype.put(SampleType.Q_MHC_LIGAND_EXTRACT, ligandsMetadata);
  }
  private void fillParsedCategoriesToValuesForRow(Map<String, Integer> headerMapping,
      String[] row) {
    // logger.info("Collecting possible CV entries for row.");
    addValueForCategory(headerMapping, row, LigandomicsExperimentProperties.Organism.label);
    addValueForCategory(headerMapping, row, LigandomicsExperimentProperties.Tissue.label);
    addValueForCategory(headerMapping, row, LigandomicsExperimentProperties.Tumor_Type.label);
  }

  private void addValueForCategory(Map<String, Integer> headerMapping, String[] row, String cat) {
    if (headerMapping.containsKey(cat)) {
      String val = row[headerMapping.get(cat)];
      if (val != null && !val.isEmpty()) {
          if (parsedCategoriesToValues.containsKey(cat)) {
            parsedCategoriesToValues.get(cat).add(val);
          } else {
            Set<String> set = new HashSet<>();
            set.add(val);
            parsedCategoriesToValues.put(cat, set);
        }
      }
    }
  }
  public Map<ExperimentType, List<Map<String, Object>>> getExperimentInfos() {
    return experimentInfos;
  }

  public static final String UTF8_BOM = "\uFEFF";

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
    ArrayList<String[]> data = new ArrayList<>();
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
    Map<String, Integer> headerMapping = new HashMap<>();
    List<Integer> meta = new ArrayList<>();
    List<Integer> factors = new ArrayList<>();
    List<Integer> loci = new ArrayList<>();
    int numOfLevels = 5;

    ArrayList<String> found = new ArrayList<>(Arrays.asList(header));
    for (String col : mandatoryColumns) {
      if (!found.contains(col)) {
        error = "Mandatory column " + col + " not found.";
        return null;
      }
    }
    boolean hasAmountCol = false;
    if (found.contains(LigandomicsExperimentProperties.Sample_Mass.label))
      hasAmountCol = true;
    if (found.contains(LigandomicsExperimentProperties.Cell_Count.label))
      hasAmountCol = true;
    if (found.contains(LigandomicsExperimentProperties.Sample_Volume.label))
      hasAmountCol = true;
    if (!hasAmountCol) {
      error =
          "None of the columns Sample Mass, Cell Count or Sample Volume have been found. One of them has to be included.";
      return null;
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
    Map<String, TSVSampleBean> analyteToSample = new HashMap<>();
    Map<LigandPrepRun, Map<String, Object>> expIDToLigandExp =
        new HashMap<>();
    Map<MSRunCollection, Map<String, Object>> msIDToMSExp =
        new HashMap<>();
    List<TSVSampleBean> roots = new ArrayList<>();
    Set<String> speciesSet = new HashSet<>();
    Set<String> tissueSet = new HashSet<>();
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
        parsers = new HashMap<String, Command>();
        parsers.put(OpenbisPropertyCodes.Q_IS_METASTASIS.name(), new Command() {
          @Override
          public Object parse(String value) {
            return parseBoolean(value);
          }
        });
        parsers.put(OpenbisPropertyCodes.Q_ANTIBODY.name(), new Command() {
          @Override
          public Object parse(String value) {
            return parseAntibody(value);
          }
        });
        parsers.put(OpenbisPropertyCodes.Q_PREPARATION_DATE.name(), new Command() {
          @Override
          public Object parse(String value) {
            return parseDate(value);
          }
        });
        parsers.put(OpenbisPropertyCodes.Q_MEASUREMENT_FINISH_DATE.name(), new Command() {
          @Override
          public Object parse(String value) {
            return parseDate(value);
          }
        });
        parsers.put(OpenbisPropertyCodes.Q_MS_LCMS_METHOD.name(), new Command() {
          @Override
          public Object parse(String value) {
            return parseLCMSMethod(value);
          }
        });
        // mandatory fields that need to be filled to identify sources and samples
        String sourceID = row[headerMapping.get(LigandomicsExperimentProperties.Patient_ID.label)];
        String species = row[headerMapping.get(LigandomicsExperimentProperties.Organism.label)];
        String antibody = row[headerMapping.get(LigandomicsExperimentProperties.Antibody.label)];
        String tissue = row[headerMapping.get(LigandomicsExperimentProperties.Tissue.label)];
        String prepDate = row[headerMapping.get(LigandomicsExperimentProperties.Prep_Date.label)];
        String ligandExtrID = sourceID + "-" + tissue + "-" + prepDate + "-" + antibody;
        // changed from: row[headerMapping.get("Sample ID")] + antibody;
        String msRunDate = row[headerMapping.get(LigandomicsExperimentProperties.MS_Run_Date.label)];
        String fName = row[headerMapping.get(LigandomicsExperimentProperties.File_Name.label)];
        String replicate = row[headerMapping.get(LigandomicsExperimentProperties.Replicate.label)];
        String wfType = row[headerMapping.get(LigandomicsExperimentProperties.Workflow_Type.label)];
        String mhcTypes = row[headerMapping.get(LigandomicsExperimentProperties.HLA_Typing.label)];
        speciesSet.add(species);
        tissueSet.add(tissue);
        while (order.size() < numOfLevels) {
          order.add(new ArrayList<>());
        }
        // always one new measurement per row
        TSVSampleBean sampleSource = sourceIDToSample.get(sourceID);
        if (sampleSource == null) {
          sampleID++;
          sampleSource = new TSVSampleBean(Integer.toString(sampleID),
              SampleType.Q_BIOLOGICAL_ENTITY, sourceID,
              fillMetadata(header, row, meta, factors, loci, SampleType.Q_BIOLOGICAL_ENTITY));
          sampleSource.addProperty(OpenbisPropertyCodes.Q_EXTERNALDB_ID.name(), sourceID);
          roots.add(sampleSource);
          order.get(0).add(sampleSource);
          sourceIDToSample.put(sourceID, sampleSource);
          // create blood and DNA sample for hlatyping (one per sample source)
          sampleID++;
          String bloodID = sourceID + "_blood";
          TSVSampleBean blood = new TSVSampleBean(Integer.toString(sampleID),
              SampleType.Q_BIOLOGICAL_SAMPLE, bloodID, new HashMap<String, Object>());
          blood.addParentID(sourceID);
          blood.addProperty(OpenbisPropertyCodes.Q_PRIMARY_TISSUE.name(), "Blood plasma");
          blood.addProperty(OpenbisPropertyCodes.Q_EXTERNALDB_ID.name(), bloodID);
          tissueSet.add("Blood plasma");
          order.get(1).add(blood);
          sampleID++;
          TSVSampleBean dna = new TSVSampleBean(Integer.toString(sampleID),
              SampleType.Q_TEST_SAMPLE, sourceID + "_DNA", new HashMap<String, Object>());
          dna.addParentID(bloodID);
          dna.addProperty(OpenbisPropertyCodes.Q_SAMPLE_TYPE.name(), "DNA");
          dna.addProperty("MHC_I", parseMHCClass(mhcTypes, 1));
          dna.addProperty("MHC_II", parseMHCClass(mhcTypes, 2));
          order.get(2).add(dna);
        }

        String extractID = sourceID + tissue;// identifies unique tissue sample
        String prepID = extractID + " lysate";
        TSVSampleBean tissueSample = tissueToSample.get(extractID);
        if (tissueSample == null) {
          sampleID++;
          tissueSample = new TSVSampleBean(Integer.toString(sampleID),
              SampleType.Q_BIOLOGICAL_SAMPLE, extractID,
              fillMetadata(header, row, meta, factors, loci, SampleType.Q_BIOLOGICAL_SAMPLE));
          order.get(1).add(tissueSample);
          tissueSample.addParentID(sourceID);
          tissueSample.addProperty(OpenbisPropertyCodes.Q_EXTERNALDB_ID.name(), extractID);
          tissueToSample.put(extractID, tissueSample);

          sampleID++;
          TSVSampleBean analyteSample =
              new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_TEST_SAMPLE, prepID,
                  fillMetadata(header, row, meta, factors, loci, SampleType.Q_TEST_SAMPLE));
          order.get(2).add(analyteSample);
          analyteSample.addParentID(extractID);
          analyteSample.addProperty(OpenbisPropertyCodes.Q_EXTERNALDB_ID.name(), prepID);
          analyteToSample.put(prepID, tissueSample);
          analyteSample.addProperty(OpenbisPropertyCodes.Q_SAMPLE_TYPE.name(), "CELL_LYSATE");
        }
        // Ligand Extract Level (Analyte)
        TSVSampleBean ligandExtract = analyteToSample.get(ligandExtrID);

        String amountColName = getSampleAmountKeyFromRow(row, headerMapping);
        String sampleAmount = row[headerMapping.get(amountColName)];
        // Two ligand samples were prepared together (e.g. multiple antibody columns) only if
        // patient, prep date, handled tissue and sample amount (mass, vol or cell count) are the
        // same
        LigandPrepRun ligandPrepRun =
            new LigandPrepRun(sourceID, tissue, prepDate, sampleAmount + " " + amountColName);
        if (ligandExtract == null) {
          sampleID++;
          ligandExtract = new TSVSampleBean(Integer.toString(sampleID),
              SampleType.Q_MHC_LIGAND_EXTRACT, extractID,
              fillMetadata(header, row, meta, factors, loci, SampleType.Q_MHC_LIGAND_EXTRACT));
          ligandExtract.addProperty(OpenbisPropertyCodes.Q_ANTIBODY.name(), antibody);
          String[] mhcClass = getMHCClass(antibody);

          if (mhcClass == null) {
            return null;
          } else {
            ligandExtract.addProperty(OpenbisPropertyCodes.Q_MHC_CLASS.name(), mhcClass[0]);
          }
          ligandExtract.addParentID(prepID);
          ligandExtract.addProperty(OpenbisPropertyCodes.Q_EXTERNALDB_ID.name(), ligandExtrID);
          order.get(3).add(ligandExtract);
          analyteToSample.put(ligandExtrID, ligandExtract);

          ligandExtract.setExperiment(Integer.toString(ligandPrepRun.hashCode()));
          Map<String, Object> ligandExperimentMetadata = expIDToLigandExp.get(ligandPrepRun);
          if (ligandExperimentMetadata == null) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(amountCodeMap.get(amountColName), sampleAmount);
            expIDToLigandExp.put(ligandPrepRun,
                parseLigandExperimentData(row, headerMapping, metadata));
          } else
            expIDToLigandExp.put(ligandPrepRun,
                parseLigandExperimentData(row, headerMapping, ligandExperimentMetadata));
        }
        sampleID++;
        TSVSampleBean msRun = new TSVSampleBean(Integer.toString(sampleID), SampleType.Q_MS_RUN, "",
            fillMetadata(header, row, meta, factors, loci, SampleType.Q_MS_RUN));
        MSRunCollection msRuns = new MSRunCollection(ligandPrepRun, msRunDate);
        msRun.setExperiment(Integer.toString(msRuns.hashCode()));
        Map<String, Object> msExperiment = msIDToMSExp.get(msRuns);
        if (msExperiment == null)
          // TODO can we be sure that all metadata is the same if ligand prep run
          // and ms run date are the same?
          msIDToMSExp.put(msRuns,
              parseMSExperimentData(row, headerMapping, new HashMap<String, Object>()));
        msRun.addParentID(ligandExtrID);
        msRun.addProperty("File", fName);//needed to later match barcodes to filename
        String extID = fName.split("\\.")[0]; // can't be out of bounds
        msRun.addProperty(OpenbisPropertyCodes.Q_EXTERNALDB_ID.name(), extID); // file name is unique id
        msRun.addProperty(OpenbisPropertyCodes.Q_PROPERTIES.name(),
            msSampleXML.replace("%repl", replicate).replace("%wftype", wfType));
        order.get(4).add(msRun);
      }
    }
    experimentInfos = new HashMap<>();

    // mhc ligand extraction experiments
    List<Map<String, Object>> ligandExperiments = new ArrayList<>();
    for (LigandPrepRun prepRun : expIDToLigandExp.keySet()) {
      Map<String, Object> map = expIDToLigandExp.get(prepRun);
      map.put("Code", Integer.toString(prepRun.hashCode()));// used to match samples to their
      // experiments later
      ligandExperiments.add(map);
    }
    experimentInfos.put(ExperimentType.Q_MHC_LIGAND_EXTRACTION, ligandExperiments);

    // MS experiments
    List<Map<String, Object>> msExperiments = new ArrayList<>();
    for (MSRunCollection runCollection : msIDToMSExp.keySet()) {
      Map<String, Object> map = msIDToMSExp.get(runCollection);
      map.put("Code", Integer.toString(runCollection.hashCode()));// used to match samples to their
      // experiments later
      msExperiments.add(map);
    }
    experimentInfos.put(ExperimentType.Q_MS_MEASUREMENT, msExperiments);
    for (List<ISampleBean> level : order)
      beans.addAll(level);
    boolean unique = checkUniqueIDsBetweenSets(speciesSet, tissueSet);
    if (!unique)
      return null;
    this.speciesSet = speciesSet;
    this.tissueSet = tissueSet;
    return beans;
  }

  public List<String> parseMHCClass(String input, int i) {
    Set<String> classI = new HashSet<>(Arrays.asList("A", "B", "C"));
    // A*02:01;A*24:02;B*15:01;C*07:02;C*07:04;DRB1*04:01;DRB1*07:01;DQB1*03:02;DQB1*02:02
    List<String> res = new ArrayList<>();
    input = input.replaceAll("^\"|\"$", "");
    String[] alleles = input.split(";");
    for (String a : alleles) {
      String prefix = a.split("\\*")[0];
      switch (i) {
        case 1:
          if (classI.contains(prefix))
            res.add(a);
          break;
        case 2:
          if (a.startsWith("D"))
            res.add(a);
          break;
        default:
          break;
      }
    }
    return res;
  }

  /**
   * Returns key from the header which is most likely to contain sample amount information for this
   * row
   * 
   * @param row
   * @param headerMapping
   * @return
   */
  private String getSampleAmountKeyFromRow(String[] row, Map<String, Integer> headerMapping) {
    Map<String, String> amountMap = new HashMap<>();
    for (String colName : amountCodeMap.keySet()) {
      if (headerMapping.containsKey(colName))
        amountMap.put(colName, doubleOrNothing(row[headerMapping.get(colName)]));
    }
    String importantKey = "";
    // at this point one of the properties has to be in the map, as we checked for existence of the
    // columns in the beginning of the parse process
    for (String prop : amountMap.keySet()) {
      if (!amountMap.get(prop).isEmpty())
        importantKey = prop; // takes the one that isn't empty, should sample mass be preferred?
      else if (importantKey.isEmpty())
        importantKey = prop; // takes key from empty (but not null) property, if key hasn't been set
                             // at all. is overwritten by filled property if one such exists
    }
    return importantKey;
  }

  private Map<String, Object> parseLigandExperimentData(String[] row,
      Map<String, Integer> headerMapping, Map<String, Object> metadata) {
    String antibody = row[headerMapping.get(LigandomicsExperimentProperties.Antibody.label)];
    String antibodyMass = doubleOrNothing(row[headerMapping.get(LigandomicsExperimentProperties.Antibody_Mass.label)]);

    String prepDate = row[headerMapping.get(LigandomicsExperimentProperties.Prep_Date.label)];
    String abKey = OpenbisPropertyCodes.Q_MHC_ANTIBODY_COL1.name();
    String abMassKey = OpenbisPropertyCodes.Q_MHC_ANTIBODY_MASS_COL1.name();
    if (metadata.containsKey(abKey)) {
      abKey = OpenbisPropertyCodes.Q_MHC_ANTIBODY_COL2.name();
      abMassKey = OpenbisPropertyCodes.Q_MHC_ANTIBODY_MASS_COL2.name();
      if (metadata.containsKey(abKey)) {
        abKey = OpenbisPropertyCodes.Q_MHC_ANTIBODY_COL3.name();
        abMassKey = OpenbisPropertyCodes.Q_MHC_ANTIBODY_MASS_COL3.name();
      }
    }
    for (String colName : amountCodeMap.keySet()) {
      if (headerMapping.containsKey(colName)) {
        String val = doubleOrNothing(row[headerMapping.get(colName)]);
        if (!val.isEmpty())
          metadata.put(amountCodeMap.get(colName), val);
      }
    }
    if (!antibody.isEmpty())
      metadata.put(abKey, antibody);
    if (!antibodyMass.isEmpty())
      metadata.put(abMassKey, antibodyMass);
    if (!prepDate.isEmpty()) {
      metadata.put(OpenbisPropertyCodes.Q_PREPARATION_DATE.name(), parseDate(prepDate));
    }
    return metadata;
  }

  private String doubleOrNothing(String string) {
    try {
      Double.parseDouble(string);
      return string;
    } catch (NumberFormatException e) {
      return "";
    }
  }

  private Map<String, Object> parseMSExperimentData(String[] row,
      Map<String, Integer> headerMapping, HashMap<String, Object> metadata) {
    Map<String, String> designMap = new HashMap<>();
    designMap.put(LigandomicsExperimentProperties.MS_Run_Date.label, OpenbisPropertyCodes.Q_MEASUREMENT_FINISH_DATE.name());
    designMap.put(LigandomicsExperimentProperties.Share.label, OpenbisPropertyCodes.Q_EXTRACT_SHARE.name());
    designMap.put(LigandomicsExperimentProperties.MS_Device.label, OpenbisPropertyCodes.Q_MS_DEVICE.name());
    designMap.put(LigandomicsExperimentProperties.LCMS_Method.label, OpenbisPropertyCodes.Q_MS_LCMS_METHOD.name());
    designMap.put(LigandomicsExperimentProperties.MS_Comment.label, OpenbisPropertyCodes.Q_ADDITIONAL_INFO.name());
    metadata.put(OpenbisPropertyCodes.Q_CURRENT_STATUS.name(), "FINISHED");
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

  protected Object parseBoolean(String value) {
    return value.equals("1");
  }

  protected Object parseAntibody(String value) {
    return value;
  }

  protected String parseDate(String value) {
    SimpleDateFormat parser = new SimpleDateFormat("yyMMdd");
    try {
      Date date = parser.parse(value);
      SimpleDateFormat dateformat = new SimpleDateFormat("dd-MM-yyyy");
      if (date != null) {
        return dateformat.format(date);
      }
    } catch (IllegalArgumentException | ParseException e) {
      logger.warn("No valid preparation date input. Not setting Date for this experiment.");
    }
    return "";
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

  private HashMap<String, Object> fillMetadata(String[] header, String[] data, List<Integer> meta,
      List<Integer> factors, List<Integer> loci, SampleType type) {
    Map<String, String> headersToOpenbisCode = headersToTypeCodePerSampletype.get(type);
    HashMap<String, Object> res = new HashMap<>();
    if (headersToOpenbisCode != null) {
      for (int i : meta) {
        String label = header[i];
        if (!data[i].isEmpty() && headersToOpenbisCode.containsKey(label)) {
          String propertyCode = headersToOpenbisCode.get(label);
          Object val = data[i];
          if (parsers.containsKey(propertyCode))
            val = parsers.get(propertyCode).parse(data[i]);
          res.put(propertyCode, val);
        }
      }
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
  public Set<String> getAnalyteSet() {
    return new HashSet<String>(Arrays.asList("CELL_LYSATE", "DNA"));
  }

  @Override
  public List<String> getTSVByRows() {
    return tsvByRows;
  }

  @Override
  public StructuredExperiment getGraphStructure() {
    return null;
  }

  @Override
  public int countEntities(File file) throws IOException {
    // TODO Auto-generated method stub
    return -1;
  }

  @Override
  public List<TechnologyType> getTechnologyTypes() {
    return new ArrayList<TechnologyType>(
        Arrays.asList(new TechnologyType("Ligandomics"), new TechnologyType("HLA Typing")));
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
