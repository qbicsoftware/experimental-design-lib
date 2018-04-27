package life.qbic.expdesign;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates vocabulary values of parsed designs
 * 
 * @author Andreas Friedrich
 *
 */
public class VocabularyValidator {

  private Map<String, Set<String>> vocabularies;
  private String error = "";

  public VocabularyValidator(Map<String, Set<String>> vocabularies) {
    this.vocabularies = vocabularies;
  }

  public String getError() {
    return error;
  }

  public boolean validateExperimentMetadata(List<Map<String, Object>> metadataList) {
    for (Map<String, Object> experimentProperties : metadataList) {
      for (String propertyName : experimentProperties.keySet()) {
        if (vocabularies.containsKey(propertyName)) {
          Set<String> vocabulary = vocabularies.get(propertyName);
          Object property = experimentProperties.get(propertyName);
          if (!vocabulary.contains(property)) {
            error = "Property " + property + " is not a valid value for " + propertyName;
            return false;
          }
        }
      }
    }
    return true;
  }

}
