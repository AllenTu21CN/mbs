rm -r -f arm64-v8a/
mkdir -p arm64-v8a/

copyJniLib()
{
	echo "  cp $1 $2"
  echo $1 >> $2/copy_from
  cp $1 $2
}

copyJniLib /mnt/builds/develop/multimedia/avalon/ep-dev-r-agent/202102041105-820e8a25/lib/jniLibs/arm64-v8a/libep3_android.so arm64-v8a/
