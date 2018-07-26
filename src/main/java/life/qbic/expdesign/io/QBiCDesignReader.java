package life.qbic.expdesign.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import life.qbic.datamodel.identifiers.SampleCodeFunctions;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleSummary;
import life.qbic.datamodel.samples.TSVSampleBean;
import life.qbic.expdesign.SamplePreparator;
import life.qbic.expdesign.model.ExperimentalDesignType;
import life.qbic.expdesign.model.StructuredExperiment;
import life.qbic.xml.properties.Unit;

public class QBiCDesignReader implements IExperimentalDesignReader {

  private ArrayList<String> vocabulary = new ArrayList<String>(Arrays.asList("Identifier", "SPACE",
      "EXPERIMENT", "SAMPLE TYPE", "Q_SECONDARY_NAME", "PARENT"));
  private List<String> entityMandatory = new ArrayList<String>(Arrays.asList("Q_NCBI_ORGANISM"));
  private List<String> extractMandatory = new ArrayList<String>(Arrays.asList("Q_PRIMARY_TISSUE"));
  private List<String> extractSpecials = new ArrayList<String>(Arrays.asList("Q_TISSUE_DETAILED"));
  private List<String> testMandatory = new ArrayList<String>(Arrays.asList("Q_SAMPLE_TYPE"));
  private List<String> mhcSpecials = new ArrayList<String>(Arrays.asList("Q_MHC_CLASS"));
  private List<String> sampleTypesInOrder =
      new ArrayList<String>(Arrays.asList("Q_BIOLOGICAL_ENTITY", "Q_BIOLOGICAL_SAMPLE",
          "Q_TEST_SAMPLE", "Q_MHC_LIGAND_EXTRACT", "Q_NGS_SINGLE_SAMPLE_RUN", "Q_MS_RUN"));

  private String error;
  private String description;
  private boolean isPilot;
  private String secondaryName;
  private String investigator;
  private String manager;
  private String contact;
  private String space;
  private String project;
  private Map<String, List<Map<String, Object>>> experimentInfos;
  private List<String> tsvByRows;

  private static final Logger logger = LogManager.getLogger(QBiCDesignReader.class);

  public Map<String, List<Map<String, Object>>> getExperimentInfos() {
    return experimentInfos;
  }

  public String getDescription() {
    return description;
  }

  public String getInvestigator() {
    return investigator;
  }

  public String getContact() {
    return contact;
  }

