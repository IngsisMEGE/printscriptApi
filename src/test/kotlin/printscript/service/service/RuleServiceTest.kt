package printscript.service.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import printscript.service.dto.*
import printscript.service.services.interfaces.RuleService
import printscript.service.services.serviceImpl.RuleServiceImpl

class RuleServiceTest {
    private val ruleService : RuleService = RuleServiceImpl()

    @Test
    fun test001VerifyLexerRulesSuccessFully() {
        val lintRules =
            listOf(
                RulesDTO("STRING_VALUE", "\"(?:\\\\.|[^\"])*\""),
                RulesDTO("STRING_VALUE", "'(?:\\\\.|[^'])*'"),
                RulesDTO("DECLARATION_VARIABLE", "\\blet\\b"),
                RulesDTO("DECLARATION_IMMUTABLE", "\\bconst\\b"),
                RulesDTO("IF_STATEMENT", "\\bif\\b"),
                RulesDTO("ELSE_STATEMENT", "\\}\\s*else"),
                RulesDTO("OPERATOR_PLUS", "\\+"),
            )

        val result = ruleService.verifyLexerRules(lintRules)

        assertEquals(true , result)
    }

//    @Test
//    fun test002VerifyLexerRulesWithTypeThatDosentExistShouldThrowError(){
//        val lintRules =
//            listOf(
//                RulesDTO("STRING_VALUE", "\"(?:\\\\.|[^\"])*\""),
//                RulesDTO("THIS_TYPE_DONT_EXIST", "IM_NOT_EVEN_REGEX"),
//                RulesDTO("DECLARATION_VARIABLE", "\\blet\\b"),
//                RulesDTO("DECLARATION_IMMUTABLE", "\\bconst\\b"),
//                RulesDTO("IF_STATEMENT", "\\bif\\b"),
//                RulesDTO("ELSE_STATEMENT", "\\}\\s*else"),
//                RulesDTO("OPERATOR_PLUS", "\\+"),
//            )
//
//        assertThrows<Exception> {
//            ruleService.verifyLexerRules(lintRules)
//        }
//    }

    @Test
    fun test003VerifyFormatterRulesSuccessfully() {
        val rules = listOf(
            RulesDTO("DotFront", "1"),
            RulesDTO("DotBack", "1"),
            RulesDTO("EqualFront", "1"),
            RulesDTO("EqualBack", "1"),
            RulesDTO("amountOfLines", "1"),
        )

        val result = ruleService.verifyFormatterRules(rules)

        assertTrue(result)
    }

    @Test
    fun test004VerifyFormatterRulesShouldThrowErrorIfRuleDosentExistInPrintScript() {
        val rules = listOf(
            RulesDTO("DotFront", "-1"),
            RulesDTO("IDONTEXIST", "-1"),
            RulesDTO("EqualFront", "1"),
            RulesDTO("EqualBack", "1"),
            RulesDTO("amountOfLines", "1"),
        )

        assertThrows<Exception> {
            ruleService.verifyFormatterRules(rules)
        }
    }

    @Test
    fun test005VerifySCARulesShouldSuccess(){
        val scaRules =
            listOf(
                RulesDTO("CamelCaseFormat", "true"),
                RulesDTO("SnakeCaseFormat", "false"),
                RulesDTO("MethodNoExpression", "false"),
                RulesDTO("InputNoExpression", "false"),
            )

        val result = ruleService.verifySCARules(scaRules)

        assertTrue(result)
    }

    @Test
    fun test006VerifySCARulesShouldThrowExceptionIfRuleDontExist(){
        val scaRules =
            listOf(
                RulesDTO("CamelCaseFormat", "true"),
                RulesDTO("SnakeCaseFormat", "true"),
                RulesDTO("MethodNoExpression", "ehmmm what the sigma"),
                RulesDTO("InputNoExpression", "skibidi"),
            )

        assertThrows<Exception> {
            ruleService.verifySCARules(scaRules)
        }
    }

}