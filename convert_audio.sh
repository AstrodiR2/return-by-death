#!/bin/bash
# Run this in Termux before pushing to GitHub
# Converts re-zero-return-by-death.mp3 -> return_by_death.ogg
# and copies it to the correct mod folder

INPUT="/storage/shared/Download/re-zero-return-by-death.mp3"
OUTPUT="src/main/resources/assets/returnbydeath/sounds/return_by_death.ogg"

echo "Converting audio..."
ffmpeg -i "$INPUT" -c:a libvorbis -q:a 4 "$OUTPUT"

if [ $? -eq 0 ]; then
  echo "✅ Done! Audio saved to $OUTPUT"
else
  echo "❌ Error! Make sure ffmpeg is installed: pkg install ffmpeg"
fi
