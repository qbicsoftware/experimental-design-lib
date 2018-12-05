package life.qbic.isatab;

import java.util.List;

public class ISAStudyInfos {

  private String title;
  private String description;
  private String protocol;
  private List<String> designTypes;

  public ISAStudyInfos(String studyTitle, String studyDesc, String studyProtocol,
      List<String> designTypes) {
    title = studyTitle;
    description = studyDesc;
    protocol = studyProtocol;
    this.designTypes = designTypes;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getProtocol() {
    return protocol;
  }

  public List<String> getDesignTypes() {
    return designTypes;
  }



}
