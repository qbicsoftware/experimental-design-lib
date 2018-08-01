package life.qbic.isatab;

import java.util.HashMap;
import java.util.Map;

public final class ISAToReadable implements IKeywordToInterfaceTextMapper {

  public ISAToReadable() {}

  private final static Map<String, String> ISAToReadable = new HashMap<String, String>() {

    // TODO this should be replaced by ontology terms with relationship: has_specified_input some
    // deoxyribonucleic acid etc.
    {
      put("OBI:metabolite profiling", "Small Molecules");
      put("OBI:protein expression profiling", "Proteins");
      put("OBI:transcription profiling", "RNA");
      put("OBI:DNA sequencing", "DNA");
      put("OBI:DNA sequence variation detection", "DNA");
      put("OBI:DNA sequence feature detection", "DNA");
      put("OBI:DNA methylation profiling assay", "DNA");
      put("OBI:DNA methylation profiling assay", "DNA");
      put("unspecified species", "");
      put("unspecified organ", "");
    };
  };

  public String translate(String ISAKeyword) {
    if (ISAToReadable.containsKey(ISAKeyword))
      return ISAToReadable.get(ISAKeyword);
    return ISAKeyword;
  }
}
