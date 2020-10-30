package life.qbic.expdesign.io;


import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;

import life.qbic.datamodel.samples.ISampleBean;

public class MHCLigandDesignReaderTest {


  private File tsv = new File(getClass().getResource("ligandomics.tsv").getFile());

  @Before
  public void setUp() {}

  @Test
  public void testCountEntities() throws IOException {
    MHCLigandDesignReader r = new MHCLigandDesignReader();
    assertEquals(r.countEntities(tsv), -1);
    // TODO
  }

  @Test
  public void testReadSamples() throws IOException, JAXBException {
    MHCLigandDesignReader r = new MHCLigandDesignReader();
    List<ISampleBean> samples1 = r.readSamples(tsv, false);

    r = new MHCLigandDesignReader();
    List<ISampleBean> samples2 = r.readSamples(tsv, true);

    assertEquals(samples1, samples2);
    assertEquals(samples1.size(), 171);
  }

  @Test
  public void testGetGraphStructure() throws IOException, JAXBException {
    MHCLigandDesignReader r = new MHCLigandDesignReader();
    r.readSamples(tsv, true);
    assertEquals(r.getGraphStructure(), null);
    // TODO
  }

  @Test
  public void testGetTSVByRows() throws IOException, JAXBException {
    MHCLigandDesignReader r = new MHCLigandDesignReader();
    r.readSamples(tsv, true);
    assertEquals(r.getTSVByRows().size(), 97);
  }

  public void testGetSpeciesSet() throws IOException {
    MHCLigandDesignReader r = new MHCLigandDesignReader();
    r.readSamples(tsv, false);
    assertEquals(r.getSpeciesSet(), new HashSet<String>(Arrays.asList("Homo Sapiens")));
  }

  public void testGetTissueSet() throws IOException {
    MHCLigandDesignReader r = new MHCLigandDesignReader();
    r.readSamples(tsv, false);
    assertEquals(r.getTissueSet(),
        new HashSet<String>(
            Arrays.asList("Kidneys", "muscle", "spleen", "small intestine", "bladder", "heart",
                "Lungs", "myelon", "thyroid", "cerebellum", "stomach", "pancreas", "skin")));
  }

}
