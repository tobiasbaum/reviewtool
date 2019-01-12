package de.setsoftware.reviewtool.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;

import de.setsoftware.reviewtool.ordering.HierarchyExplicitness;
import de.setsoftware.reviewtool.ordering.InSameFileRelation;
import de.setsoftware.reviewtool.ordering.InSameSourceFolderRelation;
import de.setsoftware.reviewtool.ordering.InSameSystemTestRelation;
import de.setsoftware.reviewtool.ordering.MethodCallRelation;
import de.setsoftware.reviewtool.ordering.MethodOverrideRelation;
import de.setsoftware.reviewtool.ordering.RelationMatcher;
import de.setsoftware.reviewtool.ordering.TokenSimilarityRelation;

/**
 * Handles the preferences for relation matchers (activity, explicitness, order).
 */
public class RelationMatcherPreferences {

    private static final String ACTIVE_TYPES_PREFERENCE_KEY = "activeRelationTypes";
    private static final String INACTIVE_TYPES_PREFERENCE_KEY = "inactiveRelationTypes";

    private static Map<HierarchyExplicitness, String> explicitnessTexts;

    static {
        explicitnessTexts = new EnumMap<>(HierarchyExplicitness.class);
        explicitnessTexts.put(HierarchyExplicitness.ALWAYS, " (always show in hierarchy)");
        explicitnessTexts.put(HierarchyExplicitness.ONLY_NONTRIVIAL, " (show in hierarchy if multiple children)");
        explicitnessTexts.put(HierarchyExplicitness.NONE, " (don't show in hierarchy)");
    }


    /**
     * Contains the known relation types and their properties.
     * Order in the enum determines the default priority order.
     */
    private enum Types {
        SAME_FILE("In same file", HierarchyExplicitness.ALWAYS, HierarchyExplicitness.ONLY_NONTRIVIAL, true) {
            @Override
            public RelationMatcher create(HierarchyExplicitness explicitness) {
                return new InSameFileRelation(explicitness);
            }
        },
        SOURCEFOLDER("Source folder (test vs src)", HierarchyExplicitness.ALWAYS, HierarchyExplicitness.ALWAYS, true) {
            @Override
            public RelationMatcher create(HierarchyExplicitness explicitness) {
                return new InSameSourceFolderRelation(explicitness);
            }
        },
        SYSTEMTEST("System test folder", HierarchyExplicitness.ALWAYS, HierarchyExplicitness.ALWAYS, true) {
            @Override
            public RelationMatcher create(HierarchyExplicitness explicitness) {
                return new InSameSystemTestRelation(explicitness);
            }
        },
        OVERRIDE("Method overriding", HierarchyExplicitness.ALWAYS, HierarchyExplicitness.ONLY_NONTRIVIAL, true) {
            @Override
            public RelationMatcher create(HierarchyExplicitness explicitness) {
                return new MethodOverrideRelation(explicitness);
            }
        },
        METHOD_CALL("Method calls", HierarchyExplicitness.ALWAYS, HierarchyExplicitness.NONE, true) {
            @Override
            public RelationMatcher create(HierarchyExplicitness explicitness) {
                return new MethodCallRelation(explicitness);
            }
        },
        SIMILARITY("Similar content", HierarchyExplicitness.NONE, HierarchyExplicitness.NONE, true) {
            @Override
            public RelationMatcher create(HierarchyExplicitness explicitness) {
                return new TokenSimilarityRelation();
            }
        };

        private final String description;
        private final HierarchyExplicitness maximumExplicitness;
        private final HierarchyExplicitness defaultExplicitness;
        private final boolean defaultActive;

        private Types(String description,
                HierarchyExplicitness maximumExplicitness,
                HierarchyExplicitness defaultExplicitness,
                boolean defaultActive) {
            this.description = description;
            this.maximumExplicitness = maximumExplicitness;
            this.defaultExplicitness = defaultExplicitness;
            this.defaultActive = defaultActive;
        }

        public abstract RelationMatcher create(HierarchyExplicitness explicitness);

        public boolean isActiveByDefault() {
            return this.defaultActive;
        }
    }

    /**
     * Helper class wrapping a type and its explicitness.
     */
    private static final class SettingForType {
        private final Types type;
        private HierarchyExplicitness explicitness;

        public SettingForType(Types t, HierarchyExplicitness e) {
            this.type = t;
            this.explicitness = e;
        }

        public void toggleExplicitness() {
            final int curIndex = this.explicitness.ordinal();
            final int nextIndex = (curIndex + 1) % (this.type.maximumExplicitness.ordinal() + 1);
            this.explicitness = HierarchyExplicitness.values()[nextIndex];
        }
    }

