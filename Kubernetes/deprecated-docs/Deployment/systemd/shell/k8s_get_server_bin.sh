#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

KUBE_ROOT=$(pwd)

KUBERNETES_RELEASE_URL="${KUBERNETES_RELEASE_URL:-https://storage.googleapis.com/kubernetes-release/release}"
KUBERNETES_RELEASE=v1.5.2
KUBERNETES_DOWNLOAD_URL_PREFIX="${KUBERNETES_RELEASE_URL}/${KUBERNETES_RELEASE}"

SERVER_PLATFORM="linux"
SERVER_ARCH="amd64"

ETCD_RELEASE=v3.0.15
ETCD_RELEASE_URL="${ETCD_URL_PREFIX:-https://github.com/coreos/etcd/releases/download}"
ETCD_DOWNLOAD_URL_PREFIX="${ETCD_RELEASE_URL}/${ETCD_RELEASE}"

function download_tarball() {
  local -r download_path="$1"
  local -r file="$2"
  local -r url_prefix="$3"
  url="${url_prefix}/${file}"
  mkdir -p "${download_path}"
  if [[ $(which curl) ]]; then
    curl -fL --retry 3 --keepalive-time 2 "${url}" -o "${download_path}/${file}"
  elif [[ $(which wget) ]]; then
    wget "${url}" -O "${download_path}/${file}"
  else
    echo "Couldn't find curl or wget.  Bailing out." >&2
    exit 4
  fi
  # TODO: add actual verification
}


SERVER_TAR="kubernetes-server-${SERVER_PLATFORM}-${SERVER_ARCH}.tar.gz"
ETCD_TAR="etcd-${ETCD_RELEASE}-${SERVER_PLATFORM}-${SERVER_ARCH}.tar.gz"

echo "Kubernetes release: ${KUBERNETES_RELEASE}"
echo "Etcd release: ${ETCD_RELEASE}"
echo

# download server/etcd tar
if [ ! -f ${KUBE_ROOT}/${SERVER_TAR}  ]; then 
    DOWNLOAD_SERVER_TAR=true
    echo "Will download ${SERVER_TAR} from ${KUBERNETES_DOWNLOAD_URL_PREFIX}"
fi

if [ ! -f ${KUBE_ROOT}/${ETCD_TAR} ]; then 
    DOWNLOAD_ETCD_TAR=true
    echo "Will download  ${ETCD_TAR} from ${ETCD_DOWNLOAD_URL_PREFIX}"
fi


if "${DOWNLOAD_SERVER_TAR}"; then
  download_tarball "${KUBE_ROOT}" "${SERVER_TAR}" "${KUBERNETES_DOWNLOAD_URL_PREFIX}"
fi

if "${DOWNLOAD_ETCD_TAR}"; then
  download_tarball "${KUBE_ROOT}" "${ETCD_TAR}" "${ETCD_DOWNLOAD_URL_PREFIX}"
fi


## extract binaries 

mkdir ${KUBE_ROOT}/server 
tar xvf ${KUBE_ROOT}/${SERVER_TAR}   --exclude="*.tar"   --exclude="*.docker_tag" --strip-components 3  -C ${KUBE_ROOT}/server
tar xvf ${KUBE_ROOT}/${ETCD_TAR}   --exclude="Documentation*" --exclude="README*"  --strip-components 1 -C ${KUBE_ROOT}/server
