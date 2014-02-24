package org.zkoss.zss.ngmodel.sys.formula;

/**
 * To contain {@link EvaluationContributor}
 * 
 * @author dennis
 * @since 3.5
 */
public interface EvaluationContributorContainer {
	public EvaluationContributor getEvaluationContributor();

	public void setEvaluationContributor(EvaluationContributor contributor);
}