package uk.org.taverna.scufl2.rdfxml;

import java.awt.DisplayMode;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3._1999._02._22_rdf_syntax_ns_.RDF;
import org.w3._1999._02._22_rdf_syntax_ns_.Resource;
import org.w3._1999._02._22_rdf_syntax_ns_.Type;
import org.w3._2000._01.rdf_schema_.SeeAlso;
import org.xml.sax.SAXException;

import uk.org.taverna.scufl2.api.common.URITools;
import uk.org.taverna.scufl2.api.common.Visitor;
import uk.org.taverna.scufl2.api.common.WorkflowBean;
import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.core.Processor;
import uk.org.taverna.scufl2.api.core.Workflow;
import uk.org.taverna.scufl2.api.dispatchstack.DispatchStack;
import uk.org.taverna.scufl2.api.dispatchstack.DispatchStackLayer;
import uk.org.taverna.scufl2.api.io.WriterException;
import uk.org.taverna.scufl2.api.iterationstrategy.CrossProduct;
import uk.org.taverna.scufl2.api.iterationstrategy.DotProduct;
import uk.org.taverna.scufl2.api.iterationstrategy.IterationStrategy;
import uk.org.taverna.scufl2.api.iterationstrategy.IterationStrategyParent;
import uk.org.taverna.scufl2.api.iterationstrategy.IterationStrategyStack;
import uk.org.taverna.scufl2.api.iterationstrategy.PortNode;
import uk.org.taverna.scufl2.api.port.InputProcessorPort;
import uk.org.taverna.scufl2.api.port.InputWorkflowPort;
import uk.org.taverna.scufl2.api.port.OutputProcessorPort;
import uk.org.taverna.scufl2.api.port.OutputWorkflowPort;
import uk.org.taverna.scufl2.api.profiles.Profile;
import uk.org.taverna.scufl2.rdfxml.impl.NamespacePrefixMapperJAXB_RI;
import uk.org.taverna.scufl2.rdfxml.jaxb.DispatchStack.DispatchStackLayers;
import uk.org.taverna.scufl2.rdfxml.jaxb.GranularPortDepth;
import uk.org.taverna.scufl2.rdfxml.jaxb.IterationStrategyStack.IterationStrategies;
import uk.org.taverna.scufl2.rdfxml.jaxb.ObjectFactory;
import uk.org.taverna.scufl2.rdfxml.jaxb.PortDepth;
import uk.org.taverna.scufl2.rdfxml.jaxb.ProductOf;
import uk.org.taverna.scufl2.rdfxml.jaxb.ProfileDocument;
import uk.org.taverna.scufl2.rdfxml.jaxb.SeeAlsoType;
import uk.org.taverna.scufl2.rdfxml.jaxb.WorkflowBundleDocument;
import uk.org.taverna.scufl2.rdfxml.jaxb.WorkflowDocument;

public class RDFXMLSerializer {

	public class WorkflowSerialisationVisitor implements Visitor {

		private final uk.org.taverna.scufl2.rdfxml.jaxb.Workflow workflow;
		private uk.org.taverna.scufl2.rdfxml.jaxb.Processor proc;
		private Workflow wf;
		private uk.org.taverna.scufl2.rdfxml.jaxb.DispatchStack dispatchStack;
		private uk.org.taverna.scufl2.rdfxml.jaxb.IterationStrategyStack iterationStrategyStack;
		private IterationStrategies iterationStrategies;
		private Stack<List<Object>> productStack;

		public WorkflowSerialisationVisitor(
				uk.org.taverna.scufl2.rdfxml.jaxb.Workflow workflow) {
					this.workflow = workflow;
		}

		@Override
		public boolean visitEnter(WorkflowBean node) {
			return visit(node);
		}