    private final List<SettingForType> activeTypes;
    private final List<Types> inactiveTypes;

    private RelationMatcherPreferences() {
        this.activeTypes = new ArrayList<>();
        this.inactiveTypes = new ArrayList<>();
    }

    /**
     * Loads the current settings from the given preference store.
     */
    public static RelationMatcherPreferences load(IPreferenceStore preferenceStore) {
        final RelationMatcherPreferences ret = new RelationMatcherPreferences();
        ret.activeTypes.addAll(splitSettings(preferenceStore.getString(ACTIVE_TYPES_PREFERENCE_KEY)));
        ret.inactiveTypes.addAll(splitTypes(preferenceStore.getString(INACTIVE_TYPES_PREFERENCE_KEY)));
        return ret;
    }

    public void save(IPreferenceStore preferenceStore) {
        preferenceStore.setValue(ACTIVE_TYPES_PREFERENCE_KEY, this.serializeActive());
        preferenceStore.setValue(INACTIVE_TYPES_PREFERENCE_KEY, this.serializeInactive());
    }

    private String serializeActive() {
        final StringBuilder b = new StringBuilder();
        for (final SettingForType t : this.activeTypes) {
            if (b.length() > 0) {
                b.append(';');
            }
            b.append(t.type.name()).append(',').append(t.explicitness.name());
        }
        return b.toString();
    }

    private String serializeInactive() {
        final StringBuilder b = new StringBuilder();
        for (final Types t : this.inactiveTypes) {
            if (b.length() > 0) {
                b.append(';');
            }
            b.append(t.name());
        }
        return b.toString();
    }

    /**
     * Creates and returns the corresponding {@link RelationMatcher}s. Takes care of using defaults when there are no
     * settings defined.
     */
    public List<? extends RelationMatcher> createMatchers() {
        final List<RelationMatcher> ret = new ArrayList<>();
        for (final SettingForType s : this.getActiveSettingsIncludingDefaults()) {
            ret.add(s.type.create(s.explicitness));
        }
        return ret;
    }

    private List<SettingForType> getActiveSettingsIncludingDefaults() {
        final List<SettingForType> ret = new ArrayList<>();
        final Set<Types> unusedTypes = EnumSet.allOf(Types.class);
        for (final SettingForType s : this.activeTypes) {
            ret.add(s);
            unusedTypes.remove(s.type);
        }
        unusedTypes.removeAll(this.inactiveTypes);

        //when the user had no explicit opinion on a type, use default settings
        for (final Types t : unusedTypes) {
            if (t.isActiveByDefault()) {
                ret.add(createDefault(t));
            }
        }
        return ret;
    }

    private List<Types> getInactiveSettingsIncludingDefaults() {
        final List<Types> ret = new ArrayList<>();
        final Set<Types> unusedTypes = EnumSet.allOf(Types.class);
        for (final SettingForType s : this.activeTypes) {
            unusedTypes.remove(s.type);
        }
        ret.addAll(this.inactiveTypes);
        unusedTypes.removeAll(this.inactiveTypes);

        //when the user had no explicit opinion on a type, use default settings
        for (final Types t : unusedTypes) {
            if (!t.isActiveByDefault()) {
                ret.add(t);
            }
        }
        return ret;
    }

    private static List<SettingForType> splitSettings(String string) {
        final List<SettingForType> ret = new ArrayList<>();
        for (final String part : string.split(";")) {
            final String[] nameAndExplicitness = part.split(",");
            if (nameAndExplicitness.length != 2) {
                continue;
            }
            try {
                ret.add(new SettingForType(
                        Types.valueOf(nameAndExplicitness[0]),
                        HierarchyExplicitness.valueOf(nameAndExplicitness[1])));
            } catch (final Exception e) {
                //ignore unknown type names
            }
        }
        return ret;
    }

    private static List<Types> splitTypes(String string) {
        final List<Types> ret = new ArrayList<>();
        for (final String part : string.split(";")) {
            try {
                ret.add(Types.valueOf(part));
            } catch (final Exception e) {
                //ignore unknown type names
            }
        }
        return ret;
    }

    private static SettingForType createDefault(Types relationType) {
        return new SettingForType(relationType, relationType.defaultExplicitness);
    }

    /**
     * Returns the list of inactive relation matchers (including defaults), formatted for the user UI.
     */
    public List<String> getInactiveForUser() {
        final List<String> ret = new ArrayList<>();
        for (final Types t : this.getInactiveSettingsIncludingDefaults()) {
            ret.add(t.description);
        }
        return ret;
    }

