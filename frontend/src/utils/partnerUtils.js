/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { CredentialTypes, PartnerStates } from "@/constants";

export const getPartnerProfile = (partner) => {
  if (partner && Object.prototype.hasOwnProperty.call(partner, "credential")) {
    let partnerProfile = partner.credential.find((cred) => {
      return cred.type === CredentialTypes.PROFILE.type;
    });
    if (partnerProfile) {
      if (
        Object.prototype.hasOwnProperty.call(partnerProfile, "credentialData")
      ) {
        return partnerProfile.credentialData;
      } else if (
        Object.prototype.hasOwnProperty.call(partnerProfile, "documentData")
      ) {
        return partnerProfile.documentData;
      }
    }
  }
};

export const getPartnerProfileRoute = (partner) => {
  if (partner && Object.prototype.hasOwnProperty.call(partner, "credential")) {
    let partnerProfile = partner.credential.find((cred) => {
      return cred.type === CredentialTypes.PROFILE.type;
    });
    if (partnerProfile) {
      if (
        Object.prototype.hasOwnProperty.call(partnerProfile, "credentialData")
      ) {
        return { name: "Credential", params: { id: partnerProfile.id } };
      } else if (
        Object.prototype.hasOwnProperty.call(partnerProfile, "documentData")
      ) {
        return { name: "Document", params: { id: partnerProfile.id } };
      }
    }
  }
};

export const getPartnerState = (partner) => {
  if (Object.prototype.hasOwnProperty.call(partner, "state")) {
    if (partner.state === PartnerStates.REQUEST.value) {
      return partner.incoming
        ? PartnerStates.CONNECTION_REQUEST_RECEIVED
        : PartnerStates.CONNECTION_REQUEST_SENT;
    } else if (
      partner.state === PartnerStates.ACTIVE.value ||
      partner.state === PartnerStates.RESPONSE.value ||
      partner.state === PartnerStates.COMPLETED.value ||
      partner.state === PartnerStates.PING_RESPONSE.value
    ) {
      return PartnerStates.ACTIVE_OR_RESPONSE;
    } else {
      return Object.values(PartnerStates).find((state) => {
        return partner.state === state.value;
      });
    }
  } else {
    return {
      value: "",
      label: "",
    };
  }
};

export const getPartnerStateColor = (state) => {
  switch (state) {
    case PartnerStates.REQUEST.value: {
      return "yellow";
    }
    case PartnerStates.ABANDONED.value:
    case PartnerStates.PING_NO_RESPONSE.value: {
      return "red";
    }
    case PartnerStates.ACTIVE_OR_RESPONSE:
    case PartnerStates.ACTIVE.value:
    case PartnerStates.RESPONSE.value:
    case PartnerStates.COMPLETED.value:
    case PartnerStates.PING_RESPONSE.value: {
      return "green";
    }
    default: {
      return "grey";
    }
  }
};