		@Override
		public boolean visit(WorkflowBean node) {
			if (node instanceof Workflow) {
				wf = (Workflow)node;
				workflow.setAbout("");
				workflow.setName(wf.getName());
		
				if (wf.getWorkflowIdentifier() != null) {
					Resource wfId = rdfObjectFactory.createResource();
					wfId.setResource(wf.getWorkflowIdentifier().toASCIIString());
					workflow.setWorkflowIdentifier(wfId);
				}
			}
			URI uri = uriTools.relativeUriForBean(node, wf);

			if (node instanceof InputWorkflowPort) {
				InputWorkflowPort ip = (InputWorkflowPort) node;
				uk.org.taverna.scufl2.rdfxml.jaxb.Workflow.InputWorkflowPort inP = objectFactory.createWorkflowInputWorkflowPort();
				uk.org.taverna.scufl2.rdfxml.jaxb.InputWorkflowPort inPort = objectFactory.createInputWorkflowPort();
				inP.setInputWorkflowPort(inPort);
				inPort.setName(ip.getName());
				
				URI portURI = uriTools.relativeUriForBean(ip, ip.getParent());
				inPort.setAbout(portURI.toASCIIString());				

				PortDepth portDepth = makePortDepth(ip.getDepth());
				inPort.setPortDepth(portDepth);
				workflow.getInputWorkflowPort().add(inP);
			}
			if (node instanceof OutputWorkflowPort) {
				OutputWorkflowPort op = (OutputWorkflowPort) node;
				uk.org.taverna.scufl2.rdfxml.jaxb.Workflow.OutputWorkflowPort inP = objectFactory.createWorkflowOutputWorkflowPort();
				uk.org.taverna.scufl2.rdfxml.jaxb.OutputWorkflowPort outPort = objectFactory.createOutputWorkflowPort();
				inP.setOutputWorkflowPort(outPort);
				outPort.setName(op.getName());
				
				URI portURI = uriTools.relativeUriForBean(op, op.getParent());
				outPort.setAbout(portURI.toASCIIString());			
				workflow.getOutputWorkflowPort().add(inP);			
			}
			if (node instanceof Processor) {
				Processor processor = (Processor) node;
				uk.org.taverna.scufl2.rdfxml.jaxb.Workflow.Processor wfProc = objectFactory.createWorkflowProcessor();
				proc = objectFactory.createProcessor();
				wfProc.setProcessor(proc);			
				proc.setName(processor.getName());				
				URI procUri = uriTools.relativeUriForBean(processor, wf);
				proc.setAbout(procUri.toASCIIString());
				wfProc.setProcessor(proc);			
				workflow.getProcessor().add(wfProc);				
			}
			if (node instanceof InputProcessorPort) {
				InputProcessorPort inPort = (InputProcessorPort) node;
				uk.org.taverna.scufl2.rdfxml.jaxb.InputProcessorPort port = objectFactory.createInputProcessorPort();
				port.setAbout(uri.toASCIIString());
				port.setName(inPort.getName());				
				port.setPortDepth(makePortDepth(inPort.getDepth()));
				uk.org.taverna.scufl2.rdfxml.jaxb.Processor.InputProcessorPort inputProcessorPort = objectFactory.createProcessorInputProcessorPort();
				inputProcessorPort.setInputProcessorPort(port);
				proc.getInputProcessorPort().add(inputProcessorPort);
			}
			if (node instanceof OutputProcessorPort) {
				uk.org.taverna.scufl2.rdfxml.jaxb.OutputProcessorPort port;
				OutputProcessorPort outPort = (OutputProcessorPort) node;
				port = objectFactory.createOutputProcessorPort();
				port.setAbout(uri.toASCIIString());
				port.setName(outPort.getName());				
				port.setPortDepth(makePortDepth(outPort.getDepth()));
				port.setGranularPortDepth(makeGranularPortDepth(outPort.getGranularDepth()));
				
				uk.org.taverna.scufl2.rdfxml.jaxb.Processor.OutputProcessorPort outputProcessorPort = objectFactory.createProcessorOutputProcessorPort();
				outputProcessorPort.setOutputProcessorPort(port);
				proc.getOutputProcessorPort().add(outputProcessorPort);
			}
			if (node instanceof DispatchStack) {
				DispatchStack stack = (DispatchStack) node;			
				dispatchStack = objectFactory.createDispatchStack();
				dispatchStack.setAbout(uri.toASCIIString());
				uk.org.taverna.scufl2.rdfxml.jaxb.Processor.DispatchStack procDisStack = objectFactory.createProcessorDispatchStack();
				proc.setDispatchStack(procDisStack);				
				procDisStack.setDispatchStack(dispatchStack);
				if (stack.getType() != null) {
					Type type = rdfObjectFactory.createType();
					type.setResource(stack.getType().toASCIIString());
					dispatchStack.setType(type);
				}
				DispatchStackLayers dispatchStackLayers = objectFactory.createDispatchStackDispatchStackLayers();
				dispatchStackLayers.setParseType(dispatchStackLayers.getParseType());
				dispatchStack.setDispatchStackLayers(dispatchStackLayers);
			}
			if (node instanceof DispatchStackLayer) {
				DispatchStackLayer dispatchStackLayer = (DispatchStackLayer) node;
				uk.org.taverna.scufl2.rdfxml.jaxb.DispatchStackLayer layer = objectFactory.createDispatchStackLayer();
				layer.setAbout(uri.toASCIIString());
				if (dispatchStackLayer.getConfigurableType() != null) {
					Type type = rdfObjectFactory.createType();
					type.setResource(dispatchStackLayer.getConfigurableType().toASCIIString());
					layer.setType(type);
				}
				dispatchStack.getDispatchStackLayers().getDispatchStackLayer().add(layer);				
			}
			if (node instanceof IterationStrategyStack) {
				IterationStrategyStack stack = (IterationStrategyStack) node;				
				iterationStrategyStack = objectFactory.createIterationStrategyStack();
				iterationStrategyStack.setAbout(uri.toASCIIString());
				uk.org.taverna.scufl2.rdfxml.jaxb.Processor.IterationStrategyStack processorIterationStrategyStack = objectFactory.createProcessorIterationStrategyStack();
				processorIterationStrategyStack.setIterationStrategyStack(iterationStrategyStack);
				proc.setIterationStrategyStack(processorIterationStrategyStack);				
				productStack = new Stack<List<Object>>();
			}
			if (node instanceof IterationStrategy) {
				iterationStrategies = objectFactory.createIterationStrategyStackIterationStrategies();				
				iterationStrategyStack.setIterationStrategies(iterationStrategies);
				iterationStrategies.setParseType(iterationStrategies.getParseType());
				List<Object> dotProductOrCrossProduct = iterationStrategies.getDotProductOrCrossProduct();
				productStack.add(dotProductOrCrossProduct);
			}
			if (node instanceof CrossProduct) {
				uk.org.taverna.scufl2.rdfxml.jaxb.CrossProduct crossProduct = objectFactory.createCrossProduct();
				crossProduct.setAbout(uri.toASCIIString());
				productStack.peek().add(crossProduct);
				ProductOf productOf = objectFactory.createProductOf();
				productOf.setParseType(productOf.getParseType());
				crossProduct.setProductOf(productOf);				
				productStack.add(crossProduct.getProductOf().getCrossProductOrDotProductOrInputProcessorPort());				
			}
			if (node instanceof DotProduct) {
				uk.org.taverna.scufl2.rdfxml.jaxb.DotProduct dotProduct = objectFactory.createDotProduct();
				dotProduct.setAbout(uri.toASCIIString());
				productStack.peek().add(dotProduct);
				ProductOf productOf = objectFactory.createProductOf();
				productOf.setParseType(productOf.getParseType());
				dotProduct.setProductOf(productOf);				
				productStack.add(dotProduct.getProductOf().getCrossProductOrDotProductOrInputProcessorPort());				
			}
			if (node instanceof PortNode) {
				PortNode portNode = (PortNode) node;
				InputProcessorPort inPort = portNode.getInputProcessorPort();
				URI portUri = uriTools.relativeUriForBean(inPort, wf);
				uk.org.taverna.scufl2.rdfxml.jaxb.ProductOf.InputProcessorPort port = objectFactory.createProductOfInputProcessorPort();
				port.setAbout(portUri.toASCIIString());
				productStack.peek().add(port);
			}
			
			// TODO: Datalinks
			
			return true;
			
		}

