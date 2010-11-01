package uk.org.taverna.scufl2.api.profiles;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import uk.org.taverna.scufl2.api.activity.Activity;
import uk.org.taverna.scufl2.api.common.AbstractNamed;
import uk.org.taverna.scufl2.api.common.NamedSet;
import uk.org.taverna.scufl2.api.common.WorkflowBean;
import uk.org.taverna.scufl2.api.configurations.Configuration;
import uk.org.taverna.scufl2.api.container.WorkflowBundle;

/**
 * A Profile specifies a set of compatible ProcessorBindings. For example, one
 * Profile could contain ways of enacting a set of Processors on a grid whilst
 * another contained ways of enacting the Processors on a laptop.
 * 
 * @author Alan R Williams
 * 
 */
@XmlType(propOrder = { "profilePosition", "processorBindings", "configurations" })
public class Profile extends AbstractNamed implements WorkflowBean {

	private Set<ProcessorBinding> processorBindings = new HashSet<ProcessorBinding>();

	private NamedSet<Configuration> configurations = new NamedSet<Configuration>();

	private int profilePosition;

	private Set<Activity> activities = new HashSet<Activity>();

	public Profile() {
		super();
	}

	public Profile(String name) {
		super(name);
	}

	public Set<Activity> getActivities() {
		return activities;
	}

	/**
	 * @return
	 */
	@XmlElementWrapper(name = "configurations", nillable = false, required = true)
	@XmlElement(name = "configuration", nillable = false)
	public NamedSet<Configuration> getConfigurations() {
		return configurations;
	}

	/**
	 * Return the set of bindings for individual Processors.
	 * 
	 * @return
	 */
	@XmlElementWrapper(name = "processorBindings", nillable = false, required = true)
	@XmlElement(name = "processorBinding", nillable = false)
	public Set<ProcessorBinding> getProcessorBindings() {
		return processorBindings;
	}

	/**
	 * Position of this profile within the {@link WorkflowBundle}. When
	 * ordering profiles, they can be sorted by decreasing profilePosition. If
	 * two profiles have the same position, their internal order is
	 * indetermined.
	 * 
	 * @return
	 */
	@XmlElement(required = true, nillable = false)
	public final int getProfilePosition() {
		return profilePosition;
	}

	public void setActivities(Set<Activity> activities) {
		this.activities = activities;
	}

	/**
	 * @param configurations
	 */
	public void setConfigurations(Set<Configuration> configurations) {
		this.configurations.clear();
		this.configurations.addAll(configurations);
	}

	/**
	 * @param processorBindings
	 */
	public void setProcessorBindings(Set<ProcessorBinding> processorBindings) {
		this.processorBindings = processorBindings;
	}

	public final void setProfilePosition(int profilePosition) {
		this.profilePosition = profilePosition;
	}

}
