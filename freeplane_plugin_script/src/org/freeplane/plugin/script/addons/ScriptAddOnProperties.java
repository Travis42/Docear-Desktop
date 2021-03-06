package org.freeplane.plugin.script.addons;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import org.freeplane.main.addons.AddOnProperties;
import org.freeplane.n3.nanoxml.XMLElement;
import org.freeplane.plugin.script.ExecuteScriptAction.ExecutionMode;
import org.freeplane.plugin.script.ScriptingEngine;
import org.freeplane.plugin.script.ScriptingPermissions;

/** For all add-ons that are installed via installScriptAddon.groovy - themes and script collections. */
public class ScriptAddOnProperties extends AddOnProperties {
	public static class Script {
		public String name;
		public File file;
		public ExecutionMode executionMode;
		public String menuTitleKey;
		public String menuLocation;
		public ScriptingPermissions permissions;
		public String keyboardShortcut;
		public String scriptBody;
		// transient - copy of AddOnProperties.active
		public boolean active = true;

		public String toString() {
			return name + "(" + executionMode + "/" + menuTitleKey + "/" + menuLocation + "" + ")";
		}
	}

	private List<Script> scripts;

	public ScriptAddOnProperties(String name) {
		super(AddOnType.SCRIPT);
		setName(name);
	}

	public ScriptAddOnProperties(final XMLElement addOnelement) {
		super(AddOnType.SCRIPT, addOnelement);
		this.scripts = parseScripts(addOnelement.getChildrenNamed("scripts"));
		validate();
	}

	private void validate() {
		if (scripts == null)
			throw new RuntimeException(this + ": on parsing add-on XML file: scripts may not be null");
		for (Script script : scripts) {
			if (script.name == null)
				throw new RuntimeException(this + ": on parsing add-on XML file: no name");
			if (script.file == null)
				throw new RuntimeException(this + ": on parsing add-on XML file: Script file " + script + "not defined");
			if (!script.file.exists())
				throw new RuntimeException(this + ": on parsing add-on XML file: Script " + script + " does not exist");
			if (script.executionMode == null)
				throw new RuntimeException(this + ": on parsing add-on XML file: no execution_mode");
			if (script.menuTitleKey == null)
				throw new RuntimeException(this + ": on parsing add-on XML file: no menu title key");
			if (script.menuLocation == null)
				throw new RuntimeException(this + ": on parsing add-on XML file: no menu location");
			if (script.permissions == null)
				throw new RuntimeException(this + ": on parsing add-on XML file: no permissions");
		}
	}

	private List<Script> parseScripts(Vector<XMLElement> xmlElements) {
		final ArrayList<Script> scripts = new ArrayList<Script>();
		if (xmlElements == null || xmlElements.isEmpty())
			return scripts;
		for (XMLElement scriptXmlNode : xmlElements.get(0).getChildren()) {
			final Script script = new Script();
			for (Entry<Object, Object> entry : scriptXmlNode.getAttributes().entrySet()) {
				if (entry.getKey().equals("name")) {
					script.name = (String) entry.getValue();
				}
				else if (entry.getKey().equals("file")) {
					script.file = new File(entry.getValue().toString());
				}
				else if (entry.getKey().equals("executionMode")) {
					script.executionMode = parseExecutionMode(entry.getValue().toString());
				}
				else if (entry.getKey().equals("menuTitleKey")) {
					script.menuTitleKey = entry.getValue().toString();
				}
				else if (entry.getKey().equals("menuLocation")) {
					script.menuLocation = entry.getValue().toString();
				}
			}
			script.permissions = new ScriptingPermissions(scriptXmlNode.getAttributes());
			fixUnsetFile(script);
			scripts.add(script);
		}
		return scripts;
	}

	/** This code is needed to set Script.file in earlier installations.
	 * @deprecated remove before the next stable release! */
	private void fixUnsetFile(Script script) {
		if (script.file == null)
			script.file = new File(ScriptingEngine.getUserScriptDir(), script.name);
    }

	public static ExecutionMode parseExecutionMode(final String executionModeString) {
		try {
			return ExecutionMode.valueOf(executionModeString.toUpperCase());
		}
		catch (Exception e) {
			throw new RuntimeException("invalid execution mode found in " + executionModeString, e);
		}
	}

	public List<Script> getScripts() {
    	return scripts;
    }

	public static String getNameKey(final String name) {
        return "addons." + name;
    }

	public XMLElement toXml() {
		final XMLElement xmlElement = super.toXml();
		addScriptsAsChild(xmlElement);
		return xmlElement;
	}

	private void addScriptsAsChild(XMLElement parent) {
		XMLElement xmlElement = new XMLElement("scripts");
		for (Script script : scripts) {
			XMLElement scriptXmlElement = new XMLElement("script");
			scriptXmlElement.setAttribute("name", script.name);
			scriptXmlElement.setAttribute("file", script.file == null ? null : script.file.getPath());
			scriptXmlElement.setAttribute("menuTitleKey", script.menuTitleKey);
			scriptXmlElement.setAttribute("menuLocation", script.menuLocation);
			scriptXmlElement.setAttribute("executionMode", script.executionMode.toString());
			final List<String> permissionNames = ScriptingPermissions.getPermissionNames();
			for (String permission : permissionNames) {
				scriptXmlElement.setAttribute(permission, Boolean.toString(script.permissions.get(permission)));
			}
			xmlElement.addChild(scriptXmlElement);
		}
		parent.addChild(xmlElement);
	}

	@Override
    public boolean supportsOperation(String opName) {
		if (opName.equals(OP_DEACTIVATE))
			return isActive() && !scripts.isEmpty();
	    return super.supportsOperation(opName);
    }
}
