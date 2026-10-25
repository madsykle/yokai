package yokai.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

const val SecondaryItemAlpha = .78f
const val HalfAlpha = .5f

object Size {
    val none = 0.dp
    val extraExtraTiny = 1.dp
    val extraTiny = 2.dp
    val tiny = 4.dp
    val small = 8.dp
    val smedium = 12.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val huge = 48.dp
    val extraHuge = 56.dp
    val navBarSize = 68.dp
}

/**
 * iOS palette for Compose surfaces (sheets, menus, chips, dialogs).
 *
 * Mirrors the `ios_*` colours in `res/values-night/colors.xml`; kept as Compose
 * constants because those resources are night-only and Compose cannot fall back to
 * them safely in day mode.
 */
object IoscontrolsPalette {
    val black = Color(0xFF000000)
    val grouped = Color(0xFF1C1C1E)
    val raised = Color(0xFF2C2C2E)
    val raised2 = Color(0xFF3A3A3C)
    val separator = Color(0xFF38383A)
    val label = Color(0xFFFFFFFF)
    val labelSecondary = Color(0x99EBEBF5)
    val labelTertiary = Color(0x4DEBEBF5)
    val blue = Color(0xFF0A84FF)
    val red = Color(0xFFFF453A)
    val hairline = Color(0x14FFFFFF)
}

/** Corner radii used by the iOS-style control surfaces. */
object IoscontrolsRadii {
    /** Popup menus, dialogs, cards. */
    val modal = 14.dp

    /** Modal bottom sheets draw only their top corners at this radius. */
    val sheet = 32.dp
}
