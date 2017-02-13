package com.example.api

import com.example.contract.PurchaseOrderContract
import com.example.contract.PurchaseOrderState
import com.example.flow.ExampleFlow.Initiator
import com.example.flow.IssueDDRFlow.Issue
import com.example.flow.IssueDDRFlowResult
import com.example.flow.ExampleFlowResult
import com.example.model.PurchaseOrder
import com.example.model.Value
import net.corda.core.contracts.Amount
import net.corda.core.contracts.currency
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

val NOTARY_NAME = "Controller"

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("jasper")
class ExampleApi(val services: CordaRPCOps) {
    val myLegalName: String = services.nodeIdentity().legalIdentity.name

    @PUT
    @Path("{party}/issue-cash")
    fun issueCash(@PathParam("party") receivingParty: String, value: Value) : Response{

        val issueAmount = Amount(value.value, currency("CAD"))
        val otherParty = services.partyFromName(receivingParty)
        if (otherParty == null) {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
        val result: IssueDDRFlowResult = services
                .startFlow(::Issue, issueAmount, otherParty)
                .returnValue
                .toBlocking()
                .first()
        when (result) {
            is IssueDDRFlowResult.Success ->
                return Response
                        .status(Response.Status.CREATED)
                        .entity(result.message)
                        .build()
            is IssueDDRFlowResult.Failure ->
                return Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(result.message)
                        .build()
        }
    }

    /**
     * Returns the party name of the node providing this end-point.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService], the names can be used to look-up identities
     * by using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers() = mapOf("peers" to services.networkMapUpdates().first
            .map { it.legalIdentity.name })

    /**
     * Displays all purchase order states that exist in the vault.
     */
    @GET
    @Path("purchase-orders")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPurchaseOrders() = services.vaultAndUpdates().first

    @GET
    @Path("ViewTransactions")
    @Produces(MediaType.APPLICATION_JSON)
    fun viewTransactions(){}

    @PUT
    @Path("{party}/lookup")
    @Produces(MediaType.APPLICATION_JSON)
    fun getUserFromName(@PathParam("party") partyName: String) = services.partyFromName(partyName).toString()

    /**
     * This should only be called from the 'buyer' node. It initiates a flow to agree a purchase order with a
     * seller. Once the flow finishes it will have written the purchase order to ledger. Both the buyer and the
     * seller will be able to see it when calling /api/example/purchase-orders on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("{party}/create-purchase-order")
    fun createPurchaseOrder(purchaseOrder: PurchaseOrder, @PathParam("party") partyName: String): Response {
        val otherParty = services.partyFromName(partyName)
        if (otherParty == null) {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }

        val state = PurchaseOrderState(
                purchaseOrder,
                services.nodeIdentity().legalIdentity,
                otherParty,
                PurchaseOrderContract())

        // The line below blocks and waits for the future to resolve.
        val result: ExampleFlowResult = services
                .startFlow(::Initiator, state, otherParty)
                .returnValue
                .toBlocking()
                .first()

        when (result) {
            is ExampleFlowResult.Success ->
                return Response
                        .status(Response.Status.CREATED)
                        .entity(result.message)
                        .build()
            is ExampleFlowResult.Failure ->
                return Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(result.message)
                        .build()
        }
    }
}