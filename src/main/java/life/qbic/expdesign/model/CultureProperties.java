package life.qbic.expdesign.model;

import java.util.HashMap;
import java.util.Map;

public class CultureProperties {

  private String cultureMedium;
  private String cultureType;

  public CultureProperties(String cultureMedium) {
    super();
    this.cultureMedium = cultureMedium;
  }

  public String getCultureMedium() {
    return cultureMedium;
  }

  public String getCultureType() {
    return cultureType;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cultureMedium == null) ? 0 : cultureMedium.hashCode());
    result = prime * result + ((cultureType == null) ? 0 : cultureType.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CultureProperties other = (CultureProperties) obj;
    if (cultureMedium == null) {
      if (other.cultureMedium != null)
        return false;
    } else if (!cultureMedium.equals(other.cultureMedium))
      return false;
    if (cultureType == null) {
      if (other.cultureType != null)
        return false;
    } else if (!cultureType.equals(other.cultureType))
      return false;
    return true;
  }

  public Map<String, Object> getPropertyMap() {
    Map<String, Object> res = new HashMap<String, Object>();

    if (cultureMedium != null && !cultureMedium.isEmpty()) {
      res.put("Q_CULTURE_MEDIUM", cultureMedium);
    }
    if (cultureType != null && !cultureType.isEmpty()) {
      res.put("Q_CULTURE_TYPE", cultureType);
    }
    return res;
  }

}
