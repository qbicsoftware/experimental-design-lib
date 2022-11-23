package life.qbic.expdesign.io;


import static org.junit.Assert.assertEquals;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.bind.JAXBException;
import org.junit.Before;
import org.junit.Test;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.expdesign.SamplePreparator;

public class MSDesignReaderTest {


  private File tsv = new File(getClass().getResource("ptx/ptx_example1.tsv").getFile());
  private File checkMetadata = new File(getClass().getResource("ptx/fullmeta.tsv").getFile());
  private File altTSV = new File(getClass().getResource("ptx/ptx_example_noParents.tsv").getFile());
  private File small = new File(getClass().getResource("ptx/ptx_ex_small.tsv").getFile());
  private File big = new File(getClass().getResource("ptx/ptx_big_example.tsv").getFile());
  private File bug = new File(getClass().getResource("ptx/false1.txt").getFile());
  private File enrich_big =
      new File(getClass().getResource("ptx/changed_FBS_complex_example_phospho.txt").getFile());
  private File falseHeader = new File(getClass().getResource("ptx/wrongHeader.txt").getFile());
  private File trimmableHeader = new File(getClass().getResource("ptx/trimHeader.txt").getFile());
  private File noparents = new File(getClass().getResource("ptx/noparents.txt").getFile());
  private File noPeptides = new File(getClass().getResource("ptx/noPeptides.txt").getFile());
  private File nullPeptides = new File(getClass().getResource("ptx/null_peptides.tsv").getFile());

  @Before
  public void setUp() {}

  @Test
  public void testExperimentMetadata() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();
    p.processTSV(checkMetadata, new MSDesignReader(), false);
    System.out.println("ERROR? " + p.getError());

    Map<String, Object> samplePrepProps = new HashMap<>();
    samplePrepProps.put("Q_DIGESTION_ENZYMES", Arrays.asList("Trypsin", "Enzyme2"));
    samplePrepProps.put("Q_LABELING_METHOD", "SILAC");
    samplePrepProps.put("Q_MS_FRACTIONATION_METHOD", "Offgel");
    samplePrepProps.put("Q_MS_PURIFICATION_METHOD", "puri");
    samplePrepProps.put("Q_MS_ENRICHMENT_METHOD", "phospho");
    samplePrepProps.put("Q_SAMPLE_PREPARATION_METHOD", "prep");

    Map<String, Object> msProps = new HashMap<>();
    msProps.put("Q_MS_LCMS_METHOD", "30MIN");
    msProps.put("Q_CHROMATOGRAPHY_TYPE", "HPLC");
    msProps.put("Q_MS_DEVICE", "HFX");

