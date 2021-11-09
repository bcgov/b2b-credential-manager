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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.model.MyDocument;
import org.hyperledger.bpa.repository.MyDocumentRepository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Does some validation on incoming {@link MyDocumentAPI} objects. Like this we
 * make sure that the system is in a defined state, and we do not have to do
 * validation later.
 */
@Singleton
public class DocumentValidator {

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    @Setter
    SchemaService schemaService;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    public void validateNew(MyDocumentAPI document) {
        verifyOnlyOneOrgProfile(document);
        validateInternal(document);
    }

    public void validateExisting(Optional<MyDocument> existing, MyDocumentAPI newDocument) {
        if (existing.isEmpty()) {
            throw new WrongApiUsageException(ms.getMessage("api.document.validation.empty"));
        }

        if (!existing.get().getType().equals(newDocument.getType())) {
            throw new WrongApiUsageException(ms.getMessage("api.document.validation.type.changed"));
        }

        validateInternal(newDocument);
    }

    private void validateInternal(MyDocumentAPI document) {
        if (CredentialType.INDY.equals(document.getType())) {
            if (StringUtils.isEmpty(document.getSchemaId())) {
                throw new WrongApiUsageException(ms.getMessage("api.document.validation.schema.id.missing"));
            }
            // validate document data against schema
            Optional<BPASchema> schema = schemaService.getSchemaFor(document.getSchemaId());
            if (schema.isPresent()) {
                Set<String> attributeNames = schema.get().getSchemaAttributeNames();
                // assuming flat structure
                document.getDocumentData().fieldNames().forEachRemaining(fn -> {
                    if (!attributeNames.contains(fn)) {
                        throw new WrongApiUsageException(
                                ms.getMessage("api.document.validation.attribute.not.in.schema",
                                        Map.of("attr", fn)));
                    }
                });
            } else {
                throw new WrongApiUsageException(
                        ms.getMessage("api.schema.not.found", Map.of("id", document.getSchemaId())));
            }
        }
    }

    private void verifyOnlyOneOrgProfile(MyDocumentAPI doc) {
        if (doc.getType().equals(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL)) {
            docRepo.findAll().forEach(d -> {
                if (CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL.equals(d.getType())) {
                    throw new WrongApiUsageException(ms.getMessage("api.document.validation.profile.already.exists"));
                }
            });
        }
    }
}
