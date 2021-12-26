#!/bin/bash
PATH=$PATH:"/c/Program Files/CodeAndWeb/TexturePacker/bin"
for tps in *.tps; do
  TexturePacker.exe "$tps"
done
