import sys

path = r'J:\Android\TSDXXIVEK\tsdxxivek\src\main\java\com\xxivek\tsdxxivek\LogoFragment.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Вставляем import Log
old_import = 'import com.xxivek.tsdxxivek.databinding.FragmentLogoBinding\n'
new_import = old_import + 'import android.util.Log\n'
content = content.replace(old_import, new_import)

# 2. Вставляем import coroutineScope
old_coro = 'import androidx.navigation.Navigation\n'
new_coro = old_coro + 'import androidx.lifecycle.viewmodel.compose.viewModelScope\n'
content = content.replace(old_coro, new_coro)

# 3. Вставляем вызов performPrimaryAnalysis() после prefs
old_prefs = 'prefs = binding.root.context.getSharedPreferences("settings", Context.MODE_PRIVATE)\n'
new_prefs = old_prefs + '\n        // Первичный анализ состояния ТСД (до входа в MenuFragment)\n        performPrimaryAnalysis()\n'
content = content.replace(old_prefs, new_prefs)

# 4. Вставляем метод performPrimaryAnalysis перед последней закрывающей скобкой класса
method = '''
        private fun performPrimaryAnalysis() {
            if (appLic.appConnect1C > 0) {
                try {
                    val app = TSDXXIVekApplication.instance
                    if (app != null) {
                        val dao = app.database?.itemDao()
                        var total = 0
                        var notEmpty = 0
                        androidx.lifecycle.viewModelScope.launch {
                            total = dao?.getCount() ?: 0
                            notEmpty = dao?.getCountNotEmpty() ?: 0
                            if (notEmpty > 0) { appLic.appInfoBD.postValue(2) }
                            else if (total > 0) { appLic.appInfoBD.postValue(3) }
                            else { appLic.appInfoBD.postValue(0) }
                            Log.d("LogoFragment", "Первичный анализ bd: total=$total, notEmpty=$notEmpty -> bd=${appLic.appInfoBD.value}")
                        }
                    }
                    val inputDir = java.io.File(AppConstants.FILE_EXCHANGE_DIR)
                    val inputFile = java.io.File(inputDir, AppConstants.FILE_INPUT_JSON)
                    val outputFile = java.io.File(inputDir, AppConstants.FILE_OUTPUT_JSON)
                    if (inputFile.exists()) { appLic.appInfoINPUT.postValue(3); Log.d("LogoFragment", "tsd_Input.json найден -> input=3") }
                    else { appLic.appInfoINPUT.postValue(0); Log.d("LogoFragment", "tsd_Input.json не найден -> input=0") }
                    if (outputFile.exists()) { appLic.appInfoOUT.postValue(2); Log.d("LogoFragment", "tsd_Output.json найден -> output=2") }
                    else { appLic.appInfoOUT.postValue(0); Log.d("LogoFragment", "tsd_Output.json не найден -> output=0") }
                } catch (e: Exception) {
                    Log.e("LogoFragment", "Ошибка первичного анализа", e)
                    appLic.appInfoBD.postValue(1)
                    appLic.appInfoINPUT.postValue(1)
                    appLic.appInfoOUT.postValue(1)
                }
            } else {
                appLic.appInfoBD.postValue(0)
                appLic.appInfoINPUT.postValue(0)
                appLic.appInfoOUT.postValue(0)
                Log.d("LogoFragment", "Первичный анализ: сопряжение отсутствует")
            }
        }

'''

last_brace = content.rfind('}')
content = content[:last_brace] + method + content[last_brace:]

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print('Done! LogoFragment.kt updated.')