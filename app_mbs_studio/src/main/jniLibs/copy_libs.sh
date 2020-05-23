rm -r -f arm64-v8a/
mkdir -p arm64-v8a/

echo "  copy libep3_android.so from nas to local"
cp /mnt/builds/develop/multimedia/avalon/202005151707-dcfb0b4f/lib/jniLibs/arm64-v8a/libep3_android.so arm64-v8a/

echo "  copy libext_control.so from nas to local"
cp /mnt/builds/develop/multimedia/avalon/202005151707-dcfb0b4f/lib/jniLibs/arm64-v8a/libext_control.so arm64-v8a/