  public String getSecondaryName() {
    return secondaryName;
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
    this.space = "";
    this.project = "";
    BufferedReader reader = new BufferedReader(new FileReader(file));
    ArrayList<String[]> data = new ArrayList<String[]>();
    String next;
    int i = 0;
    boolean inDescription = false;
    isPilot = false;
    while ((next = reader.readLine()) != null) {
      i++;
      next = removeUTF8BOM(next);
      tsvByRows.add(next);
      String[] nextLine = next.split("\t", -1);// this is needed for trailing tabs
      if (nextLine[0].substring(0, 1).equals("#")) {
        String line = nextLine[0];
        if (line.contains("#PROJECT_DESCRIPTION=")) {
          inDescription = true;
          description = trySplitMetadata(line, "#PROJECT_DESCRIPTION=");
        } else if (line.contains("#PILOT PROJECT")) {// TODO experiment wise?
          inDescription = false;
          isPilot = true;
        } else if (line.contains("#ALTERNATIVE_NAME=")) {
          inDescription = false;
          secondaryName = trySplitMetadata(line, "#ALTERNATIVE_NAME=");
        } else if (line.contains("#INVESTIGATOR=")) {
          inDescription = false;
          investigator = trySplitMetadata(line, "#INVESTIGATOR=");
        } else if (line.contains("#CONTACT=")) {
          inDescription = false;
          contact = trySplitMetadata(line, "#CONTACT=");
        } else if (line.contains("#MANAGER=")) {
          inDescription = false;
          manager = trySplitMetadata(line, "#MANAGER=");
        } else if (line.startsWith("#EXP ")) {
          inDescription = false;
          if (experimentInfos == null)
            experimentInfos = new HashMap<String, List<Map<String, Object>>>();
          Map<String, Object> expInfos = new HashMap<String, Object>();
          String[] namesplt = line.split(":");
          expInfos.put("Code", namesplt[0].split(" ")[1]);// TODO renamed from "Name"
          String type = namesplt[1];
          String entries = line.substring(line.indexOf("{") + 1, line.indexOf("}"));
          for (String entry : entries.split("##")) {
            String[] splt = entry.split("=");
            String key = splt[0].trim();
            Object val = null;
            if (splt.length > 1)
              val = replaceSpecials(splt[1]);
            if (val != null) {
              if (entry.contains("#")) {
                List<String> list = new ArrayList<String>();
                for (String item : entry.split("#"))
                  list.add(replaceSpecials(item));
                val = list;
              }
              expInfos.put(key, val);
            }
          }

          if (experimentInfos.containsKey(type))
            experimentInfos.get(type).add(expInfos);
          else
            experimentInfos.put(type, new ArrayList<Map<String, Object>>(Arrays.asList(expInfos)));
        } else {
          if (inDescription)
            description += line.substring(1, line.length()) + "\n";
          else
            logger.warn("Found comment row out of context, ignoring.");
        }
      } else {
        if (data.isEmpty() || nextLine.length == data.get(0).length) {
          data.add(nextLine);
        } else {
          error = "Wrong number of columns in row " + i;
          reader.close();
          return null;
        }
      }
    }
    reader.close();
    String[] header = data.get(0);
    data.remove(0);
    // find out where the mandatory and other metadata data is
    Map<Integer, Integer> mapping = new HashMap<Integer, Integer>();
    List<Integer> meta = new ArrayList<Integer>();
    List<Integer> factors = new ArrayList<Integer>();
    List<Integer> loci = new ArrayList<Integer>();

    ArrayList<String> found = new ArrayList<String>(Arrays.asList(header));
    for (String col : vocabulary) {
      if (!found.contains(col)) {
        error = "Mandatory column " + col + " not found.";
        return null;
      }
    }
    for (i = 0; i < header.length; i++) {
      int position = vocabulary.indexOf(header[i]);
      if (position > -1) {
        mapping.put(position, i);
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
    for (String[] row : data) {
      boolean special = false;
      String code = row[mapping.get(0)];
      // Checksum is generated and added here if not there already
      code = checkOrAddChecksum(code);
      if (!SampleCodeFunctions.isQbicBarcode(code) && !isEntity(code)
          && !isMeasurementBarcode(code)) {
        if (isSpecialBarcode(code)) {
          special = true;
        } else {
          error = code + " is not a valid barcode!";
          return null;
        }
      }
      if (!special) {
        String sampleSpace = row[mapping.get(1)];
        // project code consists of the first 5 characters of the experiment
        String sampleProject = row[mapping.get(2)].substring(0, 5);
        String exp = row[mapping.get(2)];
        if (space.isEmpty() && project.isEmpty()) {
          space = sampleSpace;
          project = sampleProject;
        } else {
          if (!space.equals(sampleSpace)) {
            error = sampleSpace + " is not the expected space (" + space
                + "). Please only use one space for your project.";
            return null;
          }
          if (!project.equals(sampleProject)) {
            error = sampleProject + " is not the expected project (" + project
                + "). Please only use one project for your samples.";
            return null;
          }
          if (!exp.startsWith(project)) {
            error = exp + " does not fit the expected project code (" + project
                + "). Please only use one project for your samples and experiments.";
            return null;
          }
        }
        HashMap<String, Object> metadata = fillMetadata(header, row, meta, factors, loci);
        String type = row[mapping.get(3)];
        if (!sampleTypeCheckOk(code, type, metadata))
          return null;
        int experimentLevel = sampleTypesInOrder.indexOf(type);
        while (order.size() - 1 < experimentLevel) {
          order.add(new ArrayList<ISampleBean>());
        }
        List<String> parentIDs = parseParentCodes(row[mapping.get(5)]);
        if (parentIDs == null)
          return null;
        order.get(experimentLevel).add(new TSVSampleBean(code, exp, project, space, type,
            row[mapping.get(4)], parentIDs, metadata));
      }
    }
    for (List<ISampleBean> level : order)
      beans.addAll(level);
    return beans;
  }

  private String checkOrAddChecksum(String code) {
    if (code.length() < 9 || SampleCodeFunctions.isQbicBarcode(code) || isEntity(code)
        || isSpecialBarcode(code) || isMeasurementBarcode(code))
      return code;
    if (code.length() == 9)
      return code + SampleCodeFunctions.checksum(code);
    else {
      String main = code.substring(code.length() - 9);
      String prefix = code.replace(main, "");
      return prefix + main + SampleCodeFunctions.checksum(code);
    }
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

  private List<String> parseParentCodes(String parents) {
    List<String> res = new ArrayList<String>();
    if (!parents.isEmpty()) {
      for (String parent : parents.split(" ")) {
        parent = parent.trim();
        if (!parent.contains(project)) {
          error = "Parent sample " + parent + " does not fit expected project (" + project
              + "). Please only use one project for your samples.";
          return null;
        }
        if (parent.length() < 10) {
          // parents = parents.replace(parent, parent + SampleCodeFunctions.checksum(parent));
          parent = parent + SampleCodeFunctions.checksum(parent);
        }
        res.add(parent);
      }
    }
    return res;
  }

  /**
   * Checks if the sample type is known and if it has all associated mandatory metadata set while
   * having no metadata it is not supposed to have
   * 
   * @param code
   * @param type
   * @param metadata
   * @return
   */
  private boolean sampleTypeCheckOk(String code, String type, HashMap<String, Object> metadata) {
    if (!sampleTypesInOrder.contains(type)) {
      error = type + " is not a valid sample type!";
      return false;
    }
    List<String> blacklist = new ArrayList<String>();
    List<String> mandatory = new ArrayList<String>();
    switch (type) {
      case "Q_BIOLOGICAL_ENTITY":
        mandatory = entityMandatory;
        blacklist.addAll(extractMandatory);
        blacklist.addAll(extractSpecials);
        blacklist.addAll(testMandatory);
        blacklist.addAll(mhcSpecials);
        break;
      case "Q_BIOLOGICAL_SAMPLE":
        mandatory = extractMandatory;
        blacklist.addAll(entityMandatory);
        blacklist.addAll(testMandatory);
        blacklist.addAll(mhcSpecials);
        break;
      case "Q_TEST_SAMPLE":
        mandatory = testMandatory;
        blacklist.addAll(entityMandatory);
        blacklist.addAll(extractSpecials);
        blacklist.addAll(extractMandatory);
        blacklist.addAll(mhcSpecials);
        break;
      case "Q_MHC_LIGAND_EXTRACT":
        blacklist.addAll(extractMandatory);
        blacklist.addAll(extractSpecials);
        blacklist.addAll(testMandatory);
        blacklist.addAll(entityMandatory);
        blacklist.addAll(entityMandatory);
        break;
      case "Q_MS_RUN":
        blacklist.addAll(extractMandatory);
        blacklist.addAll(extractSpecials);
        blacklist.addAll(testMandatory);
        blacklist.addAll(entityMandatory);
        blacklist.addAll(entityMandatory);
    }
    Set<String> cols = metadata.keySet();
    for (String col : cols) {
      if (blacklist.contains(col)) {
        error = col + " is not a valid column for sample type " + type
            + ", but it is set for sample " + code + "!";
        return false;
      }
    }
    for (String col : mandatory) {
      if (!cols.contains(col)) {
        error = col + " is a mandatory field for sample type " + type
            + ", but it is not set for sample " + code + "!";
        return false;
      }
    }
    return true;
  }

  private static boolean isEntity(String code) {
    String pattern = "Q[A-Z0-9]{4}ENTITY-[0-9]+";
    return code.matches(pattern);
  }

  private boolean isMeasurementBarcode(String code) {
    String pattern3 = "VC[0-9]*Q[A-X0-9]{4}[0-9]{3}[A-X0-9]{2}";
    String pattern4 = "MS[0-9]*Q[A-X0-9]{4}[0-9]{3}[A-X0-9]{2}";
    return code.matches(pattern3) || code.matches(pattern4);
  }

  private boolean isSpecialBarcode(String code) {
    String pattern1 = "Q[A-X0-9]{4}000";
    String pattern2 = "Q[A-X0-9]{4}E[1-9][0-9]*-000";
    return code.matches(pattern1) || code.matches(pattern2);
  }

  private HashMap<String, Object> fillMetadata(String[] header, String[] data, List<Integer> meta,
      List<Integer> factors, List<Integer> loci) {
    HashMap<String, Object> res = new HashMap<String, Object>();
    for (int i : meta) {
      if (!data[i].isEmpty())
        res.put(header[i], data[i]);
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

  public boolean isPilot() {
    return isPilot;
  }

  public String getManager() {
    return manager;
  }

  // TODO best to return empty sets?
  @Override
  public Set<String> getSpeciesSet() {
    return new HashSet<String>();
  }

  @Override
  public Set<String> getTissueSet() {
    return new HashSet<String>();
  }

  @Override
  public Set<String> getAnalyteSet() {
    return new HashSet<String>();
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
  public int countEntities(File file) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }

}
