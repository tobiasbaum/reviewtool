package de.setsoftware.reviewtool.plugin;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class WorkbenchPreferencePage extends FieldEditorPreferencePage implements
IWorkbenchPreferencePage {

	public WorkbenchPreferencePage() {
		super(GRID);
	}

	@Override
	public void createFieldEditors() {
		this.addField(new StringFieldEditor("url", "JIRA-URL", this.getFieldEditorParent()));
		this.addField(new StringFieldEditor("reviewRemarkField", "Feldname Reviewanmerkungen", this.getFieldEditorParent()));
		this.addField(new StringFieldEditor("reviewState", "Statusname Review", this.getFieldEditorParent()));
		this.addField(new StringFieldEditor("user", "JIRA-User", this.getFieldEditorParent()));
		this.addField(new StringFieldEditor("password", "JIRA-Passwort", this.getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
		this.setPreferenceStore(Activator.getDefault().getPreferenceStore());
		this.setDescription("Einstellung für das Review-Tool");
	}

}
