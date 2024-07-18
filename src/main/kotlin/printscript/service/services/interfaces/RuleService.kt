package printscript.service.services.interfaces

import printscript.service.dto.RulesDTO

interface RuleService {
    fun verifyLexerRules(lintingRule: List<RulesDTO>): Boolean

    fun verifyFormatterRules(formatterRule: List<RulesDTO>): Boolean

    fun verifySCARules(scaRule: List<RulesDTO>): Boolean
}
