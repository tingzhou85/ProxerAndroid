package me.proxer.app.base

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.rubengees.rxbus.RxBus
import kotterknife.KotterKnife
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.openHttpPage
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import org.koin.android.ext.android.inject
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseFragment : Fragment(), CustomTabsAware {

    protected val bus by inject<RxBus>()
    protected val storageHelper by inject<StorageHelper>()
    protected val preferenceHelper by inject<PreferenceHelper>()

    protected open val hostingActivity: BaseActivity
        get() = requireActivity() as BaseActivity

    private var customTabsHelper by Delegates.notNull<CustomTabsHelperFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
    }

    override fun onDestroyView() {
        KotterKnife.reset(this)

        super.onDestroyView()
    }

    override fun setLikelyUrl(url: HttpUrl): Boolean {
        return customTabsHelper.mayLaunchUrl(url.androidUri(), bundleOf(), emptyList())
    }

    override fun showPage(url: HttpUrl, forceBrowser: Boolean) {
        customTabsHelper.openHttpPage(requireActivity(), url, forceBrowser)
    }
}
