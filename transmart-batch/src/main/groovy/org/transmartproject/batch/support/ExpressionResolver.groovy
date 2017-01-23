package org.transmartproject.batch.support

import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.expression.BeanFactoryResolver
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext

/**
 * Combines Spel expression parsing with value fetching.
 */
class ExpressionResolver {

    @Autowired
    private BeanFactory beanFactory

    @Lazy
    SpelExpressionParser parser = new SpelExpressionParser()

    @Lazy
    StandardEvaluationContext context = { ->
        def ctx = new StandardEvaluationContext(beanFactory)
        ctx.beanResolver = new BeanFactoryResolver(beanFactory)
        ctx
    }()

    @SuppressWarnings('UnnecessaryPublicModifier') // codenarc bug; it's needed
    public <T> T resolve(String expression, Class<T> resultType) {
        parser.parseExpression(expression).getValue(context, resultType)
    }
}
