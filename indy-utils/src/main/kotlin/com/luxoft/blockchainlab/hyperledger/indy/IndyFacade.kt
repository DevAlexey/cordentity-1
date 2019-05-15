package com.luxoft.blockchainlab.hyperledger.indy

import com.luxoft.blockchainlab.hyperledger.indy.ledger.LedgerService
import com.luxoft.blockchainlab.hyperledger.indy.models.*
import com.luxoft.blockchainlab.hyperledger.indy.wallet.IndyWalletFactory
import com.luxoft.blockchainlab.hyperledger.indy.wallet.IndyWalletUser

const val DEFAULT_MASTER_SECRET_ID = "main"

/**
 * This is the top-level interface that encapsulates work that should be done by [IndyWalletUser] and [LedgerService]
 *  cooperatively. Everything is abstracted as much as possible, so every valid service implementation should work.
 */
interface IndyFacade {
    val walletService: IndyWalletUser
    val ledgerService: LedgerService
    val did: String

    /**
     * Creates [Schema] using [IndyWalletUser] and stores it using [LedgerService]
     *
     * @param name [String] - schema name
     * @param version [String] - schema version in format "d.d.d"
     * @param attributes [List] of [String] - list of schema's attribute names
     *
     * @return [Schema]
     */
    fun createSchemaAndStoreOnLedger(name: String, version: String, attributes: List<String>): Schema

    /**
     * Creates [CredentialDefinition] using [IndyWalletUser] and stores it using [LedgerService]
     *
     * @param schemaId [SchemaId] - id of schema paired with this credential definition
     * @param enableRevocation [Boolean] - flag if you need revocation be enabled
     *
     * @return [CredentialDefinition]
     */
    fun createCredentialDefinitionAndStoreOnLedger(schemaId: SchemaId, enableRevocation: Boolean): CredentialDefinition

    /**
     * Creates [RevocationRegistryDefinition] and first [RevocationRegistryEntry] using [IndyWalletUser] and stores it
     * using [LedgerService]
     *
     * @param credentialDefinitionId [CredentialDefinitionId] - id of credential definition paired with this revocation
     *  registry
     * @param maxCredentialNumber [Int] - maximum credential count that this revocation registry can hold
     *
     * @return [RevocationRegistryInfo] - combination of [RevocationRegistryDefinition] and [RevocationRegistryEntry]
     */
    fun createRevocationRegistryAndStoreOnLedger(
        credentialDefinitionId: CredentialDefinitionId,
        maxCredentialNumber: Int
    ): RevocationRegistryInfo

    /**
     * Creates [CredentialOffer] using [IndyWalletUser]
     *
     * @param credentialDefinitionId [CredentialDefinitionId]
     *
     * @return [CredentialOffer]
     */
    fun createCredentialOffer(credentialDefinitionId: CredentialDefinitionId): CredentialOffer

    /**
     * Creates [CredentialRequest] using [IndyWalletUser]
     *
     * @param proverDid [String]
     * @param offer [CredentialOffer]
     * @param masterSecretId [String]
     *
     * @return [CredentialRequestInfo] - [CredentialRequest] and all related data
     */
    fun createCredentialRequest(
        proverDid: String,
        offer: CredentialOffer,
        masterSecretId: String = DEFAULT_MASTER_SECRET_ID
    ): CredentialRequestInfo

    /**
     * Issues [Credential] by [CredentialRequest] and [CredentialOffer] using [IndyWalletUser].
     * If revocation is enabled it will hold one of [maxCredentialNumber].
     *
     * @param credentialRequest [CredentialRequestInfo] - [CredentialRequest] and all reliable info
     * @param offer [CredentialOffer] - credential offer
     * @param revocationRegistryId [RevocationRegistryDefinitionId] or [null] - revocation registry definition id
     * @param proposalProvider lambda returning [Map] of [String] to [CredentialValue] - credential proposal
     *
     * @return [CredentialInfo] - credential and all reliable data
     */
    fun issueCredentialAndUpdateLedger(
        credentialRequest: CredentialRequestInfo,
        offer: CredentialOffer,
        revocationRegistryId: RevocationRegistryDefinitionId?,
        proposalProvider: () -> CredentialProposal
    ): CredentialInfo

    /**
     * Stores [Credential] in prover's wallet gathering data using [LedgerService]
     *
     * @param credentialInfo [CredentialInfo] - credential and all reliable data
     * @param credentialRequest [CredentialRequestInfo] - credential request and all reliable data
     * @param offer [CredentialOffer]
     */
    fun receiveCredential(
        credentialInfo: CredentialInfo,
        credentialRequest: CredentialRequestInfo,
        offer: CredentialOffer
    )

    /**
     * Revokes previously issued [Credential] using [IndyWalletUser] and [LedgerService]
     *
     * @param revocationRegistryId [RevocationRegistryDefinitionId] - revocation registry definition id
     * @param credentialRevocationId [String] - revocation registry credential index
     */
    fun revokeCredentialAndUpdateLedger(
        revocationRegistryId: RevocationRegistryDefinitionId,
        credentialRevocationId: String
    ): RevocationRegistryEntry

    /**
     * Creates [ProofRequest]. This function has nothing to do with Indy API, it is used just to produce well-shaped data.
     *
     * @param version [String] - ???
     * @param name [String] - name of this proof request
     * @param attributes [List] of [CredentialFieldReference] - attributes which prover needs to reveal
     * @param predicates [List] of [CredentialPredicate] - predicates which prover should answer
     * @param nonRevoked [Interval] or [null] - time interval of [attributes] and [predicates] non-revocation
     * @param nonce [String] - random value to distinct identical proofs
     *
     * @return [ProofRequest]
     */
    fun createProofRequest(
        version: String,
        name: String,
        attributes: List<CredentialFieldReference>,
        predicates: List<CredentialPredicate>,
        nonRevoked: Interval?,
        nonce: String = "123123"
    ): ProofRequest

    /**
     * Creates [ProofInfo] for provided [ProofRequest].
     *
     * @param proofRequest [ProofRequest] - proof request created by verifier
     * @param masterSecretId [String]
     *
     * @return [ProofInfo] - proof and all reliable data
     */
    fun createProofFromLedgerData(
        proofRequest: ProofRequest,
        masterSecretId: String = DEFAULT_MASTER_SECRET_ID
    ): ProofInfo

    /**
     * Verifies [ProofInfo] produced by prover
     *
     * @param proofReq [ProofRequest] - proof request used by prover to create proof
     * @param proof [ProofInfo] - proof created by prover
     *
     * @return [Boolean] - is proof valid?
     */
    fun verifyProofWithLedgerData(proofReq: ProofRequest, proof: ProofInfo): Boolean

    /**
     * Adds provided identity to whitelist and stores this info on ledger
     *
     * @param identityDetails [IdentityDetails]
     */
    fun addKnownIdentitiesAndStoreOnLedger(identityDetails: IdentityDetails)
}

/**
 * Builder for some [IndyFacade] implementation
 */
abstract class IndyFacadeBuilder {
    var builderWalletFactory: IndyWalletFactory? = null
    var builderLedgerService: LedgerService? = null
    val did: String? = null

    fun with(walletFactory: IndyWalletFactory): IndyFacadeBuilder {
        builderWalletFactory = walletFactory
        return this
    }

    fun with(ledgerService: LedgerService): IndyFacadeBuilder {
        builderLedgerService = ledgerService
        return this
    }

    /**
     * Implement this method, but be sure that you've checked presence of [IndyWalletUser] and [LedgerService]
     */
    abstract fun build(did: String): IndyFacade
}