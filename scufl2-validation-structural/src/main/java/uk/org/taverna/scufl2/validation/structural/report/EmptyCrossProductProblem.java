package uk.org.taverna.scufl2.validation.structural.report;

import uk.org.taverna.scufl2.api.common.WorkflowBean;
import uk.org.taverna.scufl2.validation.ValidationProblem;


public class EmptyCrossProductProblem extends ValidationProblem {

	public EmptyCrossProductProblem(WorkflowBean bean) {
		super(bean);
	}

}
