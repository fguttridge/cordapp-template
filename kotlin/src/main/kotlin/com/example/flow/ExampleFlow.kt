package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.Party
import net.corda.core.flows.FlowLogic

/**
 * Define your flow here.
 */
object ExampleFlow {
    /**
     * You can add a constructor to each FlowLogic subclass to pass objects into the flow.
     */
    class Initiator: FlowLogic<Unit>() {
        /**
         * Define the initiator's flow logic here.
         */
        @Suspendable
        override fun call() {}
    }

    class Acceptor(val otherParty: Party) : FlowLogic<Unit>() {
        /**
         * Define the acceptor's flow logic here.
         */
        @Suspendable
        override fun call() {}
    }
}