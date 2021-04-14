package life.qbic.expdesign.model;

import java.util.HashMap;
import java.util.Map;

public class ExtendedMSProperties {

  private String lcmsMethod;
  private String msDevice;
  private String lcmsDescription;
  private String chromatographyType;
  private String columnName;
  private String lcDevice;
  private String lcDetectionMethod;
  private String ionizationMode;
  private String washingSolvent;
  private String additionalInformation;

  public ExtendedMSProperties(String lcmsMethod, String msDevice) {
    super();
    this.lcmsMethod = lcmsMethod;
    this.msDevice = msDevice;
  }

  public String getLcmsMethod() {
    return lcmsMethod;
  }

  public void setLcmsMethod(String lcmsMethod) {
    this.lcmsMethod = lcmsMethod;
  }

  public String getMSDevice() {
    return msDevice;
  }

  public void setDevice(String device) {
    this.msDevice = device;
  }

  public String getLCDevice() {
    return lcDevice;
  }

  public void setLCDevice(String device) {
    this.lcDevice = device;
  }

  public String getLCMSDescription() {
    return lcmsDescription;
  }

  public void setLCMSDescription(String lcmsDescription) {
    this.lcmsDescription = lcmsDescription;
  }

  public String getChromatographyType() {
    return chromatographyType;
  }

  public void setChromatographyType(String chromatographyType) {
    this.chromatographyType = chromatographyType;
  }

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public String getLCDetectionMethod() {
    return lcDetectionMethod;
  }

  public void setLCDetectionMethod(String lcDetectionMethod) {
    this.lcDetectionMethod = lcDetectionMethod;
  }

  public String getIonizationMode() {
    return ionizationMode;
  }

  public void setIonizationMode(String ionizationMode) {
    this.ionizationMode = ionizationMode;
  }

  public String getWashingSolvent() {
    return washingSolvent;
  }

  public void setWashingSolvent(String washingSolvent) {
    this.washingSolvent = washingSolvent;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((chromatographyType == null) ? 0 : chromatographyType.hashCode());
    result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
    result = prime * result + ((msDevice == null) ? 0 : msDevice.hashCode());
    result = prime * result + ((lcDevice == null) ? 0 : lcDevice.hashCode());
    result = prime * result + ((ionizationMode == null) ? 0 : ionizationMode.hashCode());
    result = prime * result + ((lcDetectionMethod == null) ? 0 : lcDetectionMethod.hashCode());
    result = prime * result + ((lcmsDescription == null) ? 0 : lcmsDescription.hashCode());
    result = prime * result + ((lcmsMethod == null) ? 0 : lcmsMethod.hashCode());
    result = prime * result + ((washingSolvent == null) ? 0 : washingSolvent.hashCode());
    result =
        prime * result + ((additionalInformation == null) ? 0 : additionalInformation.hashCode());
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
    ExtendedMSProperties other = (ExtendedMSProperties) obj;
    if (chromatographyType == null) {
      if (other.chromatographyType != null)
        return false;
    } else if (!chromatographyType.equals(other.chromatographyType))
      return false;
    if (columnName == null) {
      if (other.columnName != null)
        return false;
    } else if (!columnName.equals(other.columnName))
      return false;
    if (lcDevice == null) {
      if (other.lcDevice != null)
        return false;
    } else if (!lcDevice.equals(other.lcDevice))
      return false;
    if (msDevice == null) {
      if (other.msDevice != null)
        return false;
    } else if (!msDevice.equals(other.msDevice))
      return false;
    if (ionizationMode == null) {
      if (other.ionizationMode != null)
        return false;
    } else if (!ionizationMode.equals(other.ionizationMode))
      return false;
    if (lcDetectionMethod == null) {
      if (other.lcDetectionMethod != null)
        return false;
    } else if (!lcDetectionMethod.equals(other.lcDetectionMethod))
      return false;
    if (lcmsDescription == null) {
      if (other.lcmsDescription != null)
        return false;
    } else if (!lcmsDescription.equals(other.lcmsDescription))
      return false;
    if (lcmsMethod == null) {
      if (other.lcmsMethod != null)
        return false;
    } else if (!lcmsMethod.equals(other.lcmsMethod))
      return false;
    if (washingSolvent == null) {
      if (other.washingSolvent != null)
        return false;
    } else if (!washingSolvent.equals(other.washingSolvent))
      return false;
    if (additionalInformation == null) {
      if (other.additionalInformation != null)
        return false;
    } else if (!additionalInformation.equals(other.additionalInformation))
      return false;
    return true;
  }

  public Map<String, Object> getPropertyMap() {
    Map<String, Object> res = new HashMap<String, Object>();
    if (msDevice != null)
      res.put("Q_MS_DEVICE", msDevice);
    if (lcDevice != null)
      res.put("Q_LC_DEVICE", lcDevice);
    if (lcmsMethod != null)
      res.put("Q_MS_LCMS_METHOD", lcmsMethod);
    if (chromatographyType != null)
      res.put("Q_CHROMATOGRAPHY_TYPE", chromatographyType);
    if (lcmsDescription != null && !lcmsDescription.isEmpty())
      res.put("Q_MS_LCMS_METHOD_INFO", lcmsDescription);
    if (!ionizationMode.isEmpty())
      res.put("Q_IONIZATION_MODE", ionizationMode);
    if (!lcDetectionMethod.isEmpty())
      res.put("Q_LC_DETECTION_METHOD", lcDetectionMethod);
    if (!columnName.isEmpty())
      res.put("Q_CHROMATOGRAPHY_COLUMN_NAME", columnName);
    if (!washingSolvent.isEmpty())
      res.put("Q_WASHING_SOLVENT", washingSolvent);
    if (additionalInformation!=null && !additionalInformation.isEmpty())
      res.put("Q_ADDITIONAL_INFO", additionalInformation);
    return res;
  }

  public void setAdditionalInformation(String info) {
    this.additionalInformation = info;
  }

}
