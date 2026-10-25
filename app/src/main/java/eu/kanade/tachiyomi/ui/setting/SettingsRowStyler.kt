package eu.kanade.tachiyomi.ui.setting

import androidx.preference.DialogPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.SeekBarPreference
import androidx.preference.TwoStatePreference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.widget.preference.MatPreference

/**
 * Assigns [R.layout.preference_navigation_yokai] to the settings rows that navigate somewhere,
 * so only those rows get the trailing iOS chevron.
 *
 * Rows with a control widget (switches, checkboxes, sliders, dropdowns) keep the plain
 * [R.layout.preference_yokai] layout, as do rows that only display information.
 */
internal object SettingsRowStyler {

    fun applyNavigationLayout(group: PreferenceGroup) {
        for (i in 0 until group.preferenceCount) {
            val preference = group.getPreference(i) ?: continue
            if (preference is PreferenceGroup) {
                applyNavigationLayout(preference)
                continue
            }
            if (preference.layoutResource != R.layout.preference_yokai) continue
            if (isNavigationRow(preference)) {
                preference.setLayoutResource(R.layout.preference_navigation_yokai)
            }
        }
    }

    private fun isNavigationRow(preference: Preference): Boolean {
        if (preference is PreferenceCategory) return false
        if (preference is TwoStatePreference || preference is SeekBarPreference) return false
        if (!preference.isSelectable || preference.widgetLayoutResource != 0) return false
        // Dialog rows (list, edit text, custom pickers) open something too. MatPreference and its
        // subclasses show their dialog from onClick() without registering a click listener.
        return preference is DialogPreference ||
            preference is MatPreference ||
            preference.onPreferenceClickListener != null
    }
}