		private GranularPortDepth makeGranularPortDepth(Integer granularDepth) {
			if (granularDepth == null) {
				return null;
			}
			GranularPortDepth portDepth = objectFactory.createGranularPortDepth();
			portDepth.setValue(BigInteger.valueOf(granularDepth));
			portDepth.setDatatype("http://www.w3.org/2001/XMLSchema#integer");
			return portDepth;
		}

		private PortDepth makePortDepth(Integer depth) {
			if (depth == null) {
				return null;
			}
			PortDepth portDepth = objectFactory.createPortDepth();
			portDepth.setValue(BigInteger.valueOf(depth));
			portDepth.setDatatype("http://www.w3.org/2001/XMLSchema#integer");
			return portDepth;
		}

		@Override
		public boolean visitLeave(WorkflowBean node) {
			if (node instanceof IterationStrategyParent) {
				productStack.pop();
			}
			return true;
		}

	}

	protected synchronized static JAXBContext getJAxbContextStatic()
			throws JAXBException {
		if (jaxbContextStatic == null) {
			Class<?>[] packages = { ObjectFactory.class,
					org.purl.dc.elements._1.ObjectFactory.class,
					org.purl.dc.terms.ObjectFactory.class,
					org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory.class,
					org.w3._2002._07.owl_.ObjectFactory.class,
					org.w3._2000._01.rdf_schema_.ObjectFactory.class };
			jaxbContextStatic = JAXBContext.newInstance(packages);
		}
		return jaxbContextStatic;
	}

	private ObjectFactory objectFactory = new ObjectFactory();
	private org.w3._2000._01.rdf_schema_.ObjectFactory rdfsObjectFactory = new org.w3._2000._01.rdf_schema_.ObjectFactory();
	private org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory rdfObjectFactory = new org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory();
	private org.w3._2002._07.owl_.ObjectFactory owlObjectFactory = new org.w3._2002._07.owl_.ObjectFactory();
	
	private URITools uriTools = new URITools();

	private boolean usingSchema = false;

	private WorkflowBundle wfBundle;
	private JAXBContext jaxbContext;
	private Map<WorkflowBean, URI> seeAlsoUris = new HashMap<WorkflowBean, URI>();
	private static JAXBContext jaxbContextStatic;

	private static Logger logger = Logger.getLogger(RDFXMLSerializer.class
			.getCanonicalName());

	public RDFXMLSerializer() {
	}

	public RDFXMLSerializer(WorkflowBundle wfBundle) {
		this.setWfBundle(wfBundle);
	}

	public void workflowBundleDoc(OutputStream outputStream, URI path)
			throws JAXBException, WriterException {
		uk.org.taverna.scufl2.rdfxml.jaxb.WorkflowBundle bundle = makeWorkflowBundleElem();
		WorkflowBundleDocument doc = objectFactory
				.createWorkflowBundleDocument();
		doc.getAny().add(bundle);
		JAXBElement<RDF> element = new org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory()
				.createRDF(doc);

		getMarshaller().marshal(element, outputStream);
		seeAlsoUris.put(wfBundle, path);
	}

	protected uk.org.taverna.scufl2.rdfxml.jaxb.WorkflowBundle makeWorkflowBundleElem() {
		uk.org.taverna.scufl2.rdfxml.jaxb.WorkflowBundle bundle = objectFactory
				.createWorkflowBundle();
		// FIXME: Support other URIs
		bundle.setAbout("./");
		bundle.setName(wfBundle.getName());

		if (wfBundle.getSameBaseAs() != null) {
			Resource sameBaseAs = rdfObjectFactory.createResource();
			sameBaseAs.setResource(wfBundle.getSameBaseAs().toASCIIString());
			bundle.setSameBaseAs(sameBaseAs);
		}

		for (Workflow wf : wfBundle.getWorkflows()) {
			uk.org.taverna.scufl2.rdfxml.jaxb.WorkflowBundle.Workflow wfElem = objectFactory
					.createWorkflowBundleWorkflow();
			SeeAlsoType seeAlsoElem = objectFactory.createSeeAlsoType();
			seeAlsoElem.setAbout(uriTools.relativeUriForBean(wf, wfBundle)
					.toASCIIString());
			;

			if (seeAlsoUris.containsKey(wf)) {
				SeeAlso seeAlso = rdfsObjectFactory.createSeeAlso();
				seeAlso.setResource(seeAlsoUris.get(wf).toASCIIString());
				seeAlsoElem.setSeeAlso(seeAlso);
			} else {
				logger.warning("Can't find bundle URI for workflow document "
						+ wf.getName());
			}

			wfElem.setWorkflow(seeAlsoElem);
			bundle.getWorkflow().add(wfElem);

			if (wfBundle.getMainWorkflow() == wf) {
				Resource mainWorkflow = rdfObjectFactory.createResource();
				mainWorkflow.setResource(seeAlsoElem.getAbout());
				bundle.setMainWorkflow(mainWorkflow);
			}
		}

		for (Profile pf : wfBundle.getProfiles()) {
			uk.org.taverna.scufl2.rdfxml.jaxb.WorkflowBundle.Profile wfElem = objectFactory
					.createWorkflowBundleProfile();
			SeeAlsoType seeAlsoElem = objectFactory.createSeeAlsoType();
			seeAlsoElem.setAbout(uriTools.relativeUriForBean(pf, wfBundle)
					.toASCIIString());
			;

			if (seeAlsoUris.containsKey(pf)) {
				SeeAlso seeAlso = rdfsObjectFactory.createSeeAlso();
				seeAlso.setResource(seeAlsoUris.get(pf).toASCIIString());
				seeAlsoElem.setSeeAlso(seeAlso);
			} else {
				logger.warning("Can't find bundle URI for profile document "
						+ pf.getName());
			}

			wfElem.setProfile(seeAlsoElem);
			bundle.getProfile().add(wfElem);

			if (wfBundle.getMainProfile() == pf) {
				Resource mainProfile = rdfObjectFactory.createResource();
				mainProfile.setResource(seeAlsoElem.getAbout());
				bundle.setMainProfile(mainProfile);
			}
		}

		return bundle;
	}