    ExperimentType msType = ExperimentType.Q_MS_MEASUREMENT;
    ExperimentType prepType = ExperimentType.Q_SAMPLE_PREPARATION;
    Map<String, Map<String, Object>> prepExps =
        p.transformAndReturnSpecialExperimentsOfTypeOrNull(prepType.toString());
    Map<String, Map<String, Object>> msExps =
        p.transformAndReturnSpecialExperimentsOfTypeOrNull(msType.toString());
    for (Entry<String, Object> entry : samplePrepProps.entrySet()) {
      assert (searchExperimentsForProperty(prepExps, prepType, entry));
    }
    for (Entry<String, Object> entry : msProps.entrySet()) {
      assert (searchExperimentsForProperty(msExps, msType, entry));
    }
  }

  @Test
  public void testParentsx() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();
    p.processTSV(noparents, new MSDesignReader(), false);
    // System.err.println(p.getProcessed());
  }

  @Test
  public void testPeptides() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();
    p.processTSV(noPeptides, new MSDesignReader(), false);
    System.err.println(p.getProcessed());
  }

  private boolean searchExperimentsForProperty(
      Map<String, Map<String, Object>> specialExperimentsOfTypeOrNull, ExperimentType msType,
      Entry<String, Object> entry) {
    for (String exp : specialExperimentsOfTypeOrNull.keySet()) {
      Map<String, Object> experiment = specialExperimentsOfTypeOrNull.get(exp);
      if (experiment.containsKey(entry.getKey())) {
        if (experiment.get(entry.getKey()).equals(entry.getValue())) {
          return true;
        }
      }
    }
    System.err.println("not found " + entry);
    return false;
  }

  @Test
  public void testLabeledPooling() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();
    p.processTSV(checkMetadata, new MSDesignReader(), false);
    List<List<ISampleBean>> processed = p.getProcessed();
    for (List<ISampleBean> level : processed) {
      if (level.get(0).getType().equals(SampleType.Q_TEST_SAMPLE)) {
        for (ISampleBean s : level) {
          System.err.println(s);
        }
      }
    }
  }

  public String convert(String value, String fromEncoding, String toEncoding)
      throws UnsupportedEncodingException {
    return new String(value.getBytes(fromEncoding), toEncoding);
  }

  public String charset(String value, String charsets[]) throws UnsupportedEncodingException {
    String probe = StandardCharsets.UTF_8.name();
    for (String c : charsets) {
      Charset charset = Charset.forName(c);
      if (charset != null) {
        if (value.equals(convert(convert(value, charset.name(), probe), probe, charset.name()))) {
          return c;
        }
      }
    }
    return StandardCharsets.UTF_8.name();
  }
  
  @Test
  public void testNullProblem() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();

    p.processTSV(nullPeptides, new MSDesignReader(), false);
    System.out.println(p.getError());
    List<List<ISampleBean>> processed = p.getProcessed();
    for (List<ISampleBean> level : processed) {
      if (level.get(0).getType().equals(SampleType.Q_TEST_SAMPLE)) {
        for (ISampleBean s : level) {
          System.err.println(s);
        }
      }
    }
  }
  

  @Test
  public void testColProblem() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();
    File cols = new File(getClass().getResource("col_problems.txt").getFile());
    BufferedReader reader = new BufferedReader(new FileReader(cols));
    ArrayList<String[]> data = new ArrayList<String[]>();
    String next;
    int i = 0;
    // isPilot = false;

    String charsets[] =
        new String[] {StandardCharsets.UTF_16.name(), StandardCharsets.UTF_16BE.name(),
            StandardCharsets.UTF_16LE.name(), StandardCharsets.UTF_8.name()};

    while ((next = reader.readLine()) != null) {
      System.out.println(next);
      System.out.println(charset(next, charsets));
//      System.out.println(convert(next, StandardCharsets.UTF_16BE.name(), StandardCharsets.UTF_8.name()));
    }
    System.err.println("START");
    System.err.println("START");

    p.processTSV(cols, new MSDesignReader(), false);
    System.out.println(p.getError());
    List<List<ISampleBean>> processed = p.getProcessed();
    for (List<ISampleBean> level : processed) {
      if (level.get(0).getType().equals(SampleType.Q_TEST_SAMPLE)) {
        for (ISampleBean s : level) {
          System.err.println(s);
        }
      }
    }
  }

   @Test
   public void testSampleMetadata() throws IOException, JAXBException {
   SamplePreparator p = new SamplePreparator();
   p.processTSV(checkMetadata, new MSDesignReader(), false);
   List<List<ISampleBean>> processed = p.getProcessed();
   Map<String, String> entityMetadata = new HashMap<>();
   entityMetadata.put("Q_NCBI_ORGANISM", "Homo sapiens");
   entityMetadata.put("Q_EXPRESSION_SYSTEM", "E. coli");
  
   Map<String, String> tissueMetadata = new HashMap<>();
   tissueMetadata.put("Q_PRIMARY_TISSUE", "Liver");
   tissueMetadata.put("Q_ADDITIONAL_INFO", "customer");
  
   Map<String, String> proteinMetadata = new HashMap<>();
   proteinMetadata.put("Q_SAMPLE_TYPE", "PROTEINS");
   proteinMetadata.put("Q_MOLECULAR_LABEL", "medium");
  
   Map<String, String> peptideMetadata = new HashMap<>();
   proteinMetadata.put("Q_SAMPLE_TYPE", "PEPTIDES");
   proteinMetadata.put("Q_MOLECULAR_LABEL", "medium");
  
   Map<String, String> msRunMetadata = new HashMap<>();
   msRunMetadata.put("Q_ADDITIONAL_INFO", "facility");
   msRunMetadata.put("Q_INJECTION_VOLUME", "10");
  
   for (Entry<String, String> entry : entityMetadata.entrySet()) {
   assert (searchSamplesForProperty(processed, SampleType.Q_BIOLOGICAL_ENTITY, entry));
   }
   for (Entry<String, String> entry : tissueMetadata.entrySet()) {
   assert (searchSamplesForProperty(processed, SampleType.Q_BIOLOGICAL_SAMPLE, entry));
   }
   for (Entry<String, String> entry : proteinMetadata.entrySet()) {
   assert (searchSamplesForProperty(processed, SampleType.Q_TEST_SAMPLE, entry));
   }
   for (Entry<String, String> entry : peptideMetadata.entrySet()) {
   assert (searchSamplesForProperty(processed, SampleType.Q_TEST_SAMPLE, entry));
   }
   System.err.println(msRunMetadata);
   for (Entry<String, String> entry : msRunMetadata.entrySet()) {
   assert (searchSamplesForProperty(processed, SampleType.Q_MS_RUN, entry));
   }
   }
  
   private boolean searchSamplesForProperty(List<List<ISampleBean>> hierarchy, SampleType type,
   Entry<String, String> entry) {
   for (List<ISampleBean> level : hierarchy) {
   if (level.get(0).getType().equals(type)) {
   for (ISampleBean s : level) {
   if (s.getMetadata().containsKey(entry.getKey())) {
   if (s.getMetadata().get(entry.getKey()).equals(entry.getValue())) {
   return true;
   }
   }
   }
   }
   }
   System.err.println("not found " + entry + " in " + hierarchy);
   return false;
   }

   @Test
   public void testCountEntities() throws IOException {
   MSDesignReader r = new MSDesignReader();
   assertEquals(r.countEntities(tsv), -1);// not implemented
   }
  
   @Test
   public void testTrimHeader() throws IOException {
   MSDesignReader r = new MSDesignReader();
   r.readSamples(trimmableHeader, false);
   assert (r.getError() == null);
   r.readSamples(falseHeader, false);
   assert (!r.getError().isEmpty());
   assert (r.getError().contains("Organism ID"));
   }

   @Test
   public void testFactors() throws IOException, JAXBException {
   System.err.println("XXXXX");
   System.err.println("XXXXX");
   System.err.println("XXXXX");
   SamplePreparator p = new SamplePreparator();
   p.processTSV(bug, new MSDesignReader(), false);
   for(List<ISampleBean> lvl : p.getProcessed()) {
   System.err.println("______");
   for(ISampleBean b : lvl) {
   System.out.println(b.getCode());
   System.out.println(b.getSecondaryName());
   }
   }
   System.err.println("XXXXX");
   System.err.println("XXXXX");
   System.err.println("XXXXX");
   }

   @Test
   public void testGetVocabularyValues() throws IOException, JAXBException {
   MSDesignReader r = new MSDesignReader();
   SamplePreparator p = new SamplePreparator();
   p.processTSV(tsv, new MSDesignReader(), false);
   System.out.println("vocabs");
   System.out
   .println(p.getParsedValuesForCategories(new ArrayList<>(Arrays.asList("LC Column",
   "MS Device", "Fractionation Type", "Enrichment Type", "Labeling Type", "LCMS Method",
   "Digestion Method", "Digestion Enzyme", "Sample Preparation", "Species", "Tissue"))));
  
   if (r.getError() != null)
   System.out.println(r.getError());
   }
  
   @Test
   public void testParents() throws IOException, JAXBException {
   MSDesignReader r = new MSDesignReader();
   List<ISampleBean> samples1 = r.readSamples(small, false);
  
    System.out.println("error: ");
    System.out.println(r.getError());
   
    for (ISampleBean s : samples1) {
    System.out.println("code " + s.getCode());
    System.out.println("parents " + s.getParentIDs());
    System.out.println(s);
    }
   }

  @Test
  public void testReadSamples() throws IOException, JAXBException {
    MSDesignReader r = new MSDesignReader();

    List<ISampleBean> samples1 = r.readSamples(tsv, false);

    if (r.getError() != null)
      System.err.println("2" + r.getError());

    r = new MSDesignReader();
    List<ISampleBean> samples2 = r.readSamples(tsv, true);

    assertEquals(samples1, samples2);

    SamplePreparator p = new SamplePreparator();
    p.processTSV(tsv, new MSDesignReader(), false);
    System.err.print(p.getSummary());
    assert (p.getSummary().size() == 7);
    List<List<ISampleBean>> levels = p.getProcessed();
    assertEquals(levels.get(0).size(), 1);// organism
    assertEquals(levels.get(1).size(), 2);// tissue
    assertEquals(levels.get(2).size(), 6);// proteins
    assertEquals(levels.get(3).size(), 5);// peptides
    assertEquals(levels.get(4).size(), 6);// pool of peptides + fractions of peptides
    assertEquals(levels.get(5).size(), 4);// fractions of pool, pool of other peptide fractions
    assertEquals(levels.get(6).size(), 11);// ms measurements
    assertEquals(samples1.size(), 35);
    for (List<ISampleBean> l : levels) {
      System.out.println("level");
      System.out.println(l);
    }

    p.processTSV(altTSV, new MSDesignReader(), false);
    assert (p.getSummary().size() == 7);
    levels = p.getProcessed();
    assertEquals(levels.get(0).size(), 1);// organism
    assertEquals(levels.get(1).size(), 2);// tissue
    assertEquals(levels.get(2).size(), 4);// proteins
    assertEquals(levels.get(3).size(), 3);// peptides (unpooled samples) + protein pool
    assertEquals(levels.get(4).size(), 6);// pool of peptides + fractions of peptides
    assertEquals(levels.get(5).size(), 4);// fractions of pool, pool of other fractions
    assertEquals(levels.get(6).size(), 9);// ms measurements
    assertEquals(samples1.size(), 35);

    assert (p.getSpeciesSet().contains("Homo sapiens"));
    assert (p.getTissueSet().contains("Liver"));
    assert (p.getTissueSet().contains("Whole blood"));
  }

  @Test
  public void testGetGraphStructure() throws IOException, JAXBException {
    MSDesignReader r = new MSDesignReader();
    r.readSamples(tsv, true);
    assertEquals(r.getGraphStructure(), null);
    // TODO
  }

  @Test
  public void testGetTSVByRows() throws IOException, JAXBException {
    MSDesignReader r = new MSDesignReader();
    r.readSamples(tsv, true);
    // for (String l : r.getTSVByRows()) {
    // System.out.println(l);
    // }
    assertEquals(r.getTSVByRows().size(), 12);
  }

  public void testGetSpeciesSet() throws IOException {
    MSDesignReader r = new MSDesignReader();
    r.readSamples(tsv, false);
    assertEquals(r.getSpeciesSet(), new HashSet<String>(Arrays.asList("Homo Sapiens")));
  }

  public void testGetTissueSet() throws IOException {
    MSDesignReader r = new MSDesignReader();
    r.readSamples(tsv, false);
    assertEquals(r.getTissueSet(), new HashSet<String>(Arrays.asList("Whole blood")));
  }

}
