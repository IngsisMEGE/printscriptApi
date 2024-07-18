package printscript.service.services.serviceImpl

import org.example.PrintScript
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import printscript.service.dto.RulesDTO
import printscript.service.services.interfaces.RuleService
import printscript.service.utils.FileManagement

@Service
class RuleServiceImpl() : RuleService {
    private val logger: Logger = LoggerFactory.getLogger(RuleServiceImpl::class.java)

    override fun verifyLexerRules(lintingRule: List<RulesDTO>): Boolean {
        logger.debug("Entering verifyLexerRules")
        return try {
            val rulesPath = FileManagement.createLexerRuleFile(lintingRule)
            val printScript = PrintScript(::loadInput)
            printScript.updateRegexRules(rulesPath)
            FileManagement.deleteTempFile(rulesPath)
            FileManagement.deleteTempFile("lexerRules.json")
            logger.info("Successfully verified lexer rules")
            true
        } catch (e: Exception) {
            logger.error("Error verifying lexer rules", e)
            false
        } finally {
            logger.debug("Exiting verifyLexerRules")
        }
    }

    override fun verifyFormatterRules(formatterRule: List<RulesDTO>): Boolean {
        logger.debug("Entering verifyFormatterRules")
        return try {
            val rules = rulesToJSONString(formatterRule)
            val rulePath = FileManagement.createTempFileWithContent(rules)
            val printScript = PrintScript(::loadInput)
            printScript.changeFormatterConfig(rulePath)
            FileManagement.deleteTempFile(rulePath)
            FileManagement.deleteTempFile("formatterConfig.json")
            logger.info("Successfully verified formatter rules")
            true
        } catch (e: Exception) {
            logger.error("Error verifying formatter rules", e)
            false
        } finally {
            logger.debug("Exiting verifyFormatterRules")
        }
    }

    override fun verifySCARules(scaRule: List<RulesDTO>): Boolean {
        logger.debug("Entering verifySCARules")
        return try {
            val rulePath = FileManagement.creteSCARuleFile(scaRule)
            val printScript = PrintScript(::loadInput)
            printScript.changeSCAConfig(rulePath)
            FileManagement.deleteTempFile(rulePath)
            FileManagement.deleteTempFile("scaConfig.json")
            logger.info("Successfully verified SCA rules")
            true
        } catch (e: Exception) {
            logger.error("Error verifying SCA rules", e)
            false
        } finally {
            logger.debug("Exiting verifySCARules")
        }
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
