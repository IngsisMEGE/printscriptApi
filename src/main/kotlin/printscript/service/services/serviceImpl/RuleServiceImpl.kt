package printscript.service.services.serviceImpl

import org.example.PrintScript
import org.springframework.stereotype.Service
import printscript.service.dto.RulesDTO
import printscript.service.services.interfaces.RuleService
import printscript.service.utils.FileManagement

@Service
class RuleServiceImpl() : RuleService {
    override fun verifyLexerRules(lintingRule: List<RulesDTO>): Boolean {
        val rulesPath = FileManagement.createLexerRuleFile(lintingRule)
        val printScript = PrintScript(::loadInput)
        printScript.updateRegexRules(rulesPath)
        FileManagement.deleteTempFile(rulesPath)
        FileManagement.deleteTempFile("lexerRules.json")
        return true
    }

    override fun verifyFormatterRules(formatterRule: List<RulesDTO>): Boolean {
        val rules = rulesToJSONString(formatterRule)
        val rulePath = FileManagement.createTempFileWithContent(rules)
        val printScript = PrintScript(::loadInput)
        printScript.changeFormatterConfig(rulePath)
        FileManagement.deleteTempFile(rulePath)
        FileManagement.deleteTempFile("formatterConfig.json")
        return true
    }

    override fun verifySCARules(scaRule: List<RulesDTO>): Boolean {
        val rulePath = FileManagement.creteSCARuleFile(scaRule)
        val printScript = PrintScript(::loadInput)
        printScript.changeSCAConfig(rulePath)
        FileManagement.deleteTempFile(rulePath)
        FileManagement.deleteTempFile("scaConfig.json")
        return true
    }

    private fun rulesToJSONString(rules: List<RulesDTO>): String {
        val rulesMap = mutableMapOf<String, String>()
        rules.forEach { rule ->
            rulesMap[rule.name] = rule.value
        }
        return rulesMap.toString()
    }

    private fun loadInput(message: String): String {
        return "input"
    }
}
