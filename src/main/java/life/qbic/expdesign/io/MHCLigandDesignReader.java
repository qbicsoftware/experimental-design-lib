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


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import life.qbic.datamodel.ms.LigandPrepRun;
import life.qbic.datamodel.ms.MSRunCollection;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleSummary;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.xml.properties.Unit;

public class MHCLigandDesignReader implements IExperimentalDesignReader {

  private List<String> mandatoryColumns;
  private List<String> mandatoryFilled;
  private List<String> optionalCols;
  private final List<String> sampleTypesInOrder =
      new ArrayList<String>(Arrays.asList("Q_BIOLOGICAL_ENTITY", "Q_BIOLOGICAL_SAMPLE",
          "Q_TEST_SAMPLE", "Q_MHC_LIGAND_EXTRACT", "Q_NGS_SINGLE_SAMPLE_RUN", "Q_MS_RUN"));
  private Map<String, Map<String, String>> headersToTypeCodePerSampletype;
  private String msSampleXML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> <qproperties> <qfactors> <qcategorical label=\"technical_replicate\" value=\"%repl\"/> <qcategorical label=\"workflow_type\" value=\"%wftype\"/> </qfactors> </qproperties>";


  // private ExperimentalDesignType designType;

  private String error;
  private Map<String, List<Map<String, Object>>> experimentInfos;
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
      put("Cell Count", "Q_CELL_COUNT");
      put("Sample Mass", "Q_SAMPLE_MASS");
      put("Sample Volume", "Q_SAMPLE_VOLUME");
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
      put("M5.144.15.2", new String[] {"MHC_CLASS_II"});// H2-Ab
    };
  };
  private HashMap<String, Command> parsers;

  public String[] getMHCClass(String antibody) {
    if (antibodyToMHCClass.containsKey(antibody))
      return antibodyToMHCClass.get(antibody);
    else {
      logger.error(antibody + " is an unknown antibody. Returning 'null' as MHC Class");
      return null;
    }
  }

  public MHCLigandDesignReader() {
    this.mandatoryColumns = new ArrayList<String>(Arrays.asList("Organism", "Patient ID", "Tissue",
        "Antibody", "Prep Date", "MS Run Date", "Filename", "HLA Typing", "Share",
        "MS Device", "LCMS Method", "Replicate", "Workflow Type"));
    this.mandatoryFilled = new ArrayList<String>(Arrays.asList("Organism", "Patient ID", "Tissue",
        "Antibody", "MS Run Date", "Filename"));
    this.optionalCols = new ArrayList<String>(Arrays.asList("Sample Mass", "Cell Count", "Sample Volume",
        "Antibody Mass", "HLA Typing", "MS Comment", "Cell Type", "Tumor Type", "Sequencing"));

    Map<String, String> sourceMetadata = new HashMap<String, String>();
    sourceMetadata.put("Organism", "Q_NCBI_ORGANISM");
    sourceMetadata.put("Patient ID", "Q_EXTERNALDB_ID");
    sourceMetadata.put("Source Comment", "Q_ADDITIONAL_INFO");
    sourceMetadata.put("Other Data", "Q_ADDITIONAL_DATA_INFO");

    Map<String, String> extractMetadata = new HashMap<String, String>();
    extractMetadata.put("Tissue", "Q_PRIMARY_TISSUE");
    // extractMetadata.put("Extract ID", "Q_EXTERNALDB_ID");
    extractMetadata.put("Tissue Comment", "Q_ADDITIONAL_INFO");
    extractMetadata.put("Detailed Tissue", "Q_TISSUE_DETAILED");
    extractMetadata.put("Dignity", "Q_DIGNITY");
    extractMetadata.put("Cell Type", "Q_TISSUE_DETAILED");
    extractMetadata.put("Tumor Type", "Q_TUMOR_TYPE");
    extractMetadata.put("Location", "Q_TISSUE_LOCATION");
    extractMetadata.put("TNM", "Q_TUMOR_STAGE");
    extractMetadata.put("Metastasis", "Q_IS_METASTASIS");

    Map<String, String> ligandsMetadata = new HashMap<String, String>();
    ligandsMetadata.put("Antibody", "Q_ANTIBODY");
    ligandsMetadata.put("MHC Class", "Q_MHC_CLASS");

    // Map<String, String> msRunMetadata = new HashMap<String, String>();
    // msRunMetadata.put("", "");


    headersToTypeCodePerSampletype = new HashMap<String, Map<String, String>>();
    headersToTypeCodePerSampletype.put("Q_BIOLOGICAL_ENTITY", sourceMetadata);
    headersToTypeCodePerSampletype.put("Q_BIOLOGICAL_SAMPLE", extractMetadata);
    headersToTypeCodePerSampletype.put("Q_TEST_SAMPLE", new HashMap<String, String>());
    headersToTypeCodePerSampletype.put("Q_MHC_LIGAND_EXTRACT", ligandsMetadata);
    // headersToTypeCodePerSampletype.put("Q_MS_RUN", msRunMetadata);
  }

  public Map<String, List<Map<String, Object>>> getExperimentInfos() {
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
    int numOfLevels = 5;

    ArrayList<String> found = new ArrayList<String>(Arrays.asList(header));
    for (String col : mandatoryColumns) {
      if (!found.contains(col)) {
        error = "Mandatory column " + col + " not found.";
        return null;
      }
    }
    boolean hasAmountCol = false;
    if (found.contains("Sample Mass"))
      hasAmountCol = true;
    if (found.contains("Cell Count"))
      hasAmountCol = true;
    if (found.contains("Sample Volume"))
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
    List<ISampleBean> beans = new ArrayList<ISampleBean>();
    List<List<ISampleBean>> order = new ArrayList<List<ISampleBean>>();
    Map<String, TSVSampleBean> sourceIDToSample = new HashMap<String, TSVSampleBean>();
    Map<String, TSVSampleBean> tissueToSample = new HashMap<String, TSVSampleBean>();
    Map<String, TSVSampleBean> analyteToSample = new HashMap<String, TSVSampleBean>();
    // Map<String, TSVSampleBean> ligandsToSample = new HashMap<String, TSVSampleBean>();
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
        parsers.put("Q_IS_METASTASIS", new Command() {
          @Override
          public Object parse(String value) {
            return parseBoolean(value);
          }
        });
        parsers.put("Q_ANTIBODY", new Command() {
          @Override
          public Object parse(String value) {
            return parseAntibody(value);
          }
        });
        parsers.put("Q_PREPARATION_DATE", new Command() {
          @Override
          public Object parse(String value) {
            return parseDate(value);
          }
        });
        parsers.put("Q_MEASUREMENT_FINISH_DATE", new Command() {
          @Override
          public Object parse(String value) {
            return parseDate(value);
          }
        });
        parsers.put("Q_MS_LCMS_METHOD", new Command() {
          @Override
          public Object parse(String value) {
            return parseLCMSMethod(value);
          }
        });
        // mandatory fields that need to be filled to identify sources and samples
        String sourceID = row[headerMapping.get("Patient ID")];
        String species = row[headerMapping.get("Organism")];
        String antibody = row[headerMapping.get("Antibody")];
        String tissue = row[headerMapping.get("Tissue")];
        String prepDate = row[headerMapping.get("Prep Date")];
        String ligandExtrID = sourceID + "-" + tissue + "-" + prepDate + "-" + antibody;
        // changed from: row[headerMapping.get("Sample ID")] + antibody;
        String msRunDate = row[headerMapping.get("MS Run Date")];
        String fName = row[headerMapping.get("Filename")];
        String replicate = row[headerMapping.get("Replicate")];
        String wfType = row[headerMapping.get("Workflow Type")];
        String mhcTypes = row[headerMapping.get("HLA Typing")];
        speciesSet.add(species);
        tissueSet.add(tissue);
        while (order.size() < numOfLevels) {
          order.add(new ArrayList<ISampleBean>());
        }
        // always one new measurement per row
        TSVSampleBean sampleSource = sourceIDToSample.get(sourceID);
        if (sampleSource == null) {
          sampleID++;
          sampleSource = new TSVSampleBean(Integer.toString(sampleID), "Q_BIOLOGICAL_ENTITY",
              sourceID, fillMetadata(header, row, meta, factors, loci, "Q_BIOLOGICAL_ENTITY"));
          sampleSource.addProperty("Q_EXTERNALDB_ID", sourceID);
          roots.add(sampleSource);
          order.get(0).add(sampleSource);
          sourceIDToSample.put(sourceID, sampleSource);
          // create blood and DNA sample for hlatyping (one per sample source)
          sampleID++;
          String bloodID = sourceID + "_blood";
          TSVSampleBean blood = new TSVSampleBean(Integer.toString(sampleID), "Q_BIOLOGICAL_SAMPLE",
              bloodID, new HashMap<String, Object>());
          blood.addParentID(sourceID);
          blood.addProperty("Q_PRIMARY_TISSUE", "Blood plasma");
          blood.addProperty("Q_EXTERNALDB_ID", bloodID);
          tissueSet.add("Blood plasma");
          order.get(1).add(blood);
          sampleID++;
          TSVSampleBean dna = new TSVSampleBean(Integer.toString(sampleID), "Q_TEST_SAMPLE",
              sourceID + "_DNA", new HashMap<String, Object>());
          dna.addParentID(bloodID);
          dna.addProperty("Q_SAMPLE_TYPE", "DNA");
          dna.addProperty("MHC_I", parseMHCClass(mhcTypes, 1));
          dna.addProperty("MHC_II", parseMHCClass(mhcTypes, 2));
          order.get(2).add(dna);
        }

        String extractID = sourceID + tissue;// identifies unique tissue sample
        String prepID = extractID + " lysate";
        TSVSampleBean tissueSample = tissueToSample.get(extractID);
        if (tissueSample == null) {
          sampleID++;
          tissueSample = new TSVSampleBean(Integer.toString(sampleID), "Q_BIOLOGICAL_SAMPLE",
              extractID, fillMetadata(header, row, meta, factors, loci, "Q_BIOLOGICAL_SAMPLE"));
          order.get(1).add(tissueSample);
          tissueSample.addParentID(sourceID);
          tissueSample.addProperty("Q_EXTERNALDB_ID", extractID);
          tissueToSample.put(extractID, tissueSample);

          sampleID++;
          TSVSampleBean analyteSample =
              new TSVSampleBean(Integer.toString(sampleID), "Q_TEST_SAMPLE", prepID,
                  fillMetadata(header, row, meta, factors, loci, "Q_TEST_SAMPLE"));
          order.get(2).add(analyteSample);
          analyteSample.addParentID(extractID);
          analyteSample.addProperty("Q_EXTERNALDB_ID", prepID);
          analyteToSample.put(prepID, tissueSample);
          analyteSample.addProperty("Q_SAMPLE_TYPE", "CELL_LYSATE");
        }
        // Ligand Extract Level (Analyte)
        TSVSampleBean ligandExtract = analyteToSample.get(ligandExtrID);

        String amountColName = getSampleAmountKeyFromRow(row, headerMapping);
        String sampleAmount = row[headerMapping.get(amountColName)];
        // Two ligand samples were prepared together (e.g. multiple antibody columns) only if
        // patient, prep date, handled tissue and sample amount (mass, vol or cell count) are the same
        LigandPrepRun ligandPrepRun =
            new LigandPrepRun(sourceID, tissue, prepDate, sampleAmount + " " + amountColName);
        if (ligandExtract == null) {
          sampleID++;
          ligandExtract = new TSVSampleBean(Integer.toString(sampleID), "Q_MHC_LIGAND_EXTRACT",
              extractID, fillMetadata(header, row, meta, factors, loci, "Q_MHC_LIGAND_EXTRACT"));
          ligandExtract.addProperty("Q_ANTIBODY", antibody);
          String[] mhcClass = getMHCClass(antibody);
          if (mhcClass.length == 1) {
            ligandExtract.addProperty("Q_MHC_CLASS", mhcClass[0]);
          }
          ligandExtract.addParentID(prepID);
          ligandExtract.addProperty("Q_EXTERNALDB_ID", ligandExtrID);
          order.get(3).add(ligandExtract);
          analyteToSample.put(ligandExtrID, ligandExtract);
          
          ligandExtract.setExperiment(Integer.toString(ligandPrepRun.hashCode()));
          Map<String, Object> ligandExperimentMetadata = expIDToLigandExp.get(ligandPrepRun);
          if (ligandExperimentMetadata == null) {
            Map<String, Object> metadata = new HashMap<String, Object>();
            metadata.put(amountCodeMap.get(amountColName), sampleAmount);
            expIDToLigandExp.put(ligandPrepRun,
                parseLigandExperimentData(row, headerMapping, metadata));
          } else
            expIDToLigandExp.put(ligandPrepRun,
                parseLigandExperimentData(row, headerMapping, ligandExperimentMetadata));
        }
        TSVSampleBean msRun = new TSVSampleBean(Integer.toString(sampleID), "Q_MS_RUN", "",
            fillMetadata(header, row, meta, factors, loci, "Q_MS_RUN"));
        MSRunCollection msRuns = new MSRunCollection(ligandPrepRun, msRunDate);
        msRun.setExperiment(Integer.toString(msRuns.hashCode()));
        Map<String, Object> msExperiment = msIDToMSExp.get(msRuns);
        if (msExperiment == null)
          // TODO can we be sure that all metadata is the same if ligand prep run
          // and ms run date are the same?
          msIDToMSExp.put(msRuns,
              parseMSExperimentData(row, headerMapping, new HashMap<String, Object>()));
        msRun.addParentID(ligandExtrID);
        msRun.addProperty("File", fName);

        msRun.addProperty("Q_PROPERTIES",
            msSampleXML.replace("%repl", replicate).replace("%wftype", wfType));
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
    for (List<ISampleBean> level : order)
      beans.addAll(level);
    boolean unique = checkUniqueIDsBetweenSets(speciesSet, tissueSet);
    if (!unique)
      return null;
    this.speciesSet = speciesSet;
    this.tissueSet = tissueSet;
    return beans;
  }

  private List<String> parseMHCClass(String input, int i) {
    Set<String> classI = new HashSet<String>(Arrays.asList("A", "B", "C"));
    // A*02:01;A*24:02;B*15:01;C*07:02;C*07:04;DRB1*04:01;DRB1*07:01;DQB1*03:02;DQB1*02:02
    List<String> res = new ArrayList<String>();
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
    Map<String, String> amountMap = new HashMap<String, String>();
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
    String antibody = row[headerMapping.get("Antibody")];
    String antibodyMass = doubleOrNothing(row[headerMapping.get("Antibody Mass")]);

    String prepDate = row[headerMapping.get("Prep Date")];
    String abKey = "Q_MHC_ANTIBODY_COL1";
    String abMassKey = "Q_MHC_ANTIBODY_MASS_COL1";
    if (metadata.containsKey(abKey)) {
      abKey = "Q_MHC_ANTIBODY_COL2";
      abMassKey = "Q_MHC_ANTIBODY_MASS_COL2";
      if (metadata.containsKey(abKey)) {
        abKey = "Q_MHC_ANTIBODY_COL3";
        abMassKey = "Q_MHC_ANTIBODY_MASS_COL3";
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
      metadata.put("Q_PREPARATION_DATE", parseDate(prepDate));
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
    Map<String, String> designMap = new HashMap<String, String>();
    designMap.put("MS Run Date", "Q_MEASUREMENT_FINISH_DATE");
    designMap.put("Share", "Q_EXTRACT_SHARE");
    designMap.put("MS Device", "Q_MS_DEVICE");
    designMap.put("LCMS Method", "Q_MS_LCMS_METHOD");
    designMap.put("MS Comment", "Q_ADDITIONAL_INFO");
    metadata.put("Q_CURRENT_STATUS", "FINISHED");
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
      List<Integer> factors, List<Integer> loci, String sampleType) {
    Map<String, String> headersToOpenbisCode = headersToTypeCodePerSampletype.get(sampleType);
    HashMap<String, Object> res = new HashMap<String, Object>();
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
  public Map<String, List<SampleSummary>> getSampleGraphNodes() {
    return null;
  }

  @Override
  public int countEntities(File file) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

}
