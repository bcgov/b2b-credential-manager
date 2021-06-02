package org.hyperledger.bpa.impl.aries;

import lombok.NonNull;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationRequest;
import org.hyperledger.aries.api.present_proof.PresentationRequestCredentials;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PresentationRequestBuilder {

    /**
     * Auto accept all matching credentials
     * @param presentationExchange {@link PresentationExchangeRecord}
     * @param matchingCredentials {@link PresentationRequestCredentials}
     * @return {@link PresentationRequest}
     */
    public static Optional<PresentationRequest> acceptAll(
            @NonNull PresentationExchangeRecord presentationExchange,
            @NonNull List<PresentationRequestCredentials> matchingCredentials) {

        Optional<PresentationRequest> result = Optional.empty();
        Map<String, PresentationRequest.IndyRequestedCredsRequestedAttr> requestedAttributes =
                buildRequestedAttributes(presentationExchange, matchingCredentials);
        Map<String, PresentationRequest.IndyRequestedCredsRequestedPred> requestedPredicates =
                buildRequestedPredicates(presentationExchange, matchingCredentials);

        if (!requestedAttributes.isEmpty() || !requestedPredicates.isEmpty()) {
            result = Optional.of(PresentationRequest
                    .builder()
                    .requestedAttributes(requestedAttributes)
                    .requestedPredicates(requestedPredicates)
                    .build());
        }
        return result;
    }

    private static Map<String, PresentationRequest.IndyRequestedCredsRequestedAttr> buildRequestedAttributes(
            @NonNull PresentationExchangeRecord presentationExchange,
            @NonNull List<PresentationRequestCredentials> matchingCredentials) {

        Map<String, PresentationRequest.IndyRequestedCredsRequestedAttr> result = new LinkedHashMap<>();
        PresentProofRequest.ProofRequest presentationRequest = presentationExchange.getPresentationRequest();
        if (presentationRequest != null && presentationRequest.getRequestedAttributes() != null) {
            Set<String> requestedReferents = presentationRequest.getRequestedAttributes().keySet();
            requestedReferents.forEach(ref -> {
                // find requested referent in matching wallet credentials
                matchReferent(matchingCredentials, ref).ifPresent(
                        match -> result.put(ref, PresentationRequest.IndyRequestedCredsRequestedAttr
                                .builder()
                                .credId(match)
                                .revealed(Boolean.TRUE)
                                .build()));
            });
        }
        return result;
    }

    private static Map<String, PresentationRequest.IndyRequestedCredsRequestedPred> buildRequestedPredicates(
            @NonNull PresentationExchangeRecord presentationExchange,
            @NonNull List<PresentationRequestCredentials> matchingCredentials) {
        Map<String, PresentationRequest.IndyRequestedCredsRequestedPred> result = new LinkedHashMap<>();

        PresentProofRequest.ProofRequest presentationRequest = presentationExchange.getPresentationRequest();
        if (presentationRequest != null && presentationRequest.getRequestedPredicates() != null) {
            Set<String> requestedReferents = presentationRequest.getRequestedPredicates().keySet();
            requestedReferents.forEach(ref -> matchReferent(matchingCredentials, ref).ifPresent(
                    match -> result.put(ref, PresentationRequest.IndyRequestedCredsRequestedPred
                            .builder()
                            .credId(match)
                            // .timestamp let aca-py do this
                            .build())));
        }
        return result;
    }

    private static Optional<String> matchReferent(
            @NotNull List<PresentationRequestCredentials> matchingCredentials, String ref) {
        return matchingCredentials
                .stream()
                .filter(cred -> cred.getPresentationReferents().contains(ref))
                .map(PresentationRequestCredentials::getCredentialInfo)
                .map(PresentationRequestCredentials.CredentialInfo::getReferent)
                .findFirst();
    }
}