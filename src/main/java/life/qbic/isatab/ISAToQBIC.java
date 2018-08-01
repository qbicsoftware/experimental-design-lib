package life.qbic.isatab;

import java.util.HashMap;
import java.util.Map;

public final class KeywordTranslator {

  private KeywordTranslator() {}

  private final static Map<String, String> ISAToQBiC = new HashMap<String, String>() {

    //TODO this should be replaced by ontology terms with relationship: has_specified_input some deoxyribonucleic acid etc.
    {
      put("OBI:metabolite profiling", "SMALLMOLECULES");
      put("OBI:protein expression profiling", "PROTEINS");
      put("OBI:transcription profiling", "RNA");
      put("OBI:DNA sequencing", "DNA");
      put("OBI:DNA sequence variation detection", "DNA");
      put("OBI:DNA sequence feature detection", "DNA");
      put("OBI:DNA methylation profiling assay", "DNA");
    }

    ;
  };


  public static String getQBiCKeyword(String ISAKeyword) {
    return ISAToQBiC.get(ISAKeyword);
  }
}
