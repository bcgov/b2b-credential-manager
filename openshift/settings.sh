#!/bin/bash

export PROJECT_NAMESPACE="f6b17d"
export GIT_URI="https://github.com/hyperledger-labs/business-partner-agent"
export GIT_REF="master"

# The templates that should not have their GIT references(uri and ref) over-ridden
# Templates NOT in this list will have they GIT references over-ridden
# with the values of GIT_URI and GIT_REF

export APPLICATION_DOMAIN_POSTFIX=.apps.silver.devops.gov.bc.ca
