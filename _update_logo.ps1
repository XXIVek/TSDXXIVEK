$content = [System.IO.File]::ReadAllText('J:\Android\TSDXXIVEK\tsdxxivek\src\main\java\com\xxivek\tsdxxivek\LogoFragment.kt', [System.Text.Encoding]::UTF8)
$oldImport = 'import com.xxivek.tsdxxivek.databinding.FragmentLogoBinding' + [char]13 + [char]10
$newImport = $oldImport + 'import android.util.Log' + [char]13 + [char]10
$content = $content.Replace($oldImport, $newImport)
$oldCoro = 'import androidx.navigation.Navigation' + [char]13 + [char]10
$newCoro = $oldCoro + 'import kotlinx.coroutines.launch' + [char]13 + [char]10
$content = $content.Replace($oldCoro, $newCoro)
$oldPrefs = 'prefs = binding.root.context.getSharedPreferences("settings", Context.MODE_PRIVATE)' + [char]13 + [char]10
$newPrefs = $oldPrefs + [char]13 + [char]10 + '        // Первичный анализ состояния ТСД (до входа в MenuFragment)' + [char]13 + [char]10 + '        performPrimaryAnalysis()' + [char]13 + [char]10
$content = $content.Replace($oldPrefs, $newPrefs)
$nl = [char]13 + [char]10
$method = $nl
$method += '        // Первичный анализ состояния ТСД при входе в LogoFragment' + $nl
$method += '        private fun performPrimaryAnalysis() {' + $nl
$method += '            if (appLic.appConnect1C > 0) {' + $nl
$method += '                try {' + $nl
$method += '                    val app = TSDXXIVekApplication.instance' + $nl
$method += '                    if (app != null) {' + $nl
$method += '                        val dao = app.database?.itemDao()' + $nl
$method += '                        var total = 0' + $nl
$method += '                        var notEmpty = 0' + $nl
$method += '                        androidx.lifecycle.viewModelScope.launch {' + $nl
$method += '                            total = dao?.getCount() ?: 0' + $nl
$method += '                            notEmpty = dao?.getCountNotEmpty() ?: 0' + $nl
$method += '                            if (notEmpty > 0) { appLic.appInfoBD.postValue(2) }' + $nl
$method += '                            else if (total > 0) { appLic.appInfoBD.postValue(3) }' + $nl
$method += '                            else { appLic.appInfoBD.postValue(0) }' + $nl
$method += '                            Log.d("LogoFragment", "Первичный анализ bd: total=$total, notEmpty=$notEmpty -> bd=${appLic.appInfoBD.value}")' + $nl
$method += '                        }' + $nl
$method += '                    }' + $nl
$method += '                    val inputDir = java.io.File(AppConstants.FILE_EXCHANGE_DIR)' + $nl
$method += '                    val inputFile = java.io.File(inputDir, AppConstants.FILE_INPUT_JSON)' + $nl
$method += '                    val outputFile = java.io.File(inputDir, AppConstants.FILE_OUTPUT_JSON)' + $nl
$method += '                    if (inputFile.exists()) { appLic.appInfoINPUT.postValue(3); Log.d("LogoFragment", "tsd_Input.json найден -> input=3") }' + $nl
$method += '                    else { appLic.appInfoINPUT.postValue(0); Log.d("LogoFragment", "tsd_Input.json не найден -> input=0") }' + $nl
$method += '                    if (outputFile.exists()) { appLic.appInfoOUT.postValue(2); Log.d("LogoFragment", "tsd_Output.json найден -> output=2") }' + $nl
$method += '                    else { appLic.appInfoOUT.postValue(0); Log.d("LogoFragment", "tsd_Output.json не найден -> output=0") }' + $nl
$method += '                } catch (e: Exception) {' + $nl
$method += '                    Log.e("LogoFragment", "Ошибка первичного анализа", e)' + $nl
$method += '                    appLic.appInfoBD.postValue(1)' + $nl
$method += '                    appLic.appInfoINPUT.postValue(1)' + $nl
$method += '                    appLic.appInfoOUT.postValue(1)' + $nl
$method += '                }' + $nl
$method += '            } else {' + $nl
$method += '                appLic.appInfoBD.postValue(0)' + $nl
$method += '                appLic.appInfoINPUT.postValue(0)' + $nl
$method += '                appLic.appInfoOUT.postValue(0)' + $nl
$method += '                Log.d("LogoFragment", "Первичный анализ: сопряжение отсутствует")' + $nl
$method += '            }' + $nl
$method += '        }' + $nl
$lastBrace = $content.LastIndexOf('}')
$result = $content.Substring(0, $lastBrace) + $method + $content.Substring($lastBrace)
[System.IO.File]::WriteAllText('J:\Android\TSDXXIVEK\tsdxxivek\src\main\java\com\xxivek\tsdxxivek\LogoFragment.kt', $result, [System.Text.Encoding]::UTF8)
Write-Output 'Done!'