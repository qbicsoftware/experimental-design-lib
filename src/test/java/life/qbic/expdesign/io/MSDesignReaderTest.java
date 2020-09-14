package life.qbic.expdesign.io;


import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.junit.Before;
import org.junit.Test;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.expdesign.model.MassSpecSampleHierarchy;

public class MSDesignReaderTest {


  private File tsv = new File(getClass().getResource("ptx_example1.tsv").getFile());

  @Before
  public void setUp() {}

  @Test
  public void testCountEntities() throws IOException {
    MSDesignReader r = new MSDesignReader();
    assertEquals(r.countEntities(tsv), -1);// not implemented
  }

  @Test
  public void testReadSamples() throws IOException, JAXBException {
    MSDesignReader r = new MSDesignReader();
    List<ISampleBean> samples1 = r.readSamples(tsv, false);

    if (r.getError() != null)
      System.out.println(r.getError());

    r = new MSDesignReader();
    List<ISampleBean> samples2 = r.readSamples(tsv, true);

    assertEquals(samples1, samples2);
    assertEquals(samples1.size(), 171);// TODO
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
    for (String l : r.getTSVByRows()) {
      System.out.println(l);
    }
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
