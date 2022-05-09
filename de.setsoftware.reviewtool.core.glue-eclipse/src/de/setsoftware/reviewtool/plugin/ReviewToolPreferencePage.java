package de.setsoftware.reviewtool.plugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.config.ConfigurationInterpreter;

/**
 * The preference page for the review tool. Most preferences are stored in a common file (so that they can be
 * committed and shared by a team). This file contains placeholders, which are configured per user in eclipse.
 */
public class ReviewToolPreferencePage extends PreferencePage
        implements IWorkbenchPreferencePage, IPropertyChangeListener {

    public static final String TEAM_CONFIG_FILE = "teamConfigFile";
    private static final String USER_PARAM_PREFIX = "up_";

    private FileFieldEditor fileField;
    private Table userParamTable;
    private RelationMatcherPreferenceEditor relationEditor;

    public ReviewToolPreferencePage() {
        super();
    }

    @Override
    protected Control createContents(Composite parent) {
        final Composite parentComposite = new Composite(parent, SWT.NULL);
        final GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        parentComposite.setLayout(layout);
        parentComposite.setFont(parent.getFont());

        final Composite wrapper = new Composite(parentComposite, SWT.NONE);
        this.fileField = new FileFieldEditor(
                TEAM_CONFIG_FILE, "File with team config", wrapper);
        this.initField(this.fileField);

        this.userParamTable = new Table(parentComposite, SWT.BORDER);
        final TableColumn tc1 = new TableColumn(this.userParamTable, SWT.LEFT);
        final TableColumn tc2 = new TableColumn(this.userParamTable, SWT.LEFT);
        tc1.setText("Parameter");
        tc2.setText("Wert");
        tc1.setWidth(100);
        tc2.setWidth(100);
        this.userParamTable.setHeaderVisible(true);
        this.userParamTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        final TableEditor editor = new TableEditor(this.userParamTable);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        this.userParamTable.addListener(SWT.MouseDown, new Listener() {
            @Override
            public void handleEvent(Event event) {
                final Rectangle clientArea = ReviewToolPreferencePage.this.userParamTable.getClientArea();
                final Point pt = new Point(event.x, event.y);
                int index = ReviewToolPreferencePage.this.userParamTable.getTopIndex();
                while (index < ReviewToolPreferencePage.this.userParamTable.getItemCount()) {
                    final TableItem item = ReviewToolPreferencePage.this.userParamTable.getItem(index);
                    final Rectangle rect = item.getBounds(1);
                    if (rect.contains(pt)) {
                        final Text text = new Text(ReviewToolPreferencePage.this.userParamTable, SWT.NONE);
                        final Listener textListener = new Listener() {

                            @Override
                            public void handleEvent(Event e) {
                                switch (e.type) {
                                case SWT.FocusOut:
                                    this.setDataAndText(item, text);
                                    text.dispose();
                                    break;
                                case SWT.Traverse:
                                    switch (e.detail) {
                                        case SWT.TRAVERSE_RETURN:
                                        this.setDataAndText(item, text);
                                            text.dispose();
                                            e.doit = false;
                                            break;
                                        case SWT.TRAVERSE_ESCAPE:
                                            text.dispose();
                                            e.doit = false;
                                            break;
                                        default:
                                            break;
                                    }
                                    break;
                                default:
                                    break;
                                }
                            }

                            private void setDataAndText(final TableItem item, final Text text) {
                                item.setData(text.getText());
                                if (isPassword(item.getText(0))) {
                                    item.setText(1, text.getText().replaceAll(".", "*"));
                                } else {
                                    item.setText(1, text.getText());
                                }
                            }
                        };
                        text.addListener(SWT.FocusOut, textListener);
                        text.addListener(SWT.Traverse, textListener);
                        editor.setEditor(text, item, 1);
                        text.setText((String) item.getData());
                        text.selectAll();
                        text.setFocus();
                        return;
                    }
                    if (!rect.intersects(clientArea)) {
                        return;
                    }
                    index++;
                }
            }
        });

        this.createUserTableItems();

        final RelationMatcherPreferences relations =
                RelationMatcherPreferences.load(Activator.getDefault().getPreferenceStore());
        this.relationEditor = new RelationMatcherPreferenceEditor(parentComposite, SWT.FILL, relations);
        this.relationEditor.setLayoutData(new GridData(GridData.FILL_BOTH));

        return parentComposite;
    }

    private static boolean isPassword(String fieldName) {
        return fieldName.toLowerCase().contains("password") || fieldName.toLowerCase().contains("passwort");
    }

    static ISecurePreferences getSecurePreferences() throws StorageException {
        final ISecurePreferences pref = SecurePreferencesFactory.getDefault().node("de.setsoftware.reviewtool");
        if (getPref(pref, ConfigurationInterpreter.USER_PARAM_NAME).equals("")) {
            putPref(pref, ConfigurationInterpreter.USER_PARAM_NAME, System.getProperty("user.name"));
        }
        return pref;
    }

    private void createUserTableItems() {
        try {
            final Set<String> userFieldNames = new ConfigurationInterpreter().getUserSpecificParamNames(
                    ConfigurationInterpreter.load(this.getFilename()));
            for (final String userFieldName : userFieldNames) {
                final TableItem item1 = new TableItem(this.userParamTable, SWT.NONE);
                final String data = getSecurePreferences().get(USER_PARAM_PREFIX + userFieldName, "");
                item1.setText(new String[] {
                        userFieldName,
                        isPassword(userFieldName) ? data.replaceAll(".", "*") : data });
                item1.setData(data);
            }
        } catch (final IOException | SAXException | ParserConfigurationException
                | ReviewtoolException | StorageException e) {
            MessageDialog.openError(this.getShell(), "Error while loading CoRT config",
                    "The team configuration could not be loaded: " + e.getMessage());
            Logger.error("error while loading team config", e);
        }
    }

    private String getFilename() {
        final String fromField = this.fileField.getStringValue();
        if (fromField.isEmpty()) {
            return Activator.getDefault().getPreferenceStore().getString(this.fileField.getPreferenceName());
        } else {
            return fromField;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getProperty().equals(FieldEditor.VALUE) && event.getSource().equals(this.fileField)) {
            this.userParamTable.removeAll();
            this.createUserTableItems();
        }
    }

    @Override
    public void init(IWorkbench workbench) {
        this.setPreferenceStore(Activator.getDefault().getPreferenceStore());
        this.setDescription("Settings for the CoRT code review tool");
    }


    private void deinitField(final FieldEditor pe) {
        pe.setPage(null);
        pe.setPropertyChangeListener(null);
        pe.setPreferenceStore(null);
    }

    private void initField(final FieldEditor pe) {
        pe.setPage(this);
        pe.setPropertyChangeListener(this);
        pe.setPreferenceStore(this.getPreferenceStore());
        pe.load();
    }

    @Override
    protected void performDefaults() {
        this.fileField.loadDefault();
        for (final TableItem item : this.userParamTable.getItems()) {
            item.setText(1, "");
        }
        this.relationEditor.performDefaults();
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        this.relationEditor.save(this.getPreferenceStore());
        this.fileField.store();
        try {
            final ISecurePreferences prefs = getSecurePreferences();
            for (final TableItem item : this.userParamTable.getItems()) {
                putPref(prefs, item.getText(0), (String) item.getData());
            }
        } catch (final StorageException e) {
            MessageDialog.openError(this.getShell(), "Error while storing user config",
                    "The user config could not be stored: " + e.getMessage());
            Logger.error("error while storing user config", e);
        }
        //there seems to be no way to listen for changes in secure preferences.
        //  As a (little hacky but simple) workaround we store a dummy setting that will trigger the reconfigure
        this.getPreferenceStore().setValue("triggerDummy", Math.random());
        return true;
    }

    private static void putPref(ISecurePreferences pref, String key, String value) throws StorageException {
        pref.put(USER_PARAM_PREFIX + key, value, true);
    }

    @Override
    public void dispose() {
        super.dispose();
        this.deinitField(this.fileField);
        this.userParamTable.dispose();
        this.relationEditor.dispose();
    }

    /**
     * Extracts all user specific parameters used in the given config from the given preference store.
     */
    public static Map<String, String> getUserParams(Document config, ISecurePreferences pref) throws StorageException {
        final Set<String> names = new ConfigurationInterpreter().getUserSpecificParamNames(config);
        final Map<String, String> params = new HashMap<>();
        for (final String name : names) {
            params.put(name, getPref(pref, name));
        }
        return params;
    }

    private static String getPref(ISecurePreferences pref, final String name) throws StorageException {
        return pref.get(USER_PARAM_PREFIX + name, "");
    }

    public static String getUserIdPref() throws StorageException {
        return getPref(getSecurePreferences(), ConfigurationInterpreter.USER_PARAM_NAME);
    }

}
