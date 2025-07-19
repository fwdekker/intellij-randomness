package com.fwdekker.randomness.testhelpers

import io.kotest.core.TestConfiguration
import io.kotest.core.spec.AfterAny
import io.kotest.core.spec.BeforeAny
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.core.test.TestScope
import io.kotest.core.test.TestType


/**
 * Runs [before] before every test, but, unlike, [TestConfiguration.beforeAny], does not run before other scopes.
 */
fun TestConfiguration.beforeNonContainer(before: BeforeAny) {
    this.beforeAny {
        if (it.type != TestType.Container)
            before(it)
    }
}

/**
 * Runs [before] before every test, but, unlike, [ContainerScope.beforeAny], does not run before other scopes.
 */
fun ContainerScope.beforeNonContainer(before: BeforeAny) {
    this.beforeAny {
        if (it.type != TestType.Container)
            before(it)
    }
}

/**
 * Runs [after] after every test, but, unlike, [TestConfiguration.afterAny], does not run after other scopes.
 */
fun TestConfiguration.afterNonContainer(after: AfterAny) {
    this.afterAny {
        if (it.a.type != TestType.Container)
            after(it)
    }
}

/**
 * Runs [after] after every test, but, unlike, [ContainerScope.afterAny], does not run after other scopes.
 */
fun ContainerScope.afterNonContainer(after: AfterAny) {
    this.afterAny {
        if (it.a.type != TestType.Container)
            after(it)
    }
}


/**
 * Like the regular `test` function, but ensures that the [block] runs in the event-dispatching thread (EDT).
 */
suspend fun FunSpecContainerScope.ideaEdtTest(name: String, block: suspend TestScope.() -> Unit) =
    test(name) {
        ideaRunEdt {
            block()
        }
    }
