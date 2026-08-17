package ca.uhn.hl7v2.custom.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.Version;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.parser.CustomModelClassFactory;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import ca.uhn.hl7v2.util.StringUtil;

//a copy of CustomModelClassFactory but uses a different class loader
public class PluginModelClassFactory extends CustomModelClassFactory {

	private static final long serialVersionUID = 1;
	private static Logger LOG = LoggerFactory.getLogger(CustomModelClassFactory.class);

	private final ModelClassFactory delegate;
	private Map<String, String[]> customModelClasses;

	// some optimization
	private ConcurrentMap<String, Class> cache = new ConcurrentHashMap<>();

	/**
	 * Constructor which just delegated to {@link DefaultModelClassFactory}
	 */
	public PluginModelClassFactory() {
		this((Map<String, String[]>) null);
	}

	/**
	 * Constructor
	 *
	 * @param packageName The base package name to use.
	 *                    <p>
	 *                    When searching, package specified here will be appended
	 *                    with .[version].[structure type].
	 *                    </p>
	 *                    <p>
	 *                    So, for instance, when looking for a v2.5 segment object,
	 *                    if "<code>com.foo</code>" is passed in, HAPI will look in
	 *                    "<code>com.foo.v25.segment.*</code>"
	 *                    </p>
	 */
	public PluginModelClassFactory(String packageName) {
		this(new HashMap<String, String[]>());

		if (!packageName.endsWith(".")) {
			packageName += ".";
		}
		for (Version v : Version.values()) {
			addModel(v.getVersion(), new String[] { packageName + v.getPackageVersion() });
		}
	}

	/**
	 * Constructor
	 *
	 * @param map Map of packages to include.
	 *            <p>
	 *            Keys are versions of HL7, e.g. "2.5".
	 *            </p>
	 *            <p>
	 *            Values are an array of packages to search in for custom model
	 *            classes. When searching, the package name here will be appended
	 *            with "<b>.[structure type]</b>". So, for example, to specify a
	 *            custom message type, you could create the class
	 *            <code>foo.example.v23.message.ZRM_Z01</code>, and pass in the
	 *            string "<code>foo.example.v23</code>".
	 *            </p>
	 */
	public PluginModelClassFactory(Map<String, String[]> map) {
		this(new DefaultModelClassFactory(), map);
	}

	/**
	 * Set an explicit {@link ModelClassFactory} is underlying delegate
	 *
	 * @param defaultFactory default factory to be delegated to
	 * @param map            custom model map
	 */
	public PluginModelClassFactory(ModelClassFactory defaultFactory, Map<String, String[]> map) {
		this.delegate = defaultFactory;
		customModelClasses = map;
	}

	/**
	 * {@inheritDoc }
	 */
	@Override
	public Class<? extends Message> getMessageClass(String name, String version, boolean isExplicit)
			throws HL7Exception {
		if (!isExplicit) {
			name = getMessageStructureForEvent(name, Version.versionOf(version));
		}
		String key = "message" + name + version;
		Class<? extends Message> retVal = cache.get(key);
		if (retVal != null) {
			return retVal;
		}

		retVal = findClass("message", name, version);
		if (retVal == null) {
			retVal = delegate.getMessageClass(name, version, isExplicit);
		}
		if (retVal != null) {
			cache.putIfAbsent(key, retVal);
		}
		return retVal;
	}

	/**
	 * {@inheritDoc }
	 */
	@Override
	public Class<? extends Group> getGroupClass(String name, String version) throws HL7Exception {
		String key = "group" + name + version;
		Class<? extends Group> retVal = cache.get(key);
		if (retVal != null) {
			return retVal;
		}
		retVal = findClass("group", name, version);
		if (retVal == null) {
			retVal = delegate.getGroupClass(name, version);
		}
		if (retVal != null) {
			cache.putIfAbsent(key, retVal);
		}
		return retVal;
	}

