#!/bin/bash

## This file is intended to be sourced by other scripts

function err() {
  echo >&2 "$@"
}

function curl_gh() {
  if [[ -n "$GITHUB_TOKEN" ]]; then
    curl \
      --silent \
      --header "Authorization: token $GITHUB_TOKEN" \
      "$@"
  else
    err "WARNING: No GITHUB_TOKEN found. Skipping API call"
  fi
}
