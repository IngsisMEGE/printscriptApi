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
        val printScript = PrintScript()
        printScript.updateRegexRules(rulesPath)
        FileManagement.deleteTempFile(rulesPath)
        return true
    }

    override fun verifyFormatterRules(formatterRule: List<RulesDTO>): Boolean {
        val rules = rulesToJSONString(formatterRule)
        val rulePath = FileManagement.createTempFileWithContent(rules)
        val printScript = PrintScript()
        printScript.changeFormatterConfig(rulePath)
        FileManagement.deleteTempFile(rulePath)
        return true
    }

    override fun verifySCARules(scaRule: List<RulesDTO>): Boolean {
        val rulePath = FileManagement.creteSCARuleFile(scaRule)
        val printScript = PrintScript()
        printScript.changeSCAConfig(rulePath)
        FileManagement.deleteTempFile(rulePath)
        return true
    }

    private fun rulesToJSONString(rules: List<RulesDTO>): String {
        val rulesMap = mutableMapOf<String, String>()
        rules.forEach { rule ->
            rulesMap[rule.name] = rule.value
        }
        return rulesMap.toString()
    }
}
