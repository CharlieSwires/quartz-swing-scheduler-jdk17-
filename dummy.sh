#!/usr/bin/env bash

echo "Dummy process started."

for i in {10..1}
do
  echo "Exiting in ${i} second(s)..."
  sleep 1
done

echo "Dummy process finished."
exit 0