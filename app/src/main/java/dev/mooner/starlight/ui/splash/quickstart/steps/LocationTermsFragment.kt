/*
 * LocationTermsFragment.kt created by Minki Moon(mooner1022) on 22. 2. 6. 오후 5:23
 * Copyright (c) mooner1022. all rights reserved.
 * This code is licensed under the GNU General Public License v3.0.
 */

package dev.mooner.starlight.ui.splash.quickstart.steps

import android.Manifest.permission.*
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import dev.mooner.configdsl.ConfigStructure
import dev.mooner.configdsl.Icon
import dev.mooner.configdsl.adapters.ConfigAdapter
import dev.mooner.configdsl.config
import dev.mooner.configdsl.options.button
import dev.mooner.configdsl.options.toggle
import dev.mooner.starlight.databinding.FragmentLocationTermsBinding
import dev.mooner.starlight.plugincore.config.GlobalConfig
import dev.mooner.starlight.ui.splash.quickstart.QuickStartActivity

class LocationTermsFragment : Fragment() {

    private var _binding: FragmentLocationTermsBinding? = null
    private val binding get() = _binding!!
    private var adapter: ConfigAdapter? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            adapter?.redraw()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLocationTermsBinding.inflate(inflater, container, false)
        val activity = activity as QuickStartActivity

        activity.setPrevButtonEnabled(true)
        activity.setNextButtonEnabled(true)

        adapter = ConfigAdapter.Builder(activity) {
            bind(binding.recyclerView)
            structure(::getStruct)
            lifecycleOwner(this@LocationTermsFragment)
            onValueUpdated { _, id, value, _ ->
                val (pk, key) = when(id) {
                    "noob" ->
                        "general" to "newbie_mode"
                    "use_ext_module" ->
                        "project" to "load_global_libraries"
                    "use_plugin" ->
                        "plugin"  to "safe_mode"
                    else -> return@onValueUpdated
                }
                GlobalConfig.edit {
                    category(pk).setBoolean(key, (id == "use_plugin") xor (value as Boolean))
                }
            }
        }.build()

        return binding.root
    }

    private fun getStruct(): ConfigStructure =
        config {
            category {
                id = "additional"
                title = "부가 설정"
                items {
                    toggle {
                        id = "noob"
                        title = "쉬운 사용 모드"
                        description = "초심자를 위한 쉬운 사용 모드를 사용합니다."
                        icon = Icon.ECO
                        defaultValue = false
                    }
                    toggle {
                        id = "use_ext_module"
                        title = "외부 모듈 사용"
                        description = "'modules' 폴더 내의 모듈 코드를 실행합니다."
                        icon = Icon.DEVELOPER_MODE
                        defaultValue = true
                        dependency = "!noob"
                    }
                    toggle {
                        id = "use_plugin"
                        title = "플러그인 사용"
                        description = "플러그인 기능을 활성화 합니다."
                        icon = Icon.DEVELOPER_BOARD
                        defaultValue = true
                        dependency = "!noob"
                    }
                }
            }
            category {
                id = "location_terms"
                items {
                    button {
                        id = "allow_location_permission"
                        title = "위치 권한 사용"
                        description = "앱에서 기기의 위치 정보를 확인할 수 있어요. (선택)"
                        icon = Icon.LOCATION_ON
                        iconTintColor = color("#F4D19B")
                        setOnClickListener(::requestLocationPermission)
                    }
                    button {
                        id = "location_term"
                        title = "위치 정보 사용에 관하여"
                        description = "위 권한을 허용함으로서 접근할 수 있는 사용자의 위치 정보는 본 앱에서 직접적으로 수집하지 않으며, 오직 사용자의 스크립트 실행을 위해서만 사용됩니다."
                        icon = Icon.BOOKMARK
                        iconTintColor = color("#ffd866")
                    }
                }
            }
        }

    private fun requestLocationPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION)
        } else {
            arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION)
        }
        requestPermissionLauncher.launch(permissions)
    }
}