	public Marshaller getMarshaller() {
		String schemaPath = "xsd/scufl2.xsd";
		Marshaller marshaller;
		try {
			marshaller = getJaxbContext().createMarshaller();

			if (isUsingSchema()) {
				SchemaFactory schemaFactory = SchemaFactory
						.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
				Schema schema = schemaFactory.newSchema(getClass().getResource(
						schemaPath));
				// FIXME: re-enable schema
				marshaller.setSchema(schema);
			}
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
					Boolean.TRUE);
			marshaller
					.setProperty(
							"jaxb.schemaLocation",
							"http://ns.taverna.org.uk/2010/scufl2# http://ns.taverna.org.uk/2010/scufl2/scufl2.xsd "
									+ "http://www.w3.org/1999/02/22-rdf-syntax-ns# http://ns.taverna.org.uk/2010/scufl2/rdf.xsd");
		} catch (JAXBException e) {
			throw new IllegalStateException(e);
		} catch (SAXException e) {
			throw new IllegalStateException("Could not load schema "
					+ schemaPath, e);
		}
		setPrefixMapper(marshaller);
		return marshaller;

	}

	protected void setPrefixMapper(Marshaller marshaller) {
		boolean setPrefixMapper = false;

		try {
			// This only works with JAXB RI, in which case we can set the
			// namespace
			// prefix mapper
			Class.forName("com.sun.xml.bind.marshaller.NamespacePrefixMapper");
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",
					new NamespacePrefixMapperJAXB_RI());
			// Note: A similar mapper for the built-in java
			// (com.sun.xml.bind.internal.namespacePrefixMapper)
			// is no longer included here, as it will not (easily) compile with
			// Maven.
			setPrefixMapper = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!setPrefixMapper) {
			logger.warning("Could not set prefix mapper (incompatible JAXB) "
					+ "- will use prefixes ns0, ns1, ..");
		}
	}

	public void workflowDoc(OutputStream outputStream, Workflow wf, URI path)
			throws JAXBException, WriterException {
		uk.org.taverna.scufl2.rdfxml.jaxb.Workflow wfElem = makeWorkflow(wf,
				path);
		WorkflowDocument doc = objectFactory.createWorkflowDocument();
		doc.getAny().add(wfElem);
		
		URI wfUri = uriTools.relativeUriForBean(wf, wfBundle);		
		doc.setBase(uriTools.relativePath(path, wfUri).toASCIIString());
		
		JAXBElement<RDF> element = new org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory()
				.createRDF(doc);		
		getMarshaller().marshal(element, outputStream);
		seeAlsoUris.put(wf, path);
	}

	protected uk.org.taverna.scufl2.rdfxml.jaxb.Workflow makeWorkflow(
			Workflow wf, URI documentPath) {
		
		
		uk.org.taverna.scufl2.rdfxml.jaxb.Workflow workflow = objectFactory
				.createWorkflow();
		wf.accept(new WorkflowSerialisationVisitor(workflow) {});
		
		
		return workflow;
	}

	public void setWfBundle(WorkflowBundle wfBundle) {
		this.wfBundle = wfBundle;
	}

	public WorkflowBundle getWfBundle() {
		return wfBundle;
	}

	public void setJaxbContext(JAXBContext jaxbContext) {
		this.jaxbContext = jaxbContext;
	}

	public JAXBContext getJaxbContext() throws JAXBException {
		if (jaxbContext == null) {
			return getJAxbContextStatic();
		}
		return jaxbContext;
	}

	public void profileDoc(OutputStream outputStream, Profile pf, URI path)
			throws JAXBException, WriterException {
		ProfileDocument doc = objectFactory.createProfileDocument();
		JAXBElement<RDF> element = new org.w3._1999._02._22_rdf_syntax_ns_.ObjectFactory()
				.createRDF(doc);
		getMarshaller().marshal(element, outputStream);
		seeAlsoUris.put(pf, path);

	}

	public void setUsingSchema(boolean usingSchema) {
		this.usingSchema = usingSchema;
	}

	public boolean isUsingSchema() {
		return usingSchema;
	}

}
