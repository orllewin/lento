package orllewin.tirwedd

import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import java.lang.NumberFormatException

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.vector_close)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val scaleAmountPreference = preferenceManager.findPreference<EditTextPreference>("horizontal_scale_factor")
            scaleAmountPreference?.setOnBindEditTextListener { editText ->
                //NOOP
            }

            scaleAmountPreference?.setOnPreferenceChangeListener { preference, newValue ->
                var isValid = true

                try {
                    val scaleFactor: Float = "$newValue".toFloat()
                } catch (e: NumberFormatException) {
                    isValid = false
                    Toast.makeText(this@SettingsFragment.requireContext(), "$newValue is not a valid float value", Toast.LENGTH_SHORT).show()
                }

                isValid
            }
        }
    }

}