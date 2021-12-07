package pro.glideim.ui.chat

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dengzii.adapter.SuperAdapter
import com.dengzii.adapter.addViewHolderForType
import pro.glideim.R
import pro.glideim.base.BaseFragment

class SessionsFragment : BaseFragment() {

    private val mRvSessions by lazy { findViewById<RecyclerView>(R.id.rv_sessions) }
    private val mSrfRefresh by lazy { findViewById<SwipeRefreshLayout>(R.id.srf_refresh) }

    private val mSessionLise = mutableListOf<Any>()
    private val mAdapter = SuperAdapter(mSessionLise)

    override val layoutRes = R.layout.fragment_session

    override fun initView() {

        mAdapter.addViewHolderForType<String>(R.layout.item_session) {
            onBindData { _, _ ->

            }
        }
        mAdapter.setEnableEmptyView(true, SuperAdapter.EMPTY)
        mAdapter.setEnableEmptyViewOnInit(true)
        mRvSessions.adapter = mAdapter
        mRvSessions.layoutManager = LinearLayoutManager(requireContext())

        mSrfRefresh.setOnRefreshListener {
            view?.postDelayed({

                mSessionLise.add("1")
                mSessionLise.add("1")
                mSessionLise.add("1")

                mAdapter.notifyDataSetChanged()
                mSrfRefresh.isRefreshing = false

            }, 1000)
        }
    }
}