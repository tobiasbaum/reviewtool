package de.setsoftware.reviewtool.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class ReviewToolPreferencePage extends FieldEditorPreferencePage implements
IWorkbenchPreferencePage {

	public static final String USER = "user";
	public static final String JIRA_SOURCE = "jiraSource";
	public static final String JIRA_PASSWORD = "password";
	public static final String JIRA_URL = "url";
	public static final String JIRA_REVIEW_REMARK_FIELD = "reviewRemarkField";
	public static final String JIRA_REVIEW_STATE = "reviewState";
	public static final String JIRA_IMPLEMENTATION_STATE = "implementationState";
	public static final String JIRA_READY_FOR_REVIEW_STATE = "readyForReviewState";
	public static final String JIRA_REJECTED_STATE = "rejectedState";
	public static final String JIRA_DONE_STATE = "doneState";
	public static final String FILE_PATH = "filePath";

	private BooleanFieldEditor jiraActive;
	private final List<FieldEditor> jiraFields = new ArrayList<>();
	private final List<FieldEditor> fileFields = new ArrayList<>();

	public ReviewToolPreferencePage() {
		super(GRID);
	}

	@Override
	public void createFieldEditors() {
		this.addField(new StringFieldEditor(USER, "Benutzername", this.getFieldEditorParent()));

		this.jiraActive = new BooleanFieldEditor(JIRA_SOURCE, "JIRA-Anbindung aktivieren", this.getFieldEditorParent());
		this.addField(this.jiraActive);

		this.addField(this.jiraFields, new StringFieldEditor(JIRA_URL, "JIRA-URL", this.getFieldEditorParent()));
		this.addField(this.jiraFields, new StringFieldEditor(JIRA_REVIEW_REMARK_FIELD, "Feldname Reviewanmerkungen", this.getFieldEditorParent()));
		this.addField(this.jiraFields, new StringFieldEditor(JIRA_IMPLEMENTATION_STATE, "Statusname \"In Fixing\"", this.getFieldEditorParent()));
		this.addField(this.jiraFields, new StringFieldEditor(JIRA_READY_FOR_REVIEW_STATE, "Statusname \"Bereit für Review\"", this.getFieldEditorParent()));
		this.addField(this.jiraFields, new StringFieldEditor(JIRA_REVIEW_STATE, "Statusname \"In Review\"", this.getFieldEditorParent()));
		this.addField(this.jiraFields, new StringFieldEditor(JIRA_REJECTED_STATE, "Statusname \"Review-Rückläufer\"", this.getFieldEditorParent()));
		this.addField(this.jiraFields, new StringFieldEditor(JIRA_DONE_STATE, "Statusname \"Abgeschlossen\"", this.getFieldEditorParent()));
		this.addField(this.jiraFields, new StringFieldEditor(JIRA_PASSWORD, "JIRA-Passwort", this.getFieldEditorParent()));

		this.addField(this.fileFields, new StringFieldEditor(FILE_PATH, "Verzeichnis", this.getFieldEditorParent()));
	}

	public static Collection<String> getAllPreferenceIds() {
		return Arrays.asList(
				USER,
				JIRA_SOURCE,
				JIRA_PASSWORD,
				JIRA_URL,
				JIRA_REVIEW_REMARK_FIELD,
				JIRA_IMPLEMENTATION_STATE,
				JIRA_READY_FOR_REVIEW_STATE,
				JIRA_REVIEW_STATE,
				JIRA_REJECTED_STATE,
				JIRA_DONE_STATE,
				FILE_PATH);
	}

	private void addField(List<FieldEditor> list, FieldEditor editor) {
		list.add(editor);
		this.addField(editor);
	}

	@Override
	protected void initialize() {
		super.initialize();
		this.updateFieldEnabledStates();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		if (event.getProperty().equals(FieldEditor.VALUE) && event.getSource().equals(this.jiraActive)) {
			this.updateFieldEnabledStates();
		}
	}

	private void updateFieldEnabledStates() {
		final boolean jira = this.jiraActive.getBooleanValue();
		for (final FieldEditor f : this.jiraFields) {
			f.setEnabled(jira, this.getFieldEditorParent());
		}
		for (final FieldEditor f : this.fileFields) {
			f.setEnabled(!jira, this.getFieldEditorParent());
		}
	}

	@Override
	public void init(IWorkbench workbench) {
		this.setPreferenceStore(Activator.getDefault().getPreferenceStore());
		this.setDescription("Einstellung für das Review-Tool");
	}

}
