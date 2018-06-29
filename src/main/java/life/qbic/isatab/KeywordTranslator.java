package life.qbic.isatab;

import java.util.HashMap;
import java.util.Map;

public final class KeywordTranslator {

  private KeywordTranslator() {}

  private final static Map<String, String> ISAToQBiC = new HashMap<String, String>() {

    {
      put("OBI:metabolite profiling", "SMALLMOLECULES");
      put("x", "RNA");
      put("y", "DNA");
      put("z", "PROTEINS");
      put("a", "PEPTIDES");
    }

    ;
  };


  public static String getQBiCKeyword(String ISAKeyword) {
    return ISAToQBiC.get(ISAKeyword);
  }
}
