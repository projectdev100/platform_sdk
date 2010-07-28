/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.ui;

import com.android.ide.eclipse.adt.internal.resources.configurations.CountryCodeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.DockModeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.configurations.KeyboardStateQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.LanguageQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.NavigationMethodQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.NavigationStateQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.NetworkCodeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.NightModeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.PixelDensityQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.RegionQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ResourceQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenDimensionQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenOrientationQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenRatioQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenSizeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.TextInputMethodQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.TouchScreenQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.VersionQualifier;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceManager;
import com.android.sdklib.resources.Density;
import com.android.sdklib.resources.DockMode;
import com.android.sdklib.resources.Keyboard;
import com.android.sdklib.resources.KeyboardState;
import com.android.sdklib.resources.Navigation;
import com.android.sdklib.resources.NavigationState;
import com.android.sdklib.resources.NightMode;
import com.android.sdklib.resources.ResourceEnum;
import com.android.sdklib.resources.ScreenOrientation;
import com.android.sdklib.resources.ScreenRatio;
import com.android.sdklib.resources.ScreenSize;
import com.android.sdklib.resources.TouchScreen;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Custom UI widget to let user build a Folder configuration.
 * <p/>
 * To use this, instantiate somewhere in the UI and then:
 * <ul>
 * <li>Use {@link #setConfiguration(String)} or {@link #setConfiguration(FolderConfiguration)}.
 * <li>Retrieve the configuration using {@link #getConfiguration(FolderConfiguration)}.
 * </ul>
 */
public class ConfigurationSelector extends Composite {

    public static final int WIDTH_HINT = 600;
    public static final int HEIGHT_HINT = 250;

    private Runnable mOnChangeListener;

    private TableViewer mFullTableViewer;
    private TableViewer mSelectionTableViewer;
    private Button mAddButton;
    private Button mRemoveButton;
    private StackLayout mStackLayout;

    private boolean mOnRefresh = false;

    private final FolderConfiguration mBaseConfiguration = new FolderConfiguration();
    private final FolderConfiguration mSelectedConfiguration = new FolderConfiguration();

    private final HashMap<Class<? extends ResourceQualifier>, QualifierEditBase> mUiMap =
        new HashMap<Class<? extends ResourceQualifier>, QualifierEditBase>();
    private final SelectorMode mMode;
    private Composite mQualifierEditParent;
    private IQualifierFilter mQualifierFilter;

    /**
     * Basic of {@link VerifyListener} to only accept digits.
     */
    private static class DigitVerifier implements VerifyListener {
        public void verifyText(VerifyEvent e) {
            // check for digit only.
            for (int i = 0 ; i < e.text.length(); i++) {
                char letter = e.text.charAt(i);
                if (letter < '0' || letter > '9') {
                    e.doit = false;
                    return;
                }
            }
        }
    }

    /**
     * Implementation of {@link VerifyListener} for Country Code qualifiers.
     */
    public static class MobileCodeVerifier extends DigitVerifier {
        @Override
        public void verifyText(VerifyEvent e) {
            super.verifyText(e);

            // basic tests passed?
            if (e.doit) {
                // check the max 3 digits.
                if (e.text.length() - e.end + e.start +
                        ((Text)e.getSource()).getText().length() > 3) {
                    e.doit = false;
                }
            }
        }
    }

    /**
     * Implementation of {@link VerifyListener} for the Language and Region qualifiers.
     */
    public static class LanguageRegionVerifier implements VerifyListener {
        public void verifyText(VerifyEvent e) {
            // check for length
            if (e.text.length() - e.end + e.start + ((Combo)e.getSource()).getText().length() > 2) {
                e.doit = false;
                return;
            }

            // check for lower case only.
            for (int i = 0 ; i < e.text.length(); i++) {
                char letter = e.text.charAt(i);
                if ((letter < 'a' || letter > 'z') && (letter < 'A' || letter > 'Z')) {
                    e.doit = false;
                    return;
                }
            }
        }
    }

    /**
     * Implementation of {@link VerifyListener} for the Pixel Density qualifier.
     */
    public static class DensityVerifier extends DigitVerifier { }

    /**
     * Implementation of {@link VerifyListener} for the Screen Dimension qualifier.
     */
    public static class DimensionVerifier extends DigitVerifier { }

    /**
     * Enum for the state of the configuration being created.
     */
    public enum ConfigurationState {
        OK, INVALID_CONFIG, REGION_WITHOUT_LANGUAGE;
    }

    /**
     * Behavior mode for the Selector.
     *
     * @see #DEFAULT
     * @see #DEVICE_ONLY
     * @see #CONFIG_ONLY
     */
    public enum SelectorMode {
        /** the default mode */
        DEFAULT,
        /** mode forcing the qualifier values to be valid on a device.
         * For instance {@link Density#NODPI} is a valid qualifier for a resource configuration but
         * this is not valid on a device */
        DEVICE_ONLY,
        /** mode where only the specific config can be edited. The user can only select
         * which non-empty qualifier to select. */
        CONFIG_ONLY;
    }

    /**
     * A filter for {@link ResourceQualifier}.
     * @see ConfigurationSelector#setQualifierFilter(IQualifierFilter)
     */
    public interface IQualifierFilter {
        /**
         * Returns true of the qualifier is accepted.
         */
        boolean accept(ResourceQualifier qualifier);
    }

    /**
     * Creates the selector.
     * <p/>
     * The {@link SelectorMode} changes the behavior of the selector depending on what is being
     * edited (a device config, a resource config, a given configuration).
     *
     * @param parent the composite parent.
     * @param deviceMode the mode for the selector.
     */
    public ConfigurationSelector(Composite parent, SelectorMode mode) {
        super(parent, SWT.NONE);

        mMode  = mode;
        mBaseConfiguration.createDefault();

        GridLayout gl = new GridLayout(4, false);
        gl.marginWidth = gl.marginHeight = 0;
        setLayout(gl);

        // first column is the first table
        final Table fullTable = new Table(this, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
        fullTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        fullTable.setHeaderVisible(true);
        fullTable.setLinesVisible(true);

        // create the column
        final TableColumn fullTableColumn = new TableColumn(fullTable, SWT.LEFT);
        // set the header
        fullTableColumn.setText("Available Qualifiers");

        fullTable.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = fullTable.getClientArea();
                fullTableColumn.setWidth(r.width);
            }
        });

        mFullTableViewer = new TableViewer(fullTable);
        mFullTableViewer.setContentProvider(new QualifierContentProvider());
        // the label provider must return the value of the label only if the mode is
        // CONFIG_ONLY
        mFullTableViewer.setLabelProvider(new QualifierLabelProvider(
                mMode == SelectorMode.CONFIG_ONLY));
        mFullTableViewer.setInput(mBaseConfiguration);
        mFullTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection structSelection = (IStructuredSelection)selection;
                    Object first = structSelection.getFirstElement();

                    if (first instanceof ResourceQualifier) {
                        mAddButton.setEnabled(true);
                        return;
                    }
                }

                mAddButton.setEnabled(false);
            }
        });

        // 2nd column is the left/right arrow button
        Composite buttonComposite = new Composite(this, SWT.NONE);
        gl = new GridLayout(1, false);
        gl.marginWidth = gl.marginHeight = 0;
        buttonComposite.setLayout(gl);
        buttonComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        new Composite(buttonComposite, SWT.NONE);
        mAddButton = new Button(buttonComposite, SWT.BORDER | SWT.PUSH);
        mAddButton.setText("->");
        mAddButton.setEnabled(false);
        mAddButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selection =
                    (IStructuredSelection)mFullTableViewer.getSelection();

                Object first = selection.getFirstElement();
                if (first instanceof ResourceQualifier) {
                    ResourceQualifier qualifier = (ResourceQualifier)first;

                    mBaseConfiguration.removeQualifier(qualifier);
                    mSelectedConfiguration.addQualifier(qualifier);

                    mFullTableViewer.refresh();
                    mSelectionTableViewer.refresh();
                    mSelectionTableViewer.setSelection(new StructuredSelection(qualifier), true);

                    onChange(false /* keepSelection */);
                }
            }
        });

        mRemoveButton = new Button(buttonComposite, SWT.BORDER | SWT.PUSH);
        mRemoveButton.setText("<-");
        mRemoveButton.setEnabled(false);
        mRemoveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IStructuredSelection selection =
                    (IStructuredSelection)mSelectionTableViewer.getSelection();

                Object first = selection.getFirstElement();
                if (first instanceof ResourceQualifier) {
                    ResourceQualifier qualifier = (ResourceQualifier)first;

                    mSelectedConfiguration.removeQualifier(qualifier);
                    mBaseConfiguration.addQualifier(qualifier);

                    mFullTableViewer.refresh();
                    mSelectionTableViewer.refresh();

                    onChange(false /* keepSelection */);
                }
            }
        });

        // 3rd column is the selected config table
        final Table selectionTable = new Table(this, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
        selectionTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        selectionTable.setHeaderVisible(true);
        selectionTable.setLinesVisible(true);

        // create the column
        final TableColumn selectionTableColumn = new TableColumn(selectionTable, SWT.LEFT);
        // set the header
        selectionTableColumn.setText("Chosen Qualifiers");

        selectionTable.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = selectionTable.getClientArea();
                selectionTableColumn.setWidth(r.width);
            }
        });
        mSelectionTableViewer = new TableViewer(selectionTable);
        mSelectionTableViewer.setContentProvider(new QualifierContentProvider());
        // always show the qualifier value in this case.
        mSelectionTableViewer.setLabelProvider(new QualifierLabelProvider(
                true /* showQualifierValue */));
        mSelectionTableViewer.setInput(mSelectedConfiguration);
        mSelectionTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                // ignore selection changes during resfreshes in some cases.
                if (mOnRefresh) {
                    return;
                }

                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection structSelection = (IStructuredSelection)selection;

                    if (structSelection.isEmpty() == false) {
                        Object first = structSelection.getFirstElement();

                        if (first instanceof ResourceQualifier) {
                            mRemoveButton.setEnabled(true);

                            if (mMode != SelectorMode.CONFIG_ONLY) {
                                QualifierEditBase composite = mUiMap.get(first.getClass());

                                if (composite != null) {
                                    composite.setQualifier((ResourceQualifier)first);
                                }

                                mStackLayout.topControl = composite;
                                mQualifierEditParent.layout();
                            }

                            return;
                        }
                    } else {
                        if (mMode != SelectorMode.CONFIG_ONLY) {
                            mStackLayout.topControl = null;
                            mQualifierEditParent.layout();
                        }
                    }
                }

                mRemoveButton.setEnabled(false);
            }
        });

        if (mMode != SelectorMode.CONFIG_ONLY) {
            // 4th column is the detail of the selected qualifier
            mQualifierEditParent = new Composite(this, SWT.NONE);
            mQualifierEditParent.setLayout(mStackLayout = new StackLayout());
            mQualifierEditParent.setLayoutData(new GridData(GridData.FILL_VERTICAL));

            // create the UI for all the qualifiers, and associate them to the
            // ResourceQualifer class.
            mUiMap.put(CountryCodeQualifier.class, new MCCEdit(mQualifierEditParent));
            mUiMap.put(NetworkCodeQualifier.class, new MNCEdit(mQualifierEditParent));
            mUiMap.put(LanguageQualifier.class, new LanguageEdit(mQualifierEditParent));
            mUiMap.put(RegionQualifier.class, new RegionEdit(mQualifierEditParent));
            mUiMap.put(ScreenSizeQualifier.class, new ScreenSizeEdit(mQualifierEditParent));
            mUiMap.put(ScreenRatioQualifier.class, new ScreenRatioEdit(mQualifierEditParent));
            mUiMap.put(ScreenOrientationQualifier.class, new OrientationEdit(mQualifierEditParent));
            mUiMap.put(DockModeQualifier.class, new DockModeEdit(mQualifierEditParent));
            mUiMap.put(NightModeQualifier.class, new NightModeEdit(mQualifierEditParent));
            mUiMap.put(PixelDensityQualifier.class, new PixelDensityEdit(mQualifierEditParent));
            mUiMap.put(TouchScreenQualifier.class, new TouchEdit(mQualifierEditParent));
            mUiMap.put(KeyboardStateQualifier.class, new KeyboardEdit(mQualifierEditParent));
            mUiMap.put(TextInputMethodQualifier.class, new TextInputEdit(mQualifierEditParent));
            mUiMap.put(NavigationStateQualifier.class,
                    new NavigationStateEdit(mQualifierEditParent));
            mUiMap.put(NavigationMethodQualifier.class, new NavigationEdit(mQualifierEditParent));
            mUiMap.put(ScreenDimensionQualifier.class,
                    new ScreenDimensionEdit(mQualifierEditParent));
            mUiMap.put(VersionQualifier.class, new VersionEdit(mQualifierEditParent));
        }
    }

    /**
     * Sets a {@link IQualifierFilter}. If non null, this will restrict the qualifiers that
     * can be chosen.
     * @param filter the filter to set.
     */
    public void setQualifierFilter(IQualifierFilter filter) {
        mQualifierFilter = filter;
    }

    /**
     * Sets a listener to be notified when the configuration changes.
     * @param listener A {@link Runnable} whose <code>run()</code> method is called when the
     * configuration is changed. The method is called from the UI thread.
     */
    public void setOnChangeListener(Runnable listener) {
        mOnChangeListener = listener;
    }

    /**
     * Initialize the UI with a given {@link FolderConfiguration}. This must
     * be called from the UI thread.
     * @param config The configuration.
     */
    public void setConfiguration(FolderConfiguration config) {

        if (mMode != SelectorMode.CONFIG_ONLY) {
            mSelectedConfiguration.set(config, true /*nonFakeValuesOnly*/);

            // create the base config, which is the default config minus the qualifiers
            // in SelectedConfiguration
            mBaseConfiguration.substract(mSelectedConfiguration);
        } else {
            // set the base config to the edited config.
            // reset the config to be empty
            mBaseConfiguration.reset();
            mBaseConfiguration.set(config, true /*nonFakeValuesOnly*/);
        }

        mSelectionTableViewer.refresh();
        mFullTableViewer.refresh();
    }

    /**
     * Initialize the UI with the configuration represented by a resource folder name.
     * This must be called from the UI thread.
     *
     * @param folderSegments the segments of the folder name,
     *                       split using {@link FolderConfiguration#QUALIFIER_SEP}.
     * @return true if success, or false if the folder name is not a valid name.
     */
    public boolean setConfiguration(String[] folderSegments) {
        FolderConfiguration config = ResourceManager.getInstance().getConfig(folderSegments);

        if (config == null) {
            return false;
        }

        setConfiguration(config);

        return true;
    }

    /**
     * Initialize the UI with the configuration represented by a resource folder name.
     * This must be called from the UI thread.
     * @param folderName the name of the folder.
     * @return true if success, or false if the folder name is not a valid name.
     */
    public boolean setConfiguration(String folderName) {
        // split the name of the folder in segments.
        String[] folderSegments = folderName.split(FolderConfiguration.QUALIFIER_SEP);

        return setConfiguration(folderSegments);
    }

    /**
     * Gets the configuration as setup by the widget.
     * @param config the {@link FolderConfiguration} object to be filled with the information
     * from the UI.
     */
    public void getConfiguration(FolderConfiguration config) {
        config.set(mSelectedConfiguration);
    }

    /**
     * Returns the state of the configuration being edited/created.
     */
    public ConfigurationState getState() {
        if (mSelectedConfiguration.getInvalidQualifier() != null) {
            return ConfigurationState.INVALID_CONFIG;
        }

        if (mSelectedConfiguration.checkRegion() == false) {
            return ConfigurationState.REGION_WITHOUT_LANGUAGE;
        }

        return ConfigurationState.OK;
    }

    /**
     * Returns the first invalid qualifier of the configuration being edited/created,
     * or <code>null<code> if they are all valid (or if none exists).
     * <p/>If {@link #getState()} return {@link ConfigurationState#INVALID_CONFIG} then this will
     * not return <code>null</code>.
     */
    public ResourceQualifier getInvalidQualifier() {
        return mSelectedConfiguration.getInvalidQualifier();
    }

    /**
     * Handle changes in the configuration.
     * @param keepSelection if <code>true</code> attemps to avoid triggering selection change in
     * {@link #mSelectedConfiguration}.
     */
    private void onChange(boolean keepSelection) {
        ISelection selection = null;
        if (keepSelection) {
            mOnRefresh = true;
            selection = mSelectionTableViewer.getSelection();
        }

        mSelectionTableViewer.refresh(true);

        if (keepSelection) {
            mSelectionTableViewer.setSelection(selection);
            mOnRefresh = false;
        }

        if (mOnChangeListener != null) {
            mOnChangeListener.run();
        }
    }

    private void fillCombo(Combo combo, ResourceEnum[] resEnums) {
        for (ResourceEnum resEnum : resEnums) {
            // only add the enum if:
            // not in device mode OR (device mode is true and) it's a valid device value.
            // Also, always ignore fake values.
            if ((mMode == SelectorMode.DEFAULT || resEnum.isValidValueForDevice()) &&
                    resEnum.isFakeValue() == false) {
                combo.add(resEnum.getShortDisplayValue());
            }
        }
    }

    /**
     * Content provider around a {@link FolderConfiguration}.
     */
    private class QualifierContentProvider implements IStructuredContentProvider {

        private FolderConfiguration mInput;

        public QualifierContentProvider() {
        }

        public void dispose() {
            // pass
        }

        public Object[] getElements(Object inputElement) {
            // default easy case
            if (mQualifierFilter == null) {
                return mInput.getQualifiers();
            }

            // in this case we have to compute the list
            ArrayList<ResourceQualifier> list = new ArrayList<ResourceQualifier>();
            for (ResourceQualifier qual : mInput.getQualifiers()) {
                if (mQualifierFilter.accept(qual)) {
                    list.add(qual);
                }
            }

            return list.toArray();
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            mInput = null;
            if (newInput instanceof FolderConfiguration) {
                mInput = (FolderConfiguration)newInput;
            }
        }
    }

    /**
     * Label provider for {@link ResourceQualifier} objects.
     */
    private static class QualifierLabelProvider implements ITableLabelProvider {

        private final boolean mShowQualifierValue;

        public QualifierLabelProvider(boolean showQualifierValue) {
            mShowQualifierValue = showQualifierValue;
        }

        public String getColumnText(Object element, int columnIndex) {
            // only one column, so we can ignore columnIndex
            if (element instanceof ResourceQualifier) {
                if (mShowQualifierValue) {
                    String value = ((ResourceQualifier)element).getShortDisplayValue();
                    if (value.length() == 0) {
                        return String.format("%1$s (?)",
                                ((ResourceQualifier)element).getShortName());
                    } else {
                        return value;
                    }

                } else {
                    return ((ResourceQualifier)element).getShortName();
                }
            }

            return null;
        }

        public Image getColumnImage(Object element, int columnIndex) {
            // only one column, so we can ignore columnIndex
            if (element instanceof ResourceQualifier) {
                return ((ResourceQualifier)element).getIcon();
            }

            return null;
        }

        public void addListener(ILabelProviderListener listener) {
            // pass
        }

        public void dispose() {
            // pass
        }

        public boolean isLabelProperty(Object element, String property) {
            // pass
            return false;
        }

        public void removeListener(ILabelProviderListener listener) {
            // pass
        }
    }

    /**
     * Base class for Edit widget for {@link ResourceQualifier}.
     */
    private abstract static class QualifierEditBase extends Composite {

        public QualifierEditBase(Composite parent, String title) {
            super(parent, SWT.NONE);
            setLayout(new GridLayout(1, false));

            new Label(this, SWT.NONE).setText(title);
        }

        public abstract void setQualifier(ResourceQualifier qualifier);
    }

    /**
     * Edit widget for {@link CountryCodeQualifier}.
     */
    private class MCCEdit extends QualifierEditBase {

        private Text mText;

        public MCCEdit(Composite parent) {
            super(parent, CountryCodeQualifier.NAME);

            mText = new Text(this, SWT.BORDER);
            mText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mText.addVerifyListener(new MobileCodeVerifier());
            mText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    onTextChange();
                }
            });

            mText.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    onTextChange();
                }
            });

            new Label(this, SWT.NONE).setText("(3 digit code)");
        }

        private void onTextChange() {
            String value = mText.getText();

            if (value.length() == 0) {
                // empty string, means a qualifier with no value.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setCountryCodeQualifier(new CountryCodeQualifier());
            } else {
                try {
                    CountryCodeQualifier qualifier = CountryCodeQualifier.getQualifier(
                            CountryCodeQualifier.getFolderSegment(Integer.parseInt(value)));
                    if (qualifier != null) {
                        mSelectedConfiguration.setCountryCodeQualifier(qualifier);
                    } else {
                        // Failure! Looks like the value is wrong
                        // (for instance not exactly 3 digits).
                        mSelectedConfiguration.setCountryCodeQualifier(new CountryCodeQualifier());
                    }
                } catch (NumberFormatException nfe) {
                    // Looks like the code is not a number. This should not happen since the text
                    // field has a VerifyListener that prevents it.
                    mSelectedConfiguration.setCountryCodeQualifier(new CountryCodeQualifier());
                }
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            CountryCodeQualifier q = (CountryCodeQualifier)qualifier;

            mText.setText(Integer.toString(q.getCode()));
        }
    }

    /**
     * Edit widget for {@link NetworkCodeQualifier}.
     */
    private class MNCEdit extends QualifierEditBase {
        private Text mText;

        public MNCEdit(Composite parent) {
            super(parent, NetworkCodeQualifier.NAME);

            mText = new Text(this, SWT.BORDER);
            mText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mText.addVerifyListener(new MobileCodeVerifier());
            mText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    onTextChange();
                }
            });
            mText.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    onTextChange();
                }
            });

            new Label(this, SWT.NONE).setText("(1-3 digit code)");
        }

        private void onTextChange() {
            String value = mText.getText();

            if (value.length() == 0) {
                // empty string, means a qualifier with no value.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setNetworkCodeQualifier(new NetworkCodeQualifier());
            } else {
                try {
                    NetworkCodeQualifier qualifier = NetworkCodeQualifier.getQualifier(
                            NetworkCodeQualifier.getFolderSegment(Integer.parseInt(value)));
                    if (qualifier != null) {
                        mSelectedConfiguration.setNetworkCodeQualifier(qualifier);
                    } else {
                        // Failure! Looks like the value is wrong
                        // (for instance not exactly 3 digits).
                        mSelectedConfiguration.setNetworkCodeQualifier(new NetworkCodeQualifier());
                    }
                } catch (NumberFormatException nfe) {
                    // Looks like the code is not a number. This should not happen since the text
                    // field has a VerifyListener that prevents it.
                    mSelectedConfiguration.setNetworkCodeQualifier(new NetworkCodeQualifier());
                }
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            NetworkCodeQualifier q = (NetworkCodeQualifier)qualifier;

            mText.setText(Integer.toString(q.getCode()));
        }
    }

    /**
     * Edit widget for {@link LanguageQualifier}.
     */
    private class LanguageEdit extends QualifierEditBase {
        private Combo mLanguage;

        public LanguageEdit(Composite parent) {
            super(parent, LanguageQualifier.NAME);

            mLanguage = new Combo(this, SWT.DROP_DOWN);
            mLanguage.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mLanguage.addVerifyListener(new LanguageRegionVerifier());
            mLanguage.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onLanguageChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onLanguageChange();
                }
            });
            mLanguage.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    onLanguageChange();
                }
            });

            new Label(this, SWT.NONE).setText("(2 letter code)");
        }

        private void onLanguageChange() {
            // update the current config
            String value = mLanguage.getText();

            if (value.length() == 0) {
                // empty string, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setLanguageQualifier(new LanguageQualifier());
            } else {
                LanguageQualifier qualifier = null;
                String segment = LanguageQualifier.getFolderSegment(value);
                if (segment != null) {
                    qualifier = LanguageQualifier.getQualifier(segment);
                }

                if (qualifier != null) {
                    mSelectedConfiguration.setLanguageQualifier(qualifier);
                } else {
                    // Failure! Looks like the value is wrong (for instance a one letter string).
                    mSelectedConfiguration.setLanguageQualifier(new LanguageQualifier());
                }
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            LanguageQualifier q = (LanguageQualifier)qualifier;

            String value = q.getValue();
            if (value != null) {
                mLanguage.setText(value);
            }
        }
    }

    /**
     * Edit widget for {@link RegionQualifier}.
     */
    private class RegionEdit extends QualifierEditBase {
        private Combo mRegion;

        public RegionEdit(Composite parent) {
            super(parent, RegionQualifier.NAME);

            mRegion = new Combo(this, SWT.DROP_DOWN);
            mRegion.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mRegion.addVerifyListener(new LanguageRegionVerifier());
            mRegion.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onRegionChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onRegionChange();
                }
            });
            mRegion.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    onRegionChange();
                }
            });

            new Label(this, SWT.NONE).setText("(2 letter code)");
        }

        private void onRegionChange() {
            // update the current config
            String value = mRegion.getText();

            if (value.length() == 0) {
                // empty string, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setRegionQualifier(new RegionQualifier());
            } else {
                RegionQualifier qualifier = null;
                String segment = RegionQualifier.getFolderSegment(value);
                if (segment != null) {
                    qualifier = RegionQualifier.getQualifier(segment);
                }

                if (qualifier != null) {
                    mSelectedConfiguration.setRegionQualifier(qualifier);
                } else {
                    // Failure! Looks like the value is wrong (for instance a one letter string).
                    mSelectedConfiguration.setRegionQualifier(new RegionQualifier());
                }
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            RegionQualifier q = (RegionQualifier)qualifier;

            String value = q.getValue();
            if (value != null) {
                mRegion.setText(q.getValue());
            }
        }
    }

    /**
     * Edit widget for {@link ScreenSizeQualifier}.
     */
    private class ScreenSizeEdit extends QualifierEditBase {

        private Combo mSize;

        public ScreenSizeEdit(Composite parent) {
            super(parent, ScreenSizeQualifier.NAME);

            mSize = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            fillCombo(mSize, ScreenSize.values());

            mSize.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mSize.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onScreenSizeChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onScreenSizeChange();
                }
            });
        }

        protected void onScreenSizeChange() {
            // update the current config
            int index = mSize.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setScreenSizeQualifier(new ScreenSizeQualifier(
                    ScreenSize.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setScreenSizeQualifier(
                        new ScreenSizeQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            ScreenSizeQualifier q = (ScreenSizeQualifier)qualifier;

            ScreenSize value = q.getValue();
            if (value == null) {
                mSize.clearSelection();
            } else {
                mSize.select(ScreenSize.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link ScreenRatioQualifier}.
     */
    private class ScreenRatioEdit extends QualifierEditBase {

        private Combo mRatio;

        public ScreenRatioEdit(Composite parent) {
            super(parent, ScreenRatioQualifier.NAME);

            mRatio = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            fillCombo(mRatio, ScreenRatio.values());

            mRatio.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mRatio.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onScreenRatioChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onScreenRatioChange();
                }
            });
        }

        protected void onScreenRatioChange() {
            // update the current config
            int index = mRatio.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setScreenRatioQualifier(new ScreenRatioQualifier(
                        ScreenRatio.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setScreenRatioQualifier(
                        new ScreenRatioQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            ScreenRatioQualifier q = (ScreenRatioQualifier)qualifier;

            ScreenRatio value = q.getValue();
            if (value == null) {
                mRatio.clearSelection();
            } else {
                mRatio.select(ScreenRatio.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link ScreenOrientationQualifier}.
     */
    private class OrientationEdit extends QualifierEditBase {

        private Combo mOrientation;

        public OrientationEdit(Composite parent) {
            super(parent, ScreenOrientationQualifier.NAME);

            mOrientation = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            fillCombo(mOrientation, ScreenOrientation.values());

            mOrientation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mOrientation.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onOrientationChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onOrientationChange();
                }
            });
        }

        protected void onOrientationChange() {
            // update the current config
            int index = mOrientation.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setScreenOrientationQualifier(new ScreenOrientationQualifier(
                    ScreenOrientation.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setScreenOrientationQualifier(
                        new ScreenOrientationQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            ScreenOrientationQualifier q = (ScreenOrientationQualifier)qualifier;

            ScreenOrientation value = q.getValue();
            if (value == null) {
                mOrientation.clearSelection();
            } else {
                mOrientation.select(ScreenOrientation.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link DockModeQualifier}.
     */
    private class DockModeEdit extends QualifierEditBase {

        private Combo mDockMode;

        public DockModeEdit(Composite parent) {
            super(parent, DockModeQualifier.NAME);

            mDockMode = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            fillCombo(mDockMode, DockMode.values());

            mDockMode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mDockMode.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onDockModeChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onDockModeChange();
                }
            });
        }

        protected void onDockModeChange() {
            // update the current config
            int index = mDockMode.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setDockModeQualifier(
                        new DockModeQualifier(DockMode.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setDockModeQualifier(new DockModeQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            DockModeQualifier q = (DockModeQualifier)qualifier;

            DockMode value = q.getValue();
            if (value == null) {
                mDockMode.clearSelection();
            } else {
                mDockMode.select(DockMode.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link NightModeQualifier}.
     */
    private class NightModeEdit extends QualifierEditBase {

        private Combo mNightMode;

        public NightModeEdit(Composite parent) {
            super(parent, NightModeQualifier.NAME);

            mNightMode = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            fillCombo(mNightMode, NightMode.values());

            mNightMode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mNightMode.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onNightModeChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onNightModeChange();
                }
            });
        }

        protected void onNightModeChange() {
            // update the current config
            int index = mNightMode.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setNightModeQualifier(
                        new NightModeQualifier(NightMode.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setNightModeQualifier(new NightModeQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            NightModeQualifier q = (NightModeQualifier)qualifier;

            NightMode value = q.getValue();
            if (value == null) {
                mNightMode.clearSelection();
            } else {
                mNightMode.select(NightMode.getIndex(value));
            }
        }
    }


    /**
     * Edit widget for {@link PixelDensityQualifier}.
     */
    private class PixelDensityEdit extends QualifierEditBase {
        private Combo mDensity;

        public PixelDensityEdit(Composite parent) {
            super(parent, PixelDensityQualifier.NAME);

            mDensity = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            fillCombo(mDensity, Density.values());

            mDensity.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mDensity.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onDensityChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onDensityChange();
                }
            });
        }

        private void onDensityChange() {
            // update the current config
            int index = mDensity.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setPixelDensityQualifier(new PixelDensityQualifier(
                    Density.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setPixelDensityQualifier(
                        new PixelDensityQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            PixelDensityQualifier q = (PixelDensityQualifier)qualifier;

            Density value = q.getValue();
            if (value == null) {
                mDensity.clearSelection();
            } else {
                mDensity.select(Density.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link TouchScreenQualifier}.
     */
    private class TouchEdit extends QualifierEditBase {

        private Combo mTouchScreen;

        public TouchEdit(Composite parent) {
            super(parent, TouchScreenQualifier.NAME);

            mTouchScreen = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            fillCombo(mTouchScreen, TouchScreen.values());

            mTouchScreen.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mTouchScreen.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onTouchChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onTouchChange();
                }
            });
        }

        protected void onTouchChange() {
            // update the current config
            int index = mTouchScreen.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setTouchTypeQualifier(new TouchScreenQualifier(
                        TouchScreen.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setTouchTypeQualifier(new TouchScreenQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            TouchScreenQualifier q = (TouchScreenQualifier)qualifier;

            TouchScreen value = q.getValue();
            if (value == null) {
                mTouchScreen.clearSelection();
            } else {
                mTouchScreen.select(TouchScreen.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link KeyboardStateQualifier}.
     */
    private class KeyboardEdit extends QualifierEditBase {

        private Combo mKeyboardState;

        public KeyboardEdit(Composite parent) {
            super(parent, KeyboardStateQualifier.NAME);

            mKeyboardState = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            fillCombo(mKeyboardState, KeyboardState.values());

            mKeyboardState.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mKeyboardState.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onKeyboardChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onKeyboardChange();
                }
            });
        }

        protected void onKeyboardChange() {
            // update the current config
            int index = mKeyboardState.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setKeyboardStateQualifier(new KeyboardStateQualifier(
                        KeyboardState.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setKeyboardStateQualifier(
                        new KeyboardStateQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            KeyboardStateQualifier q = (KeyboardStateQualifier)qualifier;

            KeyboardState value = q.getValue();
            if (value == null) {
                mKeyboardState.clearSelection();
            } else {
                mKeyboardState.select(KeyboardState.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link TextInputMethodQualifier}.
     */
    private class TextInputEdit extends QualifierEditBase {

        private Combo mTextInput;

        public TextInputEdit(Composite parent) {
            super(parent, TextInputMethodQualifier.NAME);

            mTextInput = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            fillCombo(mTextInput, Keyboard.values());

            mTextInput.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mTextInput.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onTextInputChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onTextInputChange();
                }
            });
        }

        protected void onTextInputChange() {
            // update the current config
            int index = mTextInput.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setTextInputMethodQualifier(new TextInputMethodQualifier(
                        Keyboard.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setTextInputMethodQualifier(
                        new TextInputMethodQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            TextInputMethodQualifier q = (TextInputMethodQualifier)qualifier;

            Keyboard value = q.getValue();
            if (value == null) {
                mTextInput.clearSelection();
            } else {
                mTextInput.select(Keyboard.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link NavigationStateQualifier}.
     */
    private class NavigationStateEdit extends QualifierEditBase {

        private Combo mNavigationState;

        public NavigationStateEdit(Composite parent) {
            super(parent, NavigationStateQualifier.NAME);

            mNavigationState = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            fillCombo(mNavigationState, NavigationState.values());

            mNavigationState.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mNavigationState.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onNavigationChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onNavigationChange();
                }
            });
        }

        protected void onNavigationChange() {
            // update the current config
            int index = mNavigationState.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setNavigationStateQualifier(
                        new NavigationStateQualifier(NavigationState.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setNavigationStateQualifier(new NavigationStateQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            NavigationStateQualifier q = (NavigationStateQualifier)qualifier;

            NavigationState value = q.getValue();
            if (value == null) {
                mNavigationState.clearSelection();
            } else {
                mNavigationState.select(NavigationState.getIndex(value));
            }
        }
    }


    /**
     * Edit widget for {@link NavigationMethodQualifier}.
     */
    private class NavigationEdit extends QualifierEditBase {

        private Combo mNavigation;

        public NavigationEdit(Composite parent) {
            super(parent, NavigationMethodQualifier.NAME);

            mNavigation = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
            fillCombo(mNavigation, Navigation.values());

            mNavigation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mNavigation.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent e) {
                    onNavigationChange();
                }
                public void widgetSelected(SelectionEvent e) {
                    onNavigationChange();
                }
            });
        }

        protected void onNavigationChange() {
            // update the current config
            int index = mNavigation.getSelectionIndex();

            if (index != -1) {
                mSelectedConfiguration.setNavigationMethodQualifier(new NavigationMethodQualifier(
                        Navigation.getByIndex(index)));
            } else {
                // empty selection, means no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setNavigationMethodQualifier(
                        new NavigationMethodQualifier());
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            NavigationMethodQualifier q = (NavigationMethodQualifier)qualifier;

            Navigation value = q.getValue();
            if (value == null) {
                mNavigation.clearSelection();
            } else {
                mNavigation.select(Navigation.getIndex(value));
            }
        }
    }

    /**
     * Edit widget for {@link ScreenDimensionQualifier}.
     */
    private class ScreenDimensionEdit extends QualifierEditBase {

        private Text mSize1;
        private Text mSize2;

        public ScreenDimensionEdit(Composite parent) {
            super(parent, ScreenDimensionQualifier.NAME);

            ModifyListener modifyListener = new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    onSizeChange();
                }
            };

            FocusAdapter focusListener = new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    onSizeChange();
                }
            };

            mSize1 = new Text(this, SWT.BORDER);
            mSize1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mSize1.addVerifyListener(new DimensionVerifier());
            mSize1.addModifyListener(modifyListener);
            mSize1.addFocusListener(focusListener);

            mSize2 = new Text(this, SWT.BORDER);
            mSize2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mSize2.addVerifyListener(new DimensionVerifier());
            mSize2.addModifyListener(modifyListener);
            mSize2.addFocusListener(focusListener);
        }

        private void onSizeChange() {
            // update the current config
            String size1 = mSize1.getText();
            String size2 = mSize2.getText();

            if (size1.length() == 0 || size2.length() == 0) {
                // if one of the strings is empty, reset to no qualifier.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setScreenDimensionQualifier(new ScreenDimensionQualifier());
            } else {
                ScreenDimensionQualifier qualifier = ScreenDimensionQualifier.getQualifier(size1,
                        size2);

                if (qualifier != null) {
                    mSelectedConfiguration.setScreenDimensionQualifier(qualifier);
                } else {
                    // Failure! Looks like the value is wrong, reset the qualifier
                    // Since the qualifier classes are immutable, and we don't want to
                    // remove the qualifier from the configuration, we create a new default one.
                    mSelectedConfiguration.setScreenDimensionQualifier(
                            new ScreenDimensionQualifier());
                }
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            ScreenDimensionQualifier q = (ScreenDimensionQualifier)qualifier;

            mSize1.setText(Integer.toString(q.getValue1()));
            mSize2.setText(Integer.toString(q.getValue2()));
        }
    }

    /**
     * Edit widget for {@link VersionQualifier}.
     */
    private class VersionEdit extends QualifierEditBase {
        private Text mText;

        public VersionEdit(Composite parent) {
            super(parent, VersionQualifier.NAME);

            mText = new Text(this, SWT.BORDER);
            mText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mText.addVerifyListener(new MobileCodeVerifier());
            mText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    onVersionChange();
                }
            });
            mText.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    onVersionChange();
                }
            });

            new Label(this, SWT.NONE).setText("(Platform API level)");
        }

        private void onVersionChange() {
            String value = mText.getText();

            if (value.length() == 0) {
                // empty string, means a qualifier with no value.
                // Since the qualifier classes are immutable, and we don't want to
                // remove the qualifier from the configuration, we create a new default one.
                mSelectedConfiguration.setVersionQualifier(new VersionQualifier());
            } else {
                try {
                    VersionQualifier qualifier = VersionQualifier.getQualifier(
                            VersionQualifier.getFolderSegment(Integer.parseInt(value)));
                    if (qualifier != null) {
                        mSelectedConfiguration.setVersionQualifier(qualifier);
                    } else {
                        // Failure! Looks like the value is wrong
                        mSelectedConfiguration.setVersionQualifier(new VersionQualifier());
                    }
                } catch (NumberFormatException nfe) {
                    // Looks like the code is not a number. This should not happen since the text
                    // field has a VerifyListener that prevents it.
                    mSelectedConfiguration.setVersionQualifier(new VersionQualifier());
                }
            }

            // notify of change
            onChange(true /* keepSelection */);
        }

        @Override
        public void setQualifier(ResourceQualifier qualifier) {
            VersionQualifier q = (VersionQualifier)qualifier;

            mText.setText(Integer.toString(q.getVersion()));
        }
    }

}