	/**
	 * {@inheritDoc }
	 */
	@Override
	public Class<? extends Segment> getSegmentClass(String name, String version) throws HL7Exception {
		String key = "segment" + name + version;
		Class<? extends Segment> retVal = cache.get(key);
		if (retVal != null) {
			return retVal;
		}
		retVal = findClass("segment", name, version);
		if (retVal == null) {
			retVal = delegate.getSegmentClass(name, version);
		}
		if (retVal != null) {
			cache.putIfAbsent(key, retVal);
		}
		return retVal;
	}

	/**
	 * {@inheritDoc }
	 */
	@Override
	public Class<? extends Type> getTypeClass(String name, String version) throws HL7Exception {
		String key = "datatype" + name + version;
		Class<? extends Type> retVal = cache.get(key);
		if (retVal != null) {
			return retVal;
		}
		retVal = findClass("datatype", name, version);
		if (retVal == null) {
			retVal = delegate.getTypeClass(name, version);
		}
		if (retVal != null) {
			cache.putIfAbsent(key, retVal);
		}
		return retVal;
	}

	/**
	 * Finds appropriate classes to be loaded for the given structure/type
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected <T> Class<T> findClass(String subpackage, String name, String version) throws HL7Exception {
		Class<T> classLoaded = null;
		if (customModelClasses != null) {
			if (customModelClasses.containsKey(version)) {
				for (String next : customModelClasses.get(version)) {
					if (!next.endsWith(".")) {
						next += ".";
					}
					String fullyQualifiedName = next + subpackage + '.' + name;
					try {
//                        classLoaded = (Class<T>) Class.forName(fullyQualifiedName);
						classLoaded = (Class<T>) this.getClass().getClassLoader().loadClass(fullyQualifiedName);
						LOG.debug("Found " + fullyQualifiedName + " in custom HL7 model");
					} catch (ClassNotFoundException e) {
						// ignore
					}
				}
			}
		}
		return classLoaded;
	}

	/**
	 * Delegates calls to
	 * {@link DefaultModelClassFactory#getMessageClassInASpecificPackage(String, String, boolean, String)}
	 */
	@Override
	public Class<? extends Message> getMessageClassInASpecificPackage(String theName, String theVersion,
			boolean theIsExplicit, String thePackageName) throws HL7Exception {
		return delegate.getMessageClassInASpecificPackage(theName, theVersion, theIsExplicit, thePackageName);
	}

	/**
	 * Returns the configured custom model classes
	 *
	 * @return a map of custom model classes
	 */
	@Override
	public Map<String, String[]> getCustomModelClasses() {
		return customModelClasses;
	}

	/**
	 * Add model class packages after the object has been instantiated
	 *
	 * @param addedModelClasses map with version number as key and package names has
	 *                          value
	 */
	@Override
	public void addModels(Map<String, String[]> addedModelClasses) {
		if (customModelClasses == null) {
			customModelClasses = new HashMap<>();
		}
		for (Entry<String, String[]> entry : addedModelClasses.entrySet()) {
			addModel(entry.getKey(), entry.getValue());
		}
	}

	private void addModel(String version, String[] newPackageNames) {
		if (customModelClasses.containsKey(version)) {
			// the new packages must be added after the existing ones.
			String[] existingPackageNames = customModelClasses.get(version);
			customModelClasses.put(version, StringUtil.concatenate(existingPackageNames, newPackageNames));
		} else {
			customModelClasses.put(version, newPackageNames);
		}
	}


	/**
	 * Looks up its own event map. If no structure was found, the call is delegated
	 * to the default ModelClassFactory. If nothing can be found, the eventName is
	 * returned as structure.
	 *
	 * @see ca.uhn.hl7v2.parser.AbstractModelClassFactory#getMessageStructureForEvent(java.lang.String,
	 *      ca.uhn.hl7v2.Version)
	 */
	@Override
	public String getMessageStructureForEvent(String eventName, Version version) throws HL7Exception {
		String structure = super.getMessageStructureForEvent(eventName, version);
		if (structure != null) {
			return structure;
		}
		structure = delegate.getMessageStructureForEvent(eventName, version);
		return structure != null ? structure : eventName;
	}
}
