#!/bin/bash
echo 'deleting original config.yml...'
rm config.yml
echo 'deleted!'

METHOD='unknown'

echo 'selecting file...'
case $1 in
  http)
    METHOD=PostHTTP
    ;;
  tcp)
    METHOD=PutTCP
    ;;
  rpg)
    METHOD=RemoteProcessGroup
    ;;
  *)
    echo "unrecognized method $1"
    exit 1
    ;;
esac

case $2 in
  pv)
    # nothing to do
    ;;
  np)
    METHOD="${METHOD}_noprovenance"
    ;;
  *)
    echo "unrecognized provenance $2"
    exit 1
    ;;
esac

case $3 in
  th)
    METHOD="${METHOD}_threaded"
    ;;
  nt)
    # nothing to do
    ;;
  *)
    echo "unrecognized threading $3"
    exit 1
    ;;
esac
echo "selected $METHOD!"

echo 'copying...'
cp tmpl_SendToServer$METHOD.yml config.yml
echo 'copied!'