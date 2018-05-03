package life.qbic.expdesign;


import java.util.Comparator;

import life.qbic.datamodel.samples.ISampleBean;

/**
 * Compares ISampleBeans by their sample type
 * @author Andreas Friedrich
 *
 */
public class SampleTypeComparator implements Comparator<ISampleBean> {

	private static final SampleTypeComparator instance = 
			new SampleTypeComparator();

	public static SampleTypeComparator getInstance() {
		return instance;
	}

	private SampleTypeComparator() {
	}

	@Override
	public int compare(ISampleBean o1, ISampleBean o2) {
		return o1.getType().compareTo(o2.getType());
	}

}