    /**
     * Returns the list of active relation matchers (including defaults), formatted for the user UI.
     */
    public List<String> getActiveForUser() {
        final List<String> ret = new ArrayList<>();
        for (final SettingForType t : this.getActiveSettingsIncludingDefaults()) {
            ret.add(t.type.description + explicitnessTexts.get(t.explicitness));
        }
        return ret;
    }

    /**
     * Adds the type with the given description to the end of the active list,
     * and removes it from the inactive list if needed.
     */
    public void activate(String userDescription) {
        final Types t = this.getTypeForDescription(userDescription);
        this.inactiveTypes.remove(t);
        this.activeTypes.add(createDefault(t));
    }

    /**
     * Adds the type with the given description (including explicitness) to the inactive list,
     * and removes it from the active list if needed.
     */
    public void deactivate(String userDescription) {
        final Types t = this.getTypeForDescription(this.stripExplicitness(userDescription));
        this.inactiveTypes.add(t);
        final Iterator<SettingForType> iter = this.activeTypes.iterator();
        while (iter.hasNext()) {
            final SettingForType cur = iter.next();
            if (cur.type == t) {
                iter.remove();
            }
        }
    }

    private String stripExplicitness(String userDescription) {
        for (final String e : explicitnessTexts.values()) {
            if (userDescription.endsWith(e)) {
                return userDescription.substring(0, userDescription.length() - e.length());
            }
        }
        return userDescription;
    }

    private Types getTypeForDescription(String userDescription) {
        for (final Types t : Types.values()) {
            if (userDescription.equals(t.description)) {
                return t;
            }
        }
        throw new AssertionError("invalid description " + userDescription);
    }

    /**
     * Moves the given active relation types up if possible.
     * Activates the types first if needed.
     */
    public void moveUp(List<String> userDescriptions) {
        final Set<Types> typesToMove = this.extractTypes(userDescriptions);
        this.activatePrefixWithTypes(typesToMove);
        boolean movePossible = false;
        for (int i = 0; i < this.activeTypes.size(); i++) {
            if (typesToMove.contains(this.activeTypes.get(i).type)) {
                if (movePossible) {
                    this.swapWithPrevious(this.activeTypes, i);
                }
            } else {
                movePossible = true;
            }
        }
    }

    /**
     * Moves the given active relation types down if possible.
     * Activates the types first if needed.
     */
    public void moveDown(List<String> userDescriptions) {
        final Set<Types> typesToMove = this.extractTypes(userDescriptions);
        this.activatePrefixWithTypes(typesToMove);
        boolean movePossible = false;
        for (int i = this.activeTypes.size() - 1; i >= 0; i--) {
            if (typesToMove.contains(this.activeTypes.get(i).type)) {
                if (movePossible) {
                    this.swapWithPrevious(this.activeTypes, i + 1);
                }
            } else {
                movePossible = true;
            }
        }
    }

    private void activatePrefixWithTypes(Set<Types> typesToMove) {
        final Set<Types> toActivate = EnumSet.copyOf(typesToMove);
        for (final SettingForType t : this.activeTypes) {
            toActivate.remove(t.type);
        }
        for (final Types t : Types.values()) {
            if (toActivate.contains(t)) {
                this.activeTypes.add(createDefault(t));
            }
        }
    }

    private void swapWithPrevious(List<SettingForType> list, int i) {
        final SettingForType temp = list.get(i);
        list.set(i, list.get(i - 1));
        list.set(i - 1, temp);
    }

    private Set<Types> extractTypes(List<String> userDescriptions) {
        final Set<Types> ret = EnumSet.noneOf(Types.class);
        for (final String s : userDescriptions) {
            ret.add(this.getTypeForDescription(this.stripExplicitness(s)));
        }
        return ret;
    }

    /**
     * Changes the "explicitness" value for the entry with the given text to the next possible value.
     */
    public void toggleExplicitness(String userDescription) {
        final Types t = this.getTypeForDescription(this.stripExplicitness(userDescription));
        this.activatePrefixWithTypes(Collections.singleton(t));
        final int index = this.determineIndex(t);
        this.activeTypes.get(index).toggleExplicitness();
    }

    private int determineIndex(Types t) {
        for (int i = 0; i < this.activeTypes.size(); i++) {
            if (this.activeTypes.get(i).type == t) {
                return i;
            }
        }
        return 0;
    }

    public void resetToDefaults() {
        this.activeTypes.clear();
        this.inactiveTypes.clear();
    }

}
