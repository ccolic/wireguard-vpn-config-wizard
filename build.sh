#!/bin/bash
find -name "*.java" > sources.txt
javac -cp ".:lib/*" -d build @sources.txt
cp -r src/wgWizard/res/* build/
cp -r src/wgWizard/res build/wgWizard/
cp -r lib/ build/
cp src/META-INF/MANIFEST.MF build/
cd build
jar -cvfm wgWizard.jar MANIFEST.MF *

exit 0
