/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.bpa.impl.activity;

import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.core.util.CollectionUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.resolver.DIDDocument;
import org.hyperledger.bpa.api.ApiConstants;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.client.DidDocClient;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.util.Converter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Singleton
public class PartnerLookup {

    @Inject
    Converter converter;

    @Inject
    DidDocClient ur;

    @Inject
    CryptoManager crypto;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    @Cacheable(cacheNames = { "partner-lookup-cache" })
    public PartnerAPI lookupPartner(@NonNull String did) {
        Optional<DIDDocument> didDocument = ur.getDidDocument(did);
        if (didDocument.isPresent()) {
            Optional<String> publicProfileUrl = didDocument.get().findPublicProfileUrl();
            if (publicProfileUrl.isPresent()) {
                PartnerAPI partner = lookupPartner(
                        publicProfileUrl.get(),
                        didDocument.get().getVerificationMethod());
                partner.setAriesSupport(didDocument.get().hasAriesEndpoint());
                partner.setDidDocAPI(didDocument.get());
                return partner;
            }
            log.warn("Did: {} has no profile endpoint on the ledger, probably not a BPA", did);
            return PartnerAPI
                    .builder()
                    .ariesSupport(didDocument.get().hasAriesEndpoint())
                    .didDocAPI(didDocument.get())
                    .did(didDocument.get().getId())
                    .build();
        }
        throw new PartnerException(ms.getMessage("api.partner.lookup.no.did.doc"));
    }

    PartnerAPI lookupPartner(@NonNull String endpoint, List<DIDDocument.VerificationMethod> verificationMethods) {
        Optional<VerifiablePresentation<VerifiableIndyCredential>> profile = ur.getPublicProfile(endpoint);
        if (profile.isPresent()) {

            final PartnerAPI partner = converter.toAPIObject(profile.get());

            String verificationMethod = profile.get().getProof() != null
                    ? profile.get().getProof().getVerificationMethod()
                    : "";
            Optional<String> pk = matchKey(verificationMethod, verificationMethods);
            if (pk.isPresent()) {
                final Boolean valid = crypto.verify(pk.get(), profile.get());
                partner.setValid(valid);
            }
            return partner;
        }
        throw new PartnerException(ms.getMessage("api.partner.lookup.no.endpoint", Map.of("endpoint", endpoint)));
    }

    /**
     * Tries to find the public key in the did document that matches the proof's
     * verification method
     *
     * @param verificationMethod  the proof verification method
     * @param verificationMethods list of {@link DIDDocument.VerificationMethod}
     *                            from the did document
     * @return matching public key in Base58
     */
    static Optional<String> matchKey(String verificationMethod,
            List<DIDDocument.VerificationMethod> verificationMethods) {
        Optional<DIDDocument.VerificationMethod> key = Optional.empty();
        String result = null;
        if (StringUtils.isNotEmpty(verificationMethod) && CollectionUtils.isNotEmpty(verificationMethods)) {
            key = verificationMethods.stream().filter(k -> verificationMethod.equals(k.getId())).findFirst();
        }
        if (key.isEmpty()) { // falling back to key list from did doc
            key = verificationMethods.stream()
                    .filter(k -> ApiConstants.DEFAULT_VERIFICATION_KEY_TYPE.equals(k.getType())).findFirst();
        }

        if (key.isPresent()) {
            result = key.get().getPublicKeyBase58();
        } else {
            log.warn("Expected at least one " + ApiConstants.DEFAULT_VERIFICATION_KEY_TYPE
                    + " in the did document, but found none");
        }
        return Optional.ofNullable(result);
    }

}
