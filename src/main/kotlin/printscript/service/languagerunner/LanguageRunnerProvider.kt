package printscript.service.languagerunner

import printscript.service.dto.*

class LanguageRunnerProvider {
    companion object {
        fun getLanguageRunner(
            language: Language,
            loadInputs: (String) -> String,
        ): LanguageRunner {
            return when (language) {
                Language.Printscript -> Printscript(loadInputs)
                else -> throw Exception("Language not Implemented")
            }
        }
    }
}
